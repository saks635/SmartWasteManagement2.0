package com.waste.wastemanagement.repository;

import com.waste.wastemanagement.model.WorkerLocation;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface WorkerLocationRepository extends MongoRepository<WorkerLocation, String> {

    List<WorkerLocation> findTop10ByWorkerUsernameOrderByTimestampDesc(String workerUsername);
}
