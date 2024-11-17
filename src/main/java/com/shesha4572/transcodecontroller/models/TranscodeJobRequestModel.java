package com.shesha4572.transcodecontroller.models;

import lombok.Data;

@Data
public class TranscodeJobRequestModel {
    public String videoInternalFileId;
    public int maxResolution;  //0 -> 144p, 1 -> 240p, 2 -> 360p, 3 -> 480p, 4 -> 720p, 5 -> 1080p, 6 -> 1440p, 7 -> 2160p

}
