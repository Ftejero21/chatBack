package com.chat.chat.DTO;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class BulkEmailRequestDTO {
    private String audienceMode;
    private List<Long> userIds = new ArrayList<>();
    private List<String> recipientEmails = new ArrayList<>();
    private String subject;
    private String body;
    private Integer attachmentCount;
    private List<BulkEmailAttachmentMetaDTO> attachmentsMeta = new ArrayList<>();
    @JsonAlias({"fechaProgramada", "scheduled_at"})
    private Instant scheduledAt;
    private String scheduledAtLocal;

    public String getAudienceMode() {
        return audienceMode;
    }

    public void setAudienceMode(String audienceMode) {
        this.audienceMode = audienceMode;
    }

    public List<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<Long> userIds) {
        this.userIds = userIds;
    }

    public List<String> getRecipientEmails() {
        return recipientEmails;
    }

    public void setRecipientEmails(List<String> recipientEmails) {
        this.recipientEmails = recipientEmails;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Integer getAttachmentCount() {
        return attachmentCount;
    }

    public void setAttachmentCount(Integer attachmentCount) {
        this.attachmentCount = attachmentCount;
    }

    public List<BulkEmailAttachmentMetaDTO> getAttachmentsMeta() {
        return attachmentsMeta;
    }

    public void setAttachmentsMeta(List<BulkEmailAttachmentMetaDTO> attachmentsMeta) {
        this.attachmentsMeta = attachmentsMeta;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public String getScheduledAtLocal() {
        return scheduledAtLocal;
    }

    public void setScheduledAtLocal(String scheduledAtLocal) {
        this.scheduledAtLocal = scheduledAtLocal;
    }
}
