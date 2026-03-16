package com.legalaid.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.legalaid.backend.model.ChatMessage;

@Repository
public interface ChatRepository extends JpaRepository<ChatMessage, Integer> {

    // Load full chat history with file attachments
    @Query("SELECT DISTINCT c FROM ChatMessage c LEFT JOIN FETCH c.fileAttachments WHERE c.matchId = :matchId ORDER BY c.createdAt ASC")
    List<ChatMessage> findByMatchIdOrderByCreatedAtAsc(@Param("matchId") Integer matchId);
    // Get last message per match (for chat list UI)
    @Query("""
        SELECT c
        FROM ChatMessage c
        WHERE c.matchId IN :matchIds
        AND c.createdAt = (
            SELECT MAX(c2.createdAt)
            FROM ChatMessage c2
            WHERE c2.matchId = c.matchId
        )
    """)
    List<ChatMessage> findLastMessagesByMatchIds(List<Integer> matchIds);

}
