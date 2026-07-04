package com.waste.wastemanagement.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class ComplaintDTO implements Serializable {
    private static final long serialVersionUID = 1L; // recommended

    private String id;
    private String userName;
    private String location;
    private String issue;
    private String status;
    private String assignedWorker;
}
