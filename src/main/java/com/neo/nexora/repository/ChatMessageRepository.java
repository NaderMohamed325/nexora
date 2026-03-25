package com.neo.nexora.repository;

import com.neo.nexora.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
}
