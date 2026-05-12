package com.chat.chat.Service.UserComplaintService;

import com.chat.chat.DTO.UserComplaintCreateDTO;
import com.chat.chat.DTO.UserComplaintDTO;
import com.chat.chat.DTO.UserComplaintEstadoUpdateDTO;
import com.chat.chat.DTO.UserExpedienteDTO;
import com.chat.chat.DTO.UserComplaintStatsDTO;
import org.springframework.data.domain.Page;
import jakarta.servlet.http.HttpServletRequest;

public interface UserComplaintService {
    UserComplaintDTO createComplaint(UserComplaintCreateDTO request);

    Page<UserComplaintDTO> listComplaints(int page, int size);

    UserComplaintStatsDTO getStats();

    UserComplaintDTO markAsRead(Long id);

    UserComplaintDTO updateStatus(Long id, UserComplaintEstadoUpdateDTO request, HttpServletRequest httpRequest);

    UserExpedienteDTO getExpediente(Long userId);
}
