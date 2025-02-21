package com.shesha4572.transcodecontroller.repositories;

import com.shesha4572.transcodecontroller.entities.TranscodeJobTask;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;
import java.util.List;


@Repository
public interface TranscodeJobTaskRepository extends ListCrudRepository<TranscodeJobTask , String> {
    List<TranscodeJobTask> findByIsAssignedToWorkerAndTaskCompletedOrderByTaskCreationTimeDesc(Boolean isAssignedToWorker, Boolean taskCompleted);

    int countByVideoInternalFileIdAndTaskCompleted(String videoId , Boolean flag);

    List<TranscodeJobTask> findAllByVideoInternalFileId(String videoInternalFileId);
}
