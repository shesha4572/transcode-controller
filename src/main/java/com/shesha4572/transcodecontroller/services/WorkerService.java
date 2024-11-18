package com.shesha4572.transcodecontroller.services;

import com.shesha4572.transcodecontroller.entities.WorkerPod;
import com.shesha4572.transcodecontroller.models.WorkerPodHeartBeat;
import com.shesha4572.transcodecontroller.repositories.WorkerPodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkerService {
    public final WorkerPodRepository workerPodRepository;

    @Value("${WORKER_SERVICE_NAME}")
    public String workerServiceName;

    public void heartBeatRegister(WorkerPodHeartBeat workerPodHeartBeat){
        WorkerPod workerPod;
        String podName = workerPodHeartBeat.getPodName();
        if(workerPodRepository.existsById(workerPodHeartBeat.getPodName())){
            log.info("Worker {} pinged. Task assigned : {}" , podName , workerPodHeartBeat.getIsAssignedTask() ? workerPodHeartBeat.getAssignedTaskId() : "False");
            workerPod = workerPodRepository.findById(podName).get();
            workerPod.setLastPinged(LocalDateTime.now());
            workerPod.setIsAssumedAlive(Boolean.TRUE);
            workerPod.setIsAssignedTask(workerPodHeartBeat.getIsAssignedTask());
            if(workerPod.getIsAssignedTask()){
                workerPod.setAssignedTaskId(workerPodHeartBeat.getAssignedTaskId());
            }
            else {
                workerPod.setAssignedTaskId("");
            }
        }
        else {
            log.info("Slave {} discovered. ", podName);
            workerPod = WorkerPod.builder()
                    .workerPodName(podName)
                    .isAssignedTask(Boolean.FALSE)
                    .lastPinged(LocalDateTime.now())
                    .assignedTaskId("")
                    .isAssumedAlive(Boolean.TRUE)
                    .build();
        }
        workerPodRepository.save(workerPod);
    }

}
