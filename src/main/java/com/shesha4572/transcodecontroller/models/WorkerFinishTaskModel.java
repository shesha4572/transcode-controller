package com.shesha4572.transcodecontroller.models;

import lombok.Data;

@Data
public class WorkerFinishTaskModel {
    public String podName;
    public String mpdName;
    public String assignedTaskId;
}
