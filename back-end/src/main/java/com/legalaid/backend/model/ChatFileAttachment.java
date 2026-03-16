package com.legalaid.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_file_attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatFileAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "data", nullable = false, columnDefinition = "BYTEA")
    private byte[] data;

    @ManyToOne
    @JoinColumn(name = "chat_message_id", referencedColumnName = "id", nullable = false)
    @JsonIgnore
    private ChatMessage chatMessage;
}

