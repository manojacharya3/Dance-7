package com.studioos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "roles")
public class Role {
    public enum Name { ADMIN, STAFF, OWNER, BRANCH_HEAD, INSTRUCTOR, DEVELOPER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 30)
    private Name name;

    protected Role() {}

    public Role(Name name) { this.name = name; }
    public Long getId() { return id; }
    public Name getName() { return name; }
    public void setName(Name name) { this.name = name; }
}
