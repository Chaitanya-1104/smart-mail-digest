package com.company.maildigest.service;

import jakarta.mail.Message; 
import jakarta.mail.MessagingException; 
import jakarta.mail.internet.MimeMessage; 
import org.springframework.stereotype.Service;

import java.util.*; import java.util.regex.Matcher; import java.util.regex.Pattern;

@Service public class EmailConversationService {

public record ConversationMessage(
        String messageId,
        String inReplyTo,
        List<String> references,
        String subject,
        String sender,
        String content,
        Date receivedDate
) {}

public record EmailConversation(
        String conversationId,
        String subject,
        List<ConversationMessage> messages
) {}

public List<ConversationMessage> extractMessages(
        Message[] messages) throws MessagingException {

    List<ConversationMessage> result = new ArrayList<>();

    for (Message message : messages) {
        

        String messageId = getHeader(message, "Message-ID");
        String inReplyTo = getHeader(message, "In-Reply-To");

        String referencesHeader =
                getHeader(message, "References");

        List<String> references =
                extractMessageIds(referencesHeader);

        String sender = message.getFrom() != null
                && message.getFrom().length > 0
                ? message.getFrom()[0].toString()
                : "Unknown";

        result.add(new ConversationMessage(
                messageId,
                inReplyTo,
                references,
                message.getSubject(),
                sender,
                "",
                message.getReceivedDate()
        ));
    }

    return result;
}

public List<EmailConversation> groupConversations(
        List<ConversationMessage> messages) {

    Map<String, List<ConversationMessage>> groups =
            new LinkedHashMap<>();

    Map<String, String> messageToThread = new HashMap<>();

    for (ConversationMessage message : messages) {

        String threadId = resolveThreadId(
                message,
                messageToThread
        );

        groups.computeIfAbsent(
                threadId,
                key -> new ArrayList<>()
        ).add(message);

        if (message.messageId() != null) {
            messageToThread.put(message.messageId(), threadId);
        }
    }

    List<EmailConversation> conversations = new ArrayList<>();

    for (Map.Entry<String, List<ConversationMessage>> entry
            : groups.entrySet()) {

        List<ConversationMessage> threadMessages = entry.getValue();

        threadMessages.sort(
                Comparator.comparing(
                        ConversationMessage::receivedDate,
                        Comparator.nullsLast(Comparator.naturalOrder())
                )
        );

        String subject = threadMessages.get(0).subject();

        conversations.add(new EmailConversation(
                entry.getKey(),
                subject,
                threadMessages
        ));
    }

    return conversations;
}

private String resolveThreadId( ConversationMessage message, Map<String, String> messageToThread) {

    // 1. Check In-Reply-To
    if (message.inReplyTo() != null) {

        String parentThread =
                messageToThread.get(message.inReplyTo());

        if (parentThread != null) {
            return parentThread;
        }

        // Parent isn't in the current batch.
        // Use its Message-ID as the conversation ID.
        return message.inReplyTo();
    }

    // 2. Check References
    if (message.references() != null
            && !message.references().isEmpty()) {

        String firstReference = message.references().get(0);

        String existingThread =
                messageToThread.get(firstReference);

        if (existingThread != null) {
            return existingThread;
        }

        return firstReference;
    }

    // 3. New conversation
    if (message.messageId() != null) {
        return message.messageId();
    }

    return "UNKNOWN-" + UUID.randomUUID();

}

private String getHeader(Message message, String name)
        throws MessagingException {

    String[] values = message.getHeader(name);

    return values != null && values.length > 0
            ? values[0].trim()
            : null;
}

private List<String> extractMessageIds(String header) {

    if (header == null || header.isBlank()) {
        return Collections.emptyList();
    }
    
    List<String> ids = new ArrayList<>();

    Matcher matcher = Pattern.compile("<[^>]+>")
            .matcher(header);

    while (matcher.find()) {
        
        ids.add(matcher.group());
    }

    return ids;
}


}