package com.waste.wastemanagement.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "worker_locations")
public class WorkerLocation implements Serializable{  // ✅ Add this
    private static final long serialVersionUID = 1L; // recommended

    @Id
    private String id;
    private String workerUsername;
    private double latitude;
    private double longitude;
    private LocalDateTime timestamp;
}
