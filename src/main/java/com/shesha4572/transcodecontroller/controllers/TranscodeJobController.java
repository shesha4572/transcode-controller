package com.shesha4572.transcodecontroller.controllers;

import com.shesha4572.transcodecontroller.models.TranscodeJobRequestModel;
import com.shesha4572.transcodecontroller.services.TranscodeJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/transcode")
public class TranscodeJobController {

    private final TranscodeJobService transcodeJobService;

    @PostMapping("/newJob")
    public ResponseEntity<?> createNewTranscodeJob(@RequestBody TranscodeJobRequestModel jobRequest){
        if(transcodeJobService.registerNewTranscodeJob(jobRequest)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.internalServerError().build();
    }
}
