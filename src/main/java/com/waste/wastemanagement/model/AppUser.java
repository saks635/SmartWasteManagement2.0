package com.waste.wastemanagement.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.io.Serializable;  // << add this

@Data
@Document(collection = "users")
@AllArgsConstructor
@NoArgsConstructor
public class AppUser implements Serializable {  // << add Serializable
    private static final long serialVersionUID = 1L; // recommended

    @Id
    private String id;

    private String username;
    private String password;
    private String role; // USER, ADMIN, WORKER
}
