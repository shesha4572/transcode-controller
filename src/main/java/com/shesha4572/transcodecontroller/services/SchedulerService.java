package com.shesha4572.transcodecontroller.services;

import com.shesha4572.transcodecontroller.entities.WorkerPod;
import com.shesha4572.transcodecontroller.repositories.TranscodeJobTaskRepository;
import com.shesha4572.transcodecontroller.repositories.WorkerPodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Component
@RequiredArgsConstructor
@Slf4j
public class SchedulerService {
    public final TranscodeJobTaskRepository taskRepository;
    public final WorkerPodRepository workerPodRepository;
    @Value("${WORKER_SERVICE_NAME}")
    public String workerServiceName;


    @Scheduled(fixedDelay = 30000)
    public void scheduleTask(){
        log.info("Scheduling tasks..");
        taskRepository.findByIsAssignedToWorkerAndTaskCompletedOrderByTaskCreationTimeDesc(Boolean.FALSE , Boolean.FALSE).forEach(transcodeJobTask -> {
            log.info("Task #{} picked for assignment" , transcodeJobTask.getTaskId());
            Optional<WorkerPod> workerOptional = workerPodRepository.findWorkerPodByIsAssignedTaskAndIsAssumedAliveOrderByLastPinged(Boolean.FALSE , Boolean.TRUE);
            if(workerOptional.isPresent()){
                WorkerPod worker = workerOptional.get();
                log.info("Task #{} being assigned to worker {}" , transcodeJobTask.getTaskId() , worker.getWorkerPodName());
                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                Map<String, Object> map = new HashMap<>();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
                map.put("videoInternalFileId" , transcodeJobTask.getVideoInternalFileId());
                map.put("startTime" , formatter.format(transcodeJobTask.getStartTime()));
                map.put("endTime" , formatter.format(transcodeJobTask.getEndTime()));
                map.put("assignedTaskID" , transcodeJobTask.getTaskId());
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(map, headers);
                UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("http://%s.%s/job".formatted(worker.getWorkerPodName() , workerServiceName));
                try {
                    ResponseEntity<String> response = restTemplate.exchange(
                            builder.toUriString(),
                            HttpMethod.POST,
                            request,
                            String.class);
                    if (response.getStatusCode() == HttpStatusCode.valueOf(200)) {
                        log.info("Successfully assigned task #{} to worker {}", transcodeJobTask.getTaskId(), worker.getWorkerPodName());
                        transcodeJobTask.setAssignedWorkerNodeId(worker.getWorkerPodName());
                        worker.setAssignedTaskId(transcodeJobTask.getTaskId());
                        worker.setIsAssignedTask(Boolean.TRUE);
                        taskRepository.save(transcodeJobTask);
                        workerPodRepository.save(worker);
                    } else {
                        log.warn("Scheduling task #{} on worker {} failed. Will retry after a while ", transcodeJobTask.getTaskId(), worker.getWorkerPodName());
                    }
                }
                catch (Exception e){
                    log.error(e.getMessage());
                }
        }
        else {
            log.info("No workers are free or alive..");
        }
        });

    }
}
