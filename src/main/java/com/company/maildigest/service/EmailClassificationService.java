package com.company.maildigest.service;

import org.springframework.stereotype.Service;

import java.util.regex.Matcher; import java.util.regex.Pattern;

@Service 
public class EmailClassificationService {

private static final String NEXUS_SENDER =
        "svnnotify@medplusindia.com";

private static final Pattern NEXUS_PATTERN =
        Pattern.compile(
                "^\\[([^\\]]+)]\\s+(.+?)\\s+is on nexus repository$",
                Pattern.CASE_INSENSITIVE
        );

public EmailCategory classify(String sender, String subject) {

    if (sender != null
            && sender.toLowerCase().contains(NEXUS_SENDER)) {
        return EmailCategory.NEXUS_DEPLOYMENT;
    }

    if (subject != null
            && NEXUS_PATTERN.matcher(subject.trim()).matches()) {
        return EmailCategory.NEXUS_DEPLOYMENT;
    }

    return EmailCategory.GENERAL;
}

public NexusDeployment extractDeployment(String subject) {

    if (subject == null) {
        return null;
    }

    Matcher matcher = NEXUS_PATTERN.matcher(subject.trim());

    if (matcher.matches()) {

        return new NexusDeployment(
                matcher.group(2).trim(),
                matcher.group(1).trim()
        );
    }

    return null;
}

public enum EmailCategory {
    NEXUS_DEPLOYMENT,
    IMPORTANT,
    GENERAL
}

public record NexusDeployment(
        String serviceName,
        String version
) {}


}