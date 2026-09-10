package com.studioos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 120)
    private String fullName;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId = "default";
    @Column(name = "branch_id") private Long branchId;
    @Column(name = "instructor_id") private Long instructorId;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }

    protected User() {}

    public User(String email, String password, String fullName, String tenantId) {
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.tenantId = tenantId;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getFullName() { return fullName; }
    public String getTenantId() { return tenantId; }
    public Long getBranchId() { return branchId; }
    public Long getInstructorId() { return instructorId; }
    public boolean isEnabled() { return enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Set<Role> getRoles() { return roles; }
    public void setPassword(String password) { this.password = password; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }
    public void setInstructorId(Long instructorId) { this.instructorId = instructorId; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void addRole(Role role) { roles.add(role); }
}
