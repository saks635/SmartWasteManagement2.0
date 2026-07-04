package com.waste.wastemanagement.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class AppUserDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String username;
    private String role; // USER, ADMIN, WORKER
    // Exclude password for security
}
