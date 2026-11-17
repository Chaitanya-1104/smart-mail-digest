package com.company.maildigest.service;

import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart; 
import jakarta.mail.Part; 
import org.jsoup.Jsoup; 
import org.jsoup.nodes.Document; 
import org.springframework.stereotype.Service;

import java.io.InputStream; import java.nio.charset.StandardCharsets;

@Service public class EmailContentExtractor {

public String extractContent(Message message) {

    try {
        return extractPart(message);

    } catch (Exception e) {
        return "Unable to extract email content: " + e.getMessage();
    }
}

private String extractPart(Part part) throws Exception {

    Object content = part.getContent();

    if (content instanceof String text) {

        String contentType = part.getContentType();

        if (contentType != null &&
                contentType.toLowerCase().contains("text/html")) {

            return Jsoup.parse(text).text();
        }

        return text;
    }

    if (content instanceof Multipart multipart) {

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < multipart.getCount(); i++) {

            BodyPart bodyPart = multipart.getBodyPart(i);

            if (Part.ATTACHMENT.equalsIgnoreCase(
                    bodyPart.getDisposition())) {
                continue;
            }

            String extracted = extractPart(bodyPart);

            if (!extracted.isBlank()) {
                result.append(extracted).append("\n");
            }
        }

        return result.toString();
    }

    if (content instanceof InputStream inputStream) {
        return new String(
                inputStream.readAllBytes(),
                StandardCharsets.UTF_8
        );
    }

    return "";
}


}