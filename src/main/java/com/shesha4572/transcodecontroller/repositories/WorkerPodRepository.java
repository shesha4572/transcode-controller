package com.shesha4572.transcodecontroller.repositories;

import com.shesha4572.transcodecontroller.entities.WorkerPod;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkerPodRepository extends ListCrudRepository<WorkerPod , String> {

    Optional<WorkerPod> findWorkerPodByIsAssignedTaskAndIsAssumedAliveOrderByLastPinged(Boolean isAssignedTask, Boolean isAssumedAlive);
}
