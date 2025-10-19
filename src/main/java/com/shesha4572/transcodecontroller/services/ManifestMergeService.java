package com.shesha4572.transcodecontroller.services;

import com.shesha4572.transcodecontroller.entities.TranscodeJobTask;
import com.shesha4572.transcodecontroller.entities.VideoTranscodeRequest;
import com.shesha4572.transcodecontroller.repositories.TranscodeJobRepository;
import com.shesha4572.transcodecontroller.repositories.TranscodeJobTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;

@Service
@Component
@Slf4j
@RequiredArgsConstructor
public class ManifestMergeService {

    public final TranscodeJobTaskRepository taskRepository;
    public final TranscodeJobRepository jobRepository;
    public final FileService fileService;
    public final TranscodeJobService transcodeJobService;
    @Value("${YT_CDN_URL}")
    public String videoServerURL;
    @Scheduled(fixedDelay = 12000 , initialDelay = 2000)
    public void checkAllTasksDone(){
        log.info("Checking for videos waiting on merging MPD");
        jobRepository.findAllByIsJobComplete(Boolean.FALSE).forEach(job -> {
            List<TranscodeJobTask> remainingTasks = taskRepository.findAllByVideoInternalFileIdAndTaskCompleted(job.getVideoInternalFileId(), Boolean.FALSE);
            if(remainingTasks.isEmpty()){
                log.info("Video #{} transcode has finished. Will merge manifest files now " , job.getVideoInternalFileId());
                try {
                    if(!this.mergeMPD(job.getVideoInternalFileId())){
                        throw new Exception();
                    }
                    log.info("Merge MPD of video {} completed." , job.getVideoInternalFileId());
                    job.setIsJobComplete(true);
                    jobRepository.save(job);
                } catch (Exception e) {
                    log.info("Merge MPD of video {} failed. Will retry later Exception : {}" , job.getVideoInternalFileId() , e.getMessage());
                }

            }
        });
    }

    private static String convertToDashDuration(LocalTime time) {
        int hours = time.getHour();
        int minutes = time.getMinute();
        double seconds = time.getSecond() + (time.getNano() / 1_000_000_000.0);

        StringBuilder duration = new StringBuilder("PT");
        if (hours > 0) duration.append(hours).append("H");
        if (minutes > 0) duration.append(minutes).append("M");
        duration.append(String.format("%.1f", seconds)).append("S");

        return duration.toString();
    }

    public boolean mergeMPD(String videoInternalFileId) throws Exception {
        List<TranscodeJobTask> tasks = taskRepository.findAllByVideoInternalFileId(videoInternalFileId)
                .stream()
                .sorted(Comparator.comparing(TranscodeJobTask::getIndex))
                .toList();
        Optional<VideoTranscodeRequest> transcodeRequestOptional = jobRepository.findByVideoInternalFileId(videoInternalFileId);
        if(transcodeRequestOptional.isEmpty()){
            return false;
        }
        VideoTranscodeRequest transcodeRequest = transcodeRequestOptional.get();
        TranscodeJobTask firstTask = tasks.get(0);
        String firstMPDFileID = firstTask.getMpdFileId();
        Document firstMPD;
        try{
            log.info("Reading first MPD file #{} of video #{}" , firstMPDFileID , videoInternalFileId);
            byte[] firstMPDBytes = fileService.downloadFile(firstMPDFileID);
            InputStream inputStream = new ByteArrayInputStream(firstMPDBytes);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            firstMPD = builder.parse(inputStream);

        } catch (Exception e) {
            log.info("Reading MPD file #{} failed. Merging of video #{} will be attempted later" , firstMPDFileID , videoInternalFileId);
            return false;
        }
        Element firstMPDElem = firstMPD.getDocumentElement();
        firstMPDElem.setAttribute("mediaPresentationDuration" , convertToDashDuration(transcodeRequest.getDurationVideo()));
        Duration sliceTime = transcodeJobService.getOptimalSlice(transcodeRequest.getDurationVideo());
        LocalTime startTime = LocalTime.of(0 , 0).plus(sliceTime);
        for(int i = 1; i < tasks.size(); i++) {
            TranscodeJobTask task = tasks.get(i);
            try {
                log.info("Reading MPD file #{} of video #{}", task.getMpdFileId(), videoInternalFileId);
                byte[] firstMPDBytes = fileService.downloadFile(task.getMpdFileId());
                if(firstMPDBytes == null){
                    throw new Exception();
                }
                InputStream inputStream = new ByteArrayInputStream(firstMPDBytes);
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document mpdFile = builder.parse(inputStream);
                Element periodElem = (Element) mpdFile.getElementsByTagName("Period").item(0);
                periodElem.setAttribute("id", String.valueOf(i));
                periodElem.setAttribute("start" , convertToDashDuration(startTime));
                Node importedNode = firstMPD.importNode(periodElem, true);
                firstMPDElem.appendChild(importedNode);
                log.info("Appended Period id #{} and startTime {} to merged MPD of video #{}", i, startTime , videoInternalFileId);
                startTime = startTime.plus(Duration.ofMinutes(2));

            } catch (Exception e) {
                log.info("Reading MPD file #{} failed. Merging of video #{} will be attempted later", task.getMpdFileId(), videoInternalFileId);
                return false;
            }
        }
        String fileName = "%s_%s_mpd".formatted(videoInternalFileId , RandomStringUtils.randomAlphanumeric(5));
        File finalMPD = File.createTempFile(fileName , ".mpd");
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new DOMSource(firstMPD), new StreamResult(finalMPD));
        if(fileService.uploadFile(finalMPD, fileName, fileName)){
            transcodeRequest.setMpdName(fileName);
            jobRepository.save(transcodeRequest);
            return true;
        }
        return false;
    }

    @Scheduled(fixedDelay = 12000 , initialDelay = 3000)
    public void makeVideoServerAware(){
        log.info("Checking if any videos are playable but server is not aware of");
        List<VideoTranscodeRequest> allCompletedJobs = jobRepository.findByIsJobCompleteTrue();
        List<VideoTranscodeRequest> filteredJobs = allCompletedJobs.stream()
                .filter(task -> Boolean.FALSE.equals(task.getIsServerAwareJobComplete()))
                .toList();
        filteredJobs.forEach(job -> {
            log.info("Making server aware of video #{} being playable" , job.getVideoInternalFileId());
            String uri = videoServerURL + "/api/v1/video/transcode/done";
            HashMap<String , String> body = new HashMap<>();
            body.put("internalFileId" , job.getVideoInternalFileId());
            body.put("mpdName" , job.getMpdName());
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(uri);
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.POST,
                        request,
                        String.class);
                if(response.getStatusCode() == HttpStatusCode.valueOf(200)){
                    job.setIsServerAwareJobComplete(true);
                    jobRepository.save(job);
                    log.info("Success making server aware of video #{} being playable" , job.getVideoInternalFileId());
                }
                else {
                    log.info("Failed to make server aware of video #{} being playable. Error : {}" , job.getVideoInternalFileId() , response.getStatusCode());
                }
            } catch (Exception e) {
                log.info(e.getMessage());
            }
        });

    }

}
