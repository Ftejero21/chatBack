package com.chat.chat.Service.AiService;

public enum AiMessageSearchSenderScope {
    AUTHENTICATED_USER,
    SPECIFIC_OTHER_USER,
    MULTIPLE_POSSIBLE_USERS,
    ANY_PARTICIPANT,
    /** All messages received by authenticated user — emisorId != userId, no specific person. */
    RECEIVED_MESSAGES
}
