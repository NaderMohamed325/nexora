package com.neo.nexora.repository;

import com.neo.nexora.entity.ChatRoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface ChatRoomParticipantRepository extends JpaRepository<ChatRoomParticipant, Long> {
}
