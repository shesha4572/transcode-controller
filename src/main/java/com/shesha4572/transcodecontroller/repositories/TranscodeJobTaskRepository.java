package com.shesha4572.transcodecontroller.repositories;

import com.shesha4572.transcodecontroller.entities.TranscodeJobTask;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TranscodeJobTaskRepository extends CrudRepository<TranscodeJobTask , String> {
}
