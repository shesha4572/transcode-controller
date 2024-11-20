package com.shesha4572.transcodecontroller.controllers;

import com.shesha4572.transcodecontroller.models.WorkerFinishTaskModel;
import com.shesha4572.transcodecontroller.models.WorkerPodHeartBeat;
import com.shesha4572.transcodecontroller.services.WorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/worker")
@RequiredArgsConstructor
public class WorkerController {

    public final WorkerService workerService;

    @PostMapping("/ping")
    public ResponseEntity<?> workerDiscover(@RequestBody WorkerPodHeartBeat workerPodHeartBeat){
        workerService.heartBeatRegister(workerPodHeartBeat);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/finish")
    public ResponseEntity<?> workerFinishedTask(@RequestBody WorkerFinishTaskModel workerFinishTask){
        workerService.setWorkerAsAvailable(workerFinishTask);
        return ResponseEntity.ok().build();
    }
}
