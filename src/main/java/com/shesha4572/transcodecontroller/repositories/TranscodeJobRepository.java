package com.shesha4572.transcodecontroller.repositories;


import com.shesha4572.transcodecontroller.entities.VideoTranscodeRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TranscodeJobRepository extends MongoRepository<VideoTranscodeRequest, String> {
    Iterable<VideoTranscodeRequest> findAllByIsJobComplete(Boolean flag);
    Optional<VideoTranscodeRequest> findByVideoInternalFileId(String s);
}
