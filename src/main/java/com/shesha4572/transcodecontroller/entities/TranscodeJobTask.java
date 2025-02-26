package com.shesha4572.transcodecontroller.entities;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.time.LocalDateTime;
import java.time.LocalTime;

@RedisHash("transcode-task")
@Builder
@Data
public class TranscodeJobTask {

    @Id
    public String taskId;
    @Indexed
    public String videoInternalFileId;
    public LocalDateTime taskCreationTime;
    @Indexed
    public Boolean taskCompleted;
    public LocalDateTime taskCompletionTime;
    public LocalTime startTime;
    public LocalTime endTime;
    @Indexed
    public Boolean isAssignedToWorker;
    public String assignedWorkerNodeId;
    public String mpdFileId;
    public Integer index;
}
