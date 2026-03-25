package com.neo.nexora.repository;

import com.neo.nexora.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
}
