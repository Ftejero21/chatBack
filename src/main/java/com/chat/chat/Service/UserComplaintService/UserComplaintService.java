package com.chat.chat.Service.UserComplaintService;

import com.chat.chat.DTO.UserComplaintCreateDTO;
import com.chat.chat.DTO.UserComplaintDTO;
import com.chat.chat.DTO.UserExpedienteDTO;
import com.chat.chat.DTO.UserComplaintStatsDTO;
import org.springframework.data.domain.Page;

public interface UserComplaintService {
    UserComplaintDTO createComplaint(UserComplaintCreateDTO request);

    Page<UserComplaintDTO> listComplaints(int page, int size);

    UserComplaintStatsDTO getStats();

    UserComplaintDTO markAsRead(Long id);

    UserExpedienteDTO getExpediente(Long userId);
}
