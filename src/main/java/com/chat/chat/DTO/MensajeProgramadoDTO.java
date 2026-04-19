package com.chat.chat.DTO;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class MensajeProgramadoDTO {
    private Long id;
    private Long createdBy;
    private Long canceledBy;
    private Long chatId;
    private String message;
    private String contenido;
    private String body;
    private String subject;
    private String type;
    private String deliveryType;
    private String audienceMode;
    private java.util.List<Long> userIds;
    private java.util.List<String> recipientEmails;
    private Integer recipientCount;
    private String recipientLabel;
    private String recipientsSummary;
    @JsonAlias({"recipients", "recipient_users"})
    private java.util.List<ScheduledRecipientUserDTO> recipientUsers;
    private Integer attachmentCount;
    private java.util.List<String> attachmentNames;
    private java.util.List<ScheduledAttachmentMetaDTO> attachmentsMeta;
    private String messageContent;
    private Instant scheduledAt;
    private String status;
    private Integer attempts;
    private String lastError;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant sentAt;
    private String scheduledBatchId;
    private Boolean wsEmitted;
    private Instant wsEmittedAt;
    private String wsDestinations;
    private String wsEmitError;
    private Long persistedMessageId;
    private Instant canceledAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getCanceledBy() {
        return canceledBy;
    }

    public void setCanceledBy(Long canceledBy) {
        this.canceledBy = canceledBy;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDeliveryType() {
        return deliveryType;
    }

    public void setDeliveryType(String deliveryType) {
        this.deliveryType = deliveryType;
    }

    public String getAudienceMode() {
        return audienceMode;
    }

    public void setAudienceMode(String audienceMode) {
        this.audienceMode = audienceMode;
    }

    public java.util.List<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(java.util.List<Long> userIds) {
        this.userIds = userIds;
    }

    public java.util.List<String> getRecipientEmails() {
        return recipientEmails;
    }

    public void setRecipientEmails(java.util.List<String> recipientEmails) {
        this.recipientEmails = recipientEmails;
    }

    public Integer getRecipientCount() {
        return recipientCount;
    }

    public void setRecipientCount(Integer recipientCount) {
        this.recipientCount = recipientCount;
    }

    public String getRecipientLabel() {
        return recipientLabel;
    }

    public void setRecipientLabel(String recipientLabel) {
        this.recipientLabel = recipientLabel;
    }

    public String getRecipientsSummary() {
        return recipientsSummary;
    }

    public void setRecipientsSummary(String recipientsSummary) {
        this.recipientsSummary = recipientsSummary;
    }

    public java.util.List<ScheduledRecipientUserDTO> getRecipientUsers() {
        return recipientUsers;
    }

    public void setRecipientUsers(java.util.List<ScheduledRecipientUserDTO> recipientUsers) {
        this.recipientUsers = recipientUsers;
    }

    @JsonProperty("recipients")
    public java.util.List<ScheduledRecipientUserDTO> getRecipients() {
        return recipientUsers;
    }

    @JsonProperty("recipients")
    public void setRecipients(java.util.List<ScheduledRecipientUserDTO> recipients) {
        this.recipientUsers = recipients;
    }

    @JsonProperty("recipient_users")
    public java.util.List<ScheduledRecipientUserDTO> getRecipientUsersSnakeCase() {
        return recipientUsers;
    }

    @JsonProperty("recipient_users")
    public void setRecipientUsersSnakeCase(java.util.List<ScheduledRecipientUserDTO> recipientUsers) {
        this.recipientUsers = recipientUsers;
    }

    public Integer getAttachmentCount() {
        return attachmentCount;
    }

    public void setAttachmentCount(Integer attachmentCount) {
        this.attachmentCount = attachmentCount;
    }

    public java.util.List<String> getAttachmentNames() {
        return attachmentNames;
    }

    public void setAttachmentNames(java.util.List<String> attachmentNames) {
        this.attachmentNames = attachmentNames;
    }

    public java.util.List<ScheduledAttachmentMetaDTO> getAttachmentsMeta() {
        return attachmentsMeta;
    }

    public void setAttachmentsMeta(java.util.List<ScheduledAttachmentMetaDTO> attachmentsMeta) {
        this.attachmentsMeta = attachmentsMeta;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getAttempts() {
        return attempts;
    }

    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public String getScheduledBatchId() {
        return scheduledBatchId;
    }

    public void setScheduledBatchId(String scheduledBatchId) {
        this.scheduledBatchId = scheduledBatchId;
    }

    public Boolean getWsEmitted() {
        return wsEmitted;
    }

    public void setWsEmitted(Boolean wsEmitted) {
        this.wsEmitted = wsEmitted;
    }

    public Instant getWsEmittedAt() {
        return wsEmittedAt;
    }

    public void setWsEmittedAt(Instant wsEmittedAt) {
        this.wsEmittedAt = wsEmittedAt;
    }

    public String getWsDestinations() {
        return wsDestinations;
    }

    public void setWsDestinations(String wsDestinations) {
        this.wsDestinations = wsDestinations;
    }

    public String getWsEmitError() {
        return wsEmitError;
    }

    public void setWsEmitError(String wsEmitError) {
        this.wsEmitError = wsEmitError;
    }

    public Long getPersistedMessageId() {
        return persistedMessageId;
    }

    public void setPersistedMessageId(Long persistedMessageId) {
        this.persistedMessageId = persistedMessageId;
    }

    public Instant getCanceledAt() {
        return canceledAt;
    }

    public void setCanceledAt(Instant canceledAt) {
        this.canceledAt = canceledAt;
    }
}
