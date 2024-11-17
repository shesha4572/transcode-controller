package com.shesha4572.transcodecontroller.services;

import com.shesha4572.transcodecontroller.entities.VideoTranscodeRequest;
import com.shesha4572.transcodecontroller.models.TranscodeJobRequestModel;
import com.shesha4572.transcodecontroller.repositories.TranscodeJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranscodeJobService {

    public final TranscodeJobRepository transcodeJobRepository;

    @Value("${YT_CDN_URL}")
    public String videoServerURL;

    public void registerNewTranscodeJob(TranscodeJobRequestModel transcodeJobRequest){
        VideoTranscodeRequest videoTranscodeRequest = VideoTranscodeRequest.builder()
                .jobCreationTime(LocalDateTime.now())
                .videoInternalFileId(transcodeJobRequest.getVideoInternalFileId())
                .maxResolution(transcodeJobRequest.getMaxResolution())
                .isJobComplete(Boolean.FALSE)
                .build();
        String videoURL = videoServerURL + "/" + videoTranscodeRequest.getVideoInternalFileId();
        String videoDurationCommand = "ffmpeg -i %s 2>&1 | grep \"Duration\" | awk '{print $2}' | tr -d ','".formatted(videoURL);
        log.info("Command : {}" , videoDurationCommand);
        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/sh" , "-c" , videoDurationCommand);
            Process process = pb.start();
            process.waitFor();
            BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String duration = outputReader.readLine();
            log.info("Video #{} Duration : {}", transcodeJobRequest.getVideoInternalFileId(), duration);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SS");
            LocalTime durationVideo = LocalTime.parse(duration, formatter);
            videoTranscodeRequest.setDurationVideo(durationVideo);
        }
        catch (IOException | InterruptedException e){
            log.error(e.getMessage());
        }
        transcodeJobRepository.save(videoTranscodeRequest);

    }
}
