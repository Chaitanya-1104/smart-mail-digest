package com.company.maildigest.service;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Properties;
import jakarta.mail.search.*;
import java.util.Calendar;
import java.util.Date;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
@Service
public class EmailFetchService {

    private static final String HOST = "mapi.cloudzimail.com";
    private static final int PORT = 993;
    private final EmailClassificationService classificationService;
    private final DeploymentAggregationService aggregationService;
    private final EmailContentExtractor contentExtractor;
    private final ImportantEmailDetectionService importantService;
    private final EmailConversationService conversationService;

    private record EmailRecord(
        String sender,
        String subject,
        String category,
        String importance,
        String reason,
        String content,
        Date receivedDate
    ) {}

    public EmailFetchService(
            EmailClassificationService classificationService, DeploymentAggregationService aggregationService,
             EmailContentExtractor contentExtractor, ImportantEmailDetectionService importantService, EmailConversationService conversationService) {
        this.classificationService = classificationService;
        this.aggregationService = aggregationService;
        this.contentExtractor = contentExtractor;
        this.importantService = importantService;
        this.conversationService = conversationService;
    }

    public void fetchLatestEmails() throws Exception {

       Dotenv dotenv = Dotenv.configure()
        .ignoreIfMissing()
        .load();

        String username = dotenv.get("MAIL_USERNAME");
        String password = dotenv.get("MAIL_PASSWORD");

        if (username == null || password == null) {
            throw new IllegalStateException("Email credentials missing");
        }

        Properties properties = new Properties();

        properties.put("mail.store.protocol", "imaps");
        properties.put("mail.imaps.host", HOST);
        properties.put("mail.imaps.port", PORT);
        properties.put("mail.imaps.ssl.enable", "true");
        properties.put("mail.imaps.connectiontimeout", "10000");
        properties.put("mail.imaps.timeout", "10000");

        Session session = Session.getInstance(properties);

        Store store = session.getStore("imaps");

        try {
            System.out.println("Connecting to Zimbra...");

            store.connect(HOST, PORT, username, password);

            System.out.println("Connected successfully!");

            Folder inbox = store.getFolder("INBOX");

            inbox.open(Folder.READ_ONLY);

            // Exact rolling 24-hour window
            Date cutoff = Date.from(
                    Instant.now().minusSeconds(24 * 60 * 60)
            );

            // IMAP SINCE searches by calendar date.
            // Search from yesterday to avoid missing emails
            // near the 24-hour boundary.
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(cutoff);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            calendar.add(Calendar.DAY_OF_MONTH, -1);

            Date searchDate = calendar.getTime();

            SearchTerm searchTerm = new ReceivedDateTerm(
                    ComparisonTerm.GE,
                    searchDate
            );

            Message[] candidates = inbox.search(searchTerm);

            // Filter exact last 24 hours in Java
            Message[] messages = Arrays.stream(candidates)
                    .filter(message -> {
                        try {
                            Date receivedDate = message.getReceivedDate();

                            return receivedDate != null
                                    && !receivedDate.before(cutoff);
                        } catch (MessagingException e) {
                            return false;
                        }
                    })
                    .sorted(Comparator.comparing(message -> {
                        try {
                            return message.getReceivedDate();
                        } catch (MessagingException e) {
                            return new Date(0);
                        }
                    }))
                    .toArray(Message[]::new);

            System.out.println("\n========== EMAIL SUMMARY ==========");
            System.out.println("Period: Last 24 hours");
            System.out.println("Total emails: " + messages.length);

            System.out.println("\n========== EMAIL DETAILS ==========\n");
            List<DeploymentAggregationService.DeploymentNotification> deploymentNotifications = new ArrayList<>();
            
            List<EmailRecord> highPriorityEmails = new ArrayList<>();

            List<EmailRecord> mediumPriorityEmails = new ArrayList<>();

            List<EmailRecord> lowPriorityEmails = new ArrayList<>();
            List<Message> regularMessages = new ArrayList<>();

            for (Message message : messages) {

                Address[] from = message.getFrom();

                String sender = from != null && from.length > 0
                        ? from[0].toString()
                        : "Unknown";

                String subject = message.getSubject();

                EmailClassificationService.EmailCategory category =
                        classificationService.classify(sender, subject);

                if (category != EmailClassificationService.EmailCategory.NEXUS_DEPLOYMENT) {
                    regularMessages.add(message);
                }
                if (category ==
                        EmailClassificationService.EmailCategory.NEXUS_DEPLOYMENT) {

                    EmailClassificationService.NexusDeployment deployment =
                            classificationService.extractDeployment(subject);

                    if (deployment != null) {

                        deploymentNotifications.add(
                                new DeploymentAggregationService.DeploymentNotification(
                                        deployment.serviceName(),
                                        deployment.version(),
                                        message.getReceivedDate()
                                )
                        );
                    }

                    continue;
                }

                // Handle regular emails
                String emailContent = contentExtractor.extractContent(message);

                ImportantEmailDetectionService.EmailAnalysis analysis =
                        importantService.analyze(subject, emailContent);

                EmailRecord record = new EmailRecord(
                        sender,
                        subject,
                        analysis.category(),
                        analysis.importance().name(),
                        analysis.reason(),
                        emailContent,
                        message.getReceivedDate()
                );

                switch (analysis.importance()) {

                    case HIGH -> highPriorityEmails.add(record);

                    case MEDIUM -> mediumPriorityEmails.add(record);

                    case LOW -> lowPriorityEmails.add(record);
                }
            }

            List<DeploymentAggregationService.DeploymentSummary> summaries =
            aggregationService.aggregate(deploymentNotifications);

            aggregationService.printSummary(summaries);

            System.out.println("\n========== EMAIL ANALYSIS ==========");

            System.out.println("Total Emails: " + messages.length);

            System.out.println("Nexus Notifications: " + deploymentNotifications.size());

            System.out.println("High Priority: " + highPriorityEmails.size());

            System.out.println("Medium Priority: " + mediumPriorityEmails.size());

            System.out.println("Low Priority: " + lowPriorityEmails.size());

            System.out.println("\n========== HIGH PRIORITY EMAILS ==========");

            for (EmailRecord email : highPriorityEmails) {

                System.out.println("----------------------------------");
                System.out.println("Subject: " + email.subject());
                System.out.println("From: " + email.sender());
                System.out.println("Category: " + email.category());
                System.out.println("Reason: " + email.reason());
            }

            System.out.println("\n========== MEDIUM PRIORITY EMAILS ==========");

            for (EmailRecord email : mediumPriorityEmails) {

                System.out.println("----------------------------------");
                System.out.println("Subject: " + email.subject());
                System.out.println("From: " + email.sender());
                System.out.println("Category: " + email.category());
            }

            System.out.println("\n========== LOW PRIORITY EMAILS ==========");

            for (EmailRecord email : lowPriorityEmails) {

                System.out.println("----------------------------------");
                System.out.println("Subject: " + email.subject());
                System.out.println("From: " + email.sender());
                System.out.println("Category: " + email.category());
            }   
            List<EmailConversationService.ConversationMessage>
            conversationMessages =
            conversationService.extractMessages(regularMessages.toArray(new Message[0]));

            List<EmailConversationService.EmailConversation>
                    conversations =
                    conversationService.groupConversations(conversationMessages);

            System.out.println("\n========== EMAIL CONVERSATIONS ==========");

            for (EmailConversationService.EmailConversation conversation
                    : conversations) {

                System.out.println("----------------------------------");
                System.out.println("Subject: " + conversation.subject());
                System.out.println("Messages: " + conversation.messages().size());
                System.out.println("Conversation ID: "
                        + conversation.conversationId());
            }

            inbox.close(false);

        } finally {
            if (store.isConnected()) {
                store.close();
            }
        }

    }

    private String getText(Part part) throws Exception {

        if (part.isMimeType("text/plain")) {
            Object content = part.getContent();
            return content.toString();
        }

        if (part.isMimeType("text/html")) {
            return "[HTML email content]";
        }

        if (part.isMimeType("multipart/*")) {

            Multipart multipart = (Multipart) part.getContent();

            for (int i = 0; i < multipart.getCount(); i++) {

                String result = getText(multipart.getBodyPart(i));

                if (result != null && !result.isBlank()) {
                    return result;
                }
            }
        }

        return "";
    }
}
