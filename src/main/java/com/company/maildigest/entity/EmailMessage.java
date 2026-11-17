package com.company.maildigest.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table( name = "email_messages", uniqueConstraints = { @UniqueConstraint( name = "uk_email_message_id", columnNames = "message_id" ) } ) 
public class EmailMessage {

@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

@Column(name = "message_id", nullable = false, unique = true, length = 500)
private String messageId;

private String subject;

private String sender;

private LocalDateTime receivedDate;

@Column(columnDefinition = "TEXT")
private String body;

private String category;

private String importance;

private String conversationId;

public EmailMessage() {
}

public Long getId() {
    return id;
}

public String getMessageId() {
    return messageId;
}

public void setMessageId(String messageId) {
    this.messageId = messageId;
}

public String getSubject() {
    return subject;
}

public void setSubject(String subject) {
    this.subject = subject;
}

public String getSender() {
    return sender;
}

public void setSender(String sender) {
    this.sender = sender;
}

public LocalDateTime getReceivedDate() {
    return receivedDate;
}

public void setReceivedDate(LocalDateTime receivedDate) {
    this.receivedDate = receivedDate;
}

public String getBody() {
    return body;
}

public void setBody(String body) {
    this.body = body;
}

public String getCategory() {
    return category;
}

public void setCategory(String category) {
    this.category = category;
}

public String getImportance() {
    return importance;
}

public void setImportance(String importance) {
    this.importance = importance;
}

public String getConversationId() {
    return conversationId;
}

public void setConversationId(String conversationId) {
    this.conversationId = conversationId;
}


}