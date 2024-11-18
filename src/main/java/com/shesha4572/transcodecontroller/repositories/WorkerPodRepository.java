package com.shesha4572.transcodecontroller.repositories;

import com.shesha4572.transcodecontroller.entities.WorkerPod;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkerPodRepository extends CrudRepository<WorkerPod , String> {
}
