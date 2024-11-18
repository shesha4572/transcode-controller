package com.shesha4572.transcodecontroller.entities;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.time.LocalDateTime;

@RedisHash("worker-pod")
@Builder
@Data
public class WorkerPod {
    @Id
    public String workerPodName;
    @Indexed
    public Boolean isAssignedTask;
    @Indexed
    public String assignedTaskId;
    public LocalDateTime lastPinged;
    @Indexed
    public Boolean isAssumedAlive;
}
