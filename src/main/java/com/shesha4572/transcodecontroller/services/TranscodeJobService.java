package com.shesha4572.transcodecontroller.services;

import com.shesha4572.transcodecontroller.entities.TranscodeJobTask;
import com.shesha4572.transcodecontroller.entities.VideoTranscodeRequest;
import com.shesha4572.transcodecontroller.models.TranscodeJobRequestModel;
import com.shesha4572.transcodecontroller.repositories.TranscodeJobRepository;
import com.shesha4572.transcodecontroller.repositories.TranscodeJobTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranscodeJobService {

    public final TranscodeJobRepository transcodeJobRepository;
    public final TranscodeJobTaskRepository taskRepository;

    @Value("${YT_CDN_URL}")
    public String videoServerURL;

    public Boolean registerNewTranscodeJob(TranscodeJobRequestModel transcodeJobRequest){
        VideoTranscodeRequest videoTranscodeRequest = VideoTranscodeRequest.builder()
                .jobCreationTime(LocalDateTime.now())
                .videoInternalFileId(transcodeJobRequest.getVideoInternalFileId())
                .maxResolution(transcodeJobRequest.getMaxResolution())
                .isJobComplete(Boolean.FALSE)
                .build();
        String videoURL = videoServerURL + "/" + videoTranscodeRequest.getVideoInternalFileId();
        String videoDurationCommand = "ffmpeg -i %s 2>&1 | grep \"Duration\" | awk '{print $2}' | tr -d ','".formatted(videoURL);
        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/sh" , "-c" , videoDurationCommand);
            Process process = pb.start();
            process.waitFor();
            BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String duration = outputReader.readLine();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SS");
            LocalTime durationVideo = LocalTime.parse(duration, formatter);
            log.info("Video #{} Duration : {}", transcodeJobRequest.getVideoInternalFileId(), durationVideo);
            videoTranscodeRequest.setDurationVideo(durationVideo);
            log.info("Video #{} transcode job created" , videoTranscodeRequest.getVideoInternalFileId());
            transcodeJobRepository.save(videoTranscodeRequest);
            this.createTranscodeTasks(videoTranscodeRequest);
            return true;
        }
        catch (IOException | InterruptedException e){
            log.error(e.getMessage());
        }
        return false;
    }

    public void createTranscodeTasks(VideoTranscodeRequest videoTranscodeRequest){
        Duration optimalSlice = this.getOptimalSlice(videoTranscodeRequest.getDurationVideo());
        log.info("Optimal Slice Time for Video #{} taken as {}" , videoTranscodeRequest.getVideoInternalFileId() , optimalSlice);
        LocalTime videoLength = videoTranscodeRequest.getDurationVideo();
        LocalTime start = LocalTime.of(0 , 0 ,0);
        while (videoLength.isAfter(start)){
            TranscodeJobTask transcodeJobTask = TranscodeJobTask.builder()
                    .taskId(RandomStringUtils.randomAlphanumeric(10))
                    .taskCreationTime(LocalDateTime.now())
                    .videoInternalFileId(videoTranscodeRequest.getVideoInternalFileId())
                    .taskCompleted(Boolean.FALSE)
                    .startTime(start)
                    .endTime(start.plus(optimalSlice).isAfter(videoLength) ? videoLength : start.plus(optimalSlice))
                    .build();
            log.info("Task #{} of Video #{} created with Start Time : {} , End Time : {}" , transcodeJobTask.getTaskId() , videoTranscodeRequest.getVideoInternalFileId() , transcodeJobTask.getStartTime() , transcodeJobTask.getEndTime());
            start = start.plus(optimalSlice);
            taskRepository.save(transcodeJobTask);
        }
    }

    private Duration getOptimalSlice(LocalTime durationVideo) {
        return Duration.ofMinutes(2);
    }
}
