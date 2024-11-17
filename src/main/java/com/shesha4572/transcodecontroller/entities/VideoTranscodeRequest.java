package com.shesha4572.transcodecontroller.entities;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Document(collection = "transcode-requests")
@Data
@Builder
public class VideoTranscodeRequest implements Serializable {
    @MongoId
    public String id;
    @Indexed
    public String videoInternalFileId;
    public int maxResolution;
    public LocalDateTime jobCreationTime;
    @Indexed
    public Boolean isJobComplete;
    public LocalDateTime jobCompletionTime;
    public LocalTime durationVideo;
}
