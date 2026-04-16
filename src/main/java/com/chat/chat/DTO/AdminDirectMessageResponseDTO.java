package com.chat.chat.DTO;

import java.util.ArrayList;
import java.util.List;

public class AdminDirectMessageResponseDTO {
    private int requestedUsers;
    private int deliveredUsers;
    private List<AdminDirectMessageResultDTO> results = new ArrayList<>();

    public int getRequestedUsers() {
        return requestedUsers;
    }

    public void setRequestedUsers(int requestedUsers) {
        this.requestedUsers = requestedUsers;
    }

    public int getDeliveredUsers() {
        return deliveredUsers;
    }

    public void setDeliveredUsers(int deliveredUsers) {
        this.deliveredUsers = deliveredUsers;
    }

    public List<AdminDirectMessageResultDTO> getResults() {
        return results;
    }

    public void setResults(List<AdminDirectMessageResultDTO> results) {
        this.results = results;
    }
}
