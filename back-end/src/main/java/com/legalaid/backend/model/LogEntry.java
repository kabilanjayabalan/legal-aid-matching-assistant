package com.legalaid.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "logs")
@Getter
@Setter
public class LogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "log_timestamp", nullable = false)
    private LocalDateTime logTimestamp;

    @Column(name = "level", nullable = false, length = 20)
    private String level;

    @Column(name = "logger", length = 255)
    private String logger;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;
}


