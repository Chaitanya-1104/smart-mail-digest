package com.company.maildigest.service;

import org.springframework.stereotype.Service;

import java.util.Locale;

@Service public class ImportantEmailDetectionService {

public enum Importance {
    HIGH,
    MEDIUM,
    LOW
}

public record EmailAnalysis(
        String category,
        Importance importance,
        String reason
) {}

public EmailAnalysis analyze(String subject, String content) {

    String text = (
            (subject == null ? "" : subject) + " " +
            (content == null ? "" : content)
    ).toLowerCase(Locale.ROOT);

    if (containsAny(text,
            "production down",
            "production issue",
            "system outage",
            "critical issue",
            "service unavailable",
            "security incident")) {

        return new EmailAnalysis(
                "PRODUCTION_ALERT",
                Importance.HIGH,
                "Production or critical issue detected"
        );
    }

    if (containsAny(text,
            "action required",
            "immediate action",
            "response required",
            "please complete",
            "deadline",
            "urgent")) {

        return new EmailAnalysis(
                "ACTION_REQUIRED",
                Importance.HIGH,
                "Action or urgency indicators detected"
        );
    }

    if (containsAny(text,
            "meeting invitation",
            "meeting scheduled",
            "schedule a meeting",
            "calendar invite")) {

        return new EmailAnalysis(
                "MEETING",
                Importance.MEDIUM,
                "Meeting-related communication"
        );
    }

    if (containsAny(text,
            "project update",
            "project status",
            "requirement discussion",
            "development update",
            "release discussion")) {

        return new EmailAnalysis(
                "PROJECT_COMMUNICATION",
                Importance.MEDIUM,
                "Project-related communication"
        );
    }

    if (containsAny(text,
            "architecture",
            "technical discussion",
            "code review",
            "database design",
            "api changes")) {

        return new EmailAnalysis(
                "TECHNICAL_DISCUSSION",
                Importance.MEDIUM,
                "Technical discussion detected"
        );
    }

    if (containsAny(text,
            "hr policy",
            "company announcement",
            "leave policy",
            "payroll",
            "employee benefits")) {

        return new EmailAnalysis(
                "HR_COMMUNICATION",
                Importance.MEDIUM,
                "HR-related communication"
        );
    }

    return new EmailAnalysis(
            "GENERAL",
            Importance.LOW,
            "No predefined importance indicators found"
    );
}

private boolean containsAny(String text, String... keywords) {

    for (String keyword : keywords) {

        if (text.contains(keyword)) {
            return true;
        }
    }

    return false;
}


}