package com.shesha4572.transcodecontroller.repositories;


import com.shesha4572.transcodecontroller.entities.VideoTranscodeRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TranscodeJobRepository extends CrudRepository<VideoTranscodeRequest, String> {
}
