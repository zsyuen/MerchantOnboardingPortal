package com.merchant.portal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "permission")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Long id;

    @Column(name = "permission_name", unique = true, nullable = false)
    private String permissionName;

    @Column(name = "description")
    private String description;

    public Permission(String permissionName, String description) {
        this.permissionName = permissionName;
        this.description = description;
    }
}

