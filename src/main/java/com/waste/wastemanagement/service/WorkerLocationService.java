package com.waste.wastemanagement.service;

import com.waste.wastemanagement.dto.WorkerLocationDTO;
import com.waste.wastemanagement.model.WorkerLocation;
import com.waste.wastemanagement.repository.WorkerLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WorkerLocationService {

    private static final Logger logger = LoggerFactory.getLogger(WorkerLocationService.class);

    private final WorkerLocationRepository repo;

    // Optional — only available when Redis is configured.
    // When null (local dev without Redis), all Redis operations are safely skipped.
    @Nullable
    private final RedisTemplate<String, Object> redisTemplate;

    public WorkerLocationService(WorkerLocationRepository repo,
                                 @Autowired(required = false) RedisTemplate<String, Object> redisTemplate) {
        this.repo = repo;
        this.redisTemplate = redisTemplate;
    }

    public void saveLocation(String worker, double lat, double lng) {
        try {
            WorkerLocation loc = new WorkerLocation();
            loc.setWorkerUsername(worker);
            loc.setLatitude(lat);
            loc.setLongitude(lng);
            loc.setTimestamp(LocalDateTime.now());
            repo.save(loc);

            // Redis operations — only if Redis is available
            if (redisTemplate != null) {
                // Cache last location in Redis with TTL
                String lastLocationKey = "worker:last:" + worker;
                Map<String, Object> locationData = new HashMap<>();
                locationData.put("lat", lat);
                locationData.put("lng", lng);
                locationData.put("timestamp", LocalDateTime.now().toString());
                locationData.put("worker", worker);
                redisTemplate.opsForValue().set(lastLocationKey, locationData);

                // Publish location update for real-time tracking (if needed)
                String channel = "worker:location:" + worker;
                redisTemplate.convertAndSend(channel, locationData);

                // Clear cache for this worker's location history
                redisTemplate.delete("worker:locations:" + worker);
            }

            logger.debug("Saved location for worker {}: lat={}, lng={}", worker, lat, lng);
        } catch (Exception e) {
            logger.error("Failed to save location for worker {}: {}", worker, e.getMessage(), e);
            throw e;
        }
    }

    @Cacheable(value = "worker-locations", key = "#worker")
    public List<WorkerLocationDTO> getLastLocations(String worker) {
        return repo.findTop10ByWorkerUsernameOrderByTimestampDesc(worker).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Get last known location from Redis cache (faster than DB query)
    public Map<String, Object> getLastKnownLocation(String worker) {
        if (redisTemplate == null) return null; // Redis not available in dev
        String key = "worker:last:" + worker;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof Map) {
            return (Map<String, Object>) cached;
        }
        return null;
    }

    private WorkerLocationDTO convertToDTO(WorkerLocation location) {
        WorkerLocationDTO dto = new WorkerLocationDTO();
        dto.setId(location.getId());
        dto.setWorkerUsername(location.getWorkerUsername());
        dto.setLatitude(location.getLatitude());
        dto.setLongitude(location.getLongitude());
        dto.setTimestamp(location.getTimestamp());
        return dto;
    }
}

