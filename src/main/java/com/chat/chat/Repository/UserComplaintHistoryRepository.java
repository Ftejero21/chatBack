package com.chat.chat.Repository;

import com.chat.chat.Entity.UserComplaintHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface UserComplaintHistoryRepository extends JpaRepository<UserComplaintHistoryEntity, Long> {

    List<UserComplaintHistoryEntity> findByComplaintIdOrderByCreatedAtAsc(Long complaintId);

    List<UserComplaintHistoryEntity> findByComplaintIdInOrderByCreatedAtAsc(Collection<Long> complaintIds);
}
