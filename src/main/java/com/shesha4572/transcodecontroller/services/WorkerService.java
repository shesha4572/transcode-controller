package com.shesha4572.transcodecontroller.services;

import com.shesha4572.transcodecontroller.entities.TranscodeJobTask;
import com.shesha4572.transcodecontroller.entities.WorkerPod;
import com.shesha4572.transcodecontroller.models.WorkerFinishTaskModel;
import com.shesha4572.transcodecontroller.models.WorkerPodHeartBeat;
import com.shesha4572.transcodecontroller.repositories.TranscodeJobTaskRepository;
import com.shesha4572.transcodecontroller.repositories.WorkerPodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Component
public class WorkerService {
    public final WorkerPodRepository workerPodRepository;
    public final TranscodeJobTaskRepository taskRepository;

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
            log.info("Worker {} discovered. ", podName);
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

    @Scheduled(fixedDelay = 35000)
    public void reconcileDeadWorkers(){
        log.info("Reconciling dead worker nodes..");
        workerPodRepository.findAll().forEach(workerPod -> {
            if(workerPod.getIsAssumedAlive() && workerPod.getLastPinged().plusSeconds(30).isBefore(LocalDateTime.now())){
                workerPod.setIsAssumedAlive(Boolean.FALSE);
                log.info("Worker {} has not pinged in {} seconds, Assuming worker is dead" , workerPod.getWorkerPodName() , Duration.between(workerPod.getLastPinged() , LocalDateTime.now()).toSeconds());
                if(workerPod.getIsAssignedTask()){
                    Optional<TranscodeJobTask> optionalTranscodeJobTask = taskRepository.findById(workerPod.getAssignedTaskId());
                    if(optionalTranscodeJobTask.isPresent()){
                        TranscodeJobTask transcodeJobTask = optionalTranscodeJobTask.get();
                        transcodeJobTask.setAssignedWorkerNodeId("");
                        transcodeJobTask.setIsAssignedToWorker(Boolean.FALSE);
                        log.info("Task #{} assigned to worker {} will also be reassigned" , transcodeJobTask.getTaskId() , workerPod.getWorkerPodName());
                    }
                }
            }
            workerPodRepository.save(workerPod);
        });
    }

    public void setWorkerAsAvailable(WorkerFinishTaskModel workerFinishTask){
        if(workerPodRepository.existsById(workerFinishTask.getPodName()) && taskRepository.existsById(workerFinishTask.getAssignedTaskId())){
            log.info("Worker {} has finished task #{}" , workerFinishTask.getPodName() , workerFinishTask.getAssignedTaskId());
            WorkerPod worker = workerPodRepository.findById(workerFinishTask.getPodName()).get();
            worker.setIsAssignedTask(Boolean.FALSE);
            worker.setAssignedTaskId("");
            worker.setLastPinged(LocalDateTime.now());
            worker.setIsAssumedAlive(Boolean.TRUE);
            workerPodRepository.save(worker);

            TranscodeJobTask transcodeJobTask = taskRepository.findById(workerFinishTask.getAssignedTaskId()).get();
            transcodeJobTask.setIsAssignedToWorker(Boolean.FALSE);
            transcodeJobTask.setAssignedWorkerNodeId("");
            transcodeJobTask.setTaskCompleted(Boolean.TRUE);
            transcodeJobTask.setTaskCompletionTime(LocalDateTime.now());
            transcodeJobTask.setMpdFileId(workerFinishTask.getMpdName());
            taskRepository.save(transcodeJobTask);
        }
    }

}
