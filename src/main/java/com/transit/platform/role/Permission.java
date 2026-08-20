package com.transit.platform.role;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String module;

    private String description;

    protected Permission() {
        // requis par JPA/Hibernate
    }

    public Permission(String code, String module) {
        this.code = code;
        this.module = module;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getModule() { return module; }
    public String getDescription() { return description; }
}
