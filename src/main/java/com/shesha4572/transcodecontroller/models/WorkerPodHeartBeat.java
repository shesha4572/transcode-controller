package com.shesha4572.transcodecontroller.models;

import lombok.Data;

@Data
public class WorkerPodHeartBeat {
    public String podName;
    public Boolean isAssignedTask;
    public String assignedTaskId;
}
