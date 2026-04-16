package com.chat.chat.DTO;

public class ScheduledBatchResponseDTO {
    private boolean ok;
    private Long scheduledBatchId;
    private String message;

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public Long getScheduledBatchId() {
        return scheduledBatchId;
    }

    public void setScheduledBatchId(Long scheduledBatchId) {
        this.scheduledBatchId = scheduledBatchId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
