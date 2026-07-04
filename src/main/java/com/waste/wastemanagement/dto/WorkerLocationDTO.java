package com.waste.wastemanagement.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class WorkerLocationDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String workerUsername;
    private double latitude;
    private double longitude;
    private LocalDateTime timestamp;
}