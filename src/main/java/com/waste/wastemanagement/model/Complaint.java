package com.waste.wastemanagement.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.io.Serializable;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "complaints")
public class Complaint implements Serializable{  // ✅ Add this
    private static final long serialVersionUID = 1L; // recommended

    @Id
    private String id;
    private String userName;
    private String location;
    private String issue;
    private String status;          // NEW, ASSIGNED, CLEANED
    private String assignedWorker;  // worker username
}

