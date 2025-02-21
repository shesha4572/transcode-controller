package com.shesha4572.transcodecontroller.services;

import com.shesha4572.transcodecontroller.entities.TranscodeJobTask;
import com.shesha4572.transcodecontroller.repositories.TranscodeJobRepository;
import com.shesha4572.transcodecontroller.repositories.TranscodeJobTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;

@Service
@Component
@Slf4j
@RequiredArgsConstructor
public class ManifestMergeService {

    public final TranscodeJobTaskRepository taskRepository;
    public final TranscodeJobRepository jobRepository;
    public final FileService fileService;
    @Scheduled(fixedDelay = 120000 , initialDelay = 2000)
    public void checkAllTasksDone(){
        jobRepository.findAllByIsJobComplete(Boolean.FALSE).forEach(job -> {
            if(taskRepository.countByVideoInternalFileIdAndTaskCompleted(job.getVideoInternalFileId() , Boolean.FALSE) == 0){
                log.info("Video #{} transcode has finished. Will merge manifest files now " , job.getVideoInternalFileId());//TODO mergeMPD
            }
        });
    }

    public void mergeMPD(String videoInternalFileId){
        List<TranscodeJobTask> tasks = taskRepository.findAllByVideoInternalFileId(videoInternalFileId)
                .stream()
                .sorted(Comparator.comparing(TranscodeJobTask::getStartTime))
                .toList();
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
        }
        //TODO Change mediaPresentationDuration of MPD tag to video duration ex PT10M0.0S
        //TODO loop over rest of transcode tasks ordered by start time and add all their Period tags into the mpd and change the attribute start to corresponding start time based on the slice time chosen
        tasks.forEach(task -> {
            ;
        });
        //TODO save merged mpd in a local file
        //TODO upload local mpd to fs
    }

}
