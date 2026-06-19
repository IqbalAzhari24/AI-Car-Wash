package com.carwash.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_logs")
public class SystemLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String level;

    @Column(nullable = false, length = 100)
    private String logger;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "exception_trace", columnDefinition = "TEXT")
    private String exceptionTrace;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getLogger() { return logger; }
    public void setLogger(String logger) { this.logger = logger; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getExceptionTrace() { return exceptionTrace; }
    public void setExceptionTrace(String exceptionTrace) { this.exceptionTrace = exceptionTrace; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
