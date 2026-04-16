package com.chat.chat.DTO;

import java.util.ArrayList;
import java.util.List;

public class BulkEmailResponseDTO {
    private boolean ok;
    private int sentCount;
    private int failedCount;
    private List<BulkEmailItemResponseDTO> items = new ArrayList<>();
    private String message;

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public int getSentCount() {
        return sentCount;
    }

    public void setSentCount(int sentCount) {
        this.sentCount = sentCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public List<BulkEmailItemResponseDTO> getItems() {
        return items;
    }

    public void setItems(List<BulkEmailItemResponseDTO> items) {
        this.items = items;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
