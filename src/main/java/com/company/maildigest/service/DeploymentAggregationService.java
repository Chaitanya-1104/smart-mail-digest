package com.company.maildigest.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service public class DeploymentAggregationService {

public record DeploymentNotification(
        String serviceName,
        String version,
        Date receivedDate
) {}

public record DeploymentSummary(
        String serviceName,
        String latestVersion,
        int notificationCount,
        Date latestDeploymentDate
) {}

public List<DeploymentSummary> aggregate(
        List<DeploymentNotification> notifications) {

    Map<String, List<DeploymentNotification>> grouped =
            new TreeMap<>();

    for (DeploymentNotification notification : notifications) {

        grouped.computeIfAbsent(
                notification.serviceName(),
                key -> new ArrayList<>()
        ).add(notification);
    }

    List<DeploymentSummary> summaries = new ArrayList<>();

    for (Map.Entry<String, List<DeploymentNotification>> entry
            : grouped.entrySet()) {

        List<DeploymentNotification> deployments = entry.getValue();

        DeploymentNotification latest = deployments.stream()
                .max(Comparator.comparing(
                        DeploymentNotification::receivedDate
                ))
                .orElseThrow();

        summaries.add(new DeploymentSummary(
                entry.getKey(),
                latest.version(),
                deployments.size(),
                latest.receivedDate()
        ));
    }

    return summaries;
}

public void printSummary(List<DeploymentSummary> summaries) {

    System.out.println("\n");
    System.out.println("==================================================");
    System.out.println("       DEPLOYMENT SUMMARY - LAST 24 HOURS");
    System.out.println("==================================================");

    int totalNotifications = summaries.stream()
            .mapToInt(DeploymentSummary::notificationCount)
            .sum();

    System.out.println("Total Services       : " + summaries.size());
    System.out.println("Total Notifications  : " + totalNotifications);

    System.out.println("--------------------------------------------------");

    System.out.printf(
            "%-35s %-15s %-10s%n",
            "SERVICE", "LATEST VERSION", "COUNT"
    );

    System.out.println("--------------------------------------------------");

    for (DeploymentSummary summary : summaries) {

        System.out.printf(
                "%-35s %-15s %-10d%n",
                summary.serviceName(),
                summary.latestVersion(),
                summary.notificationCount()
        );
    }

    System.out.println("==================================================");
}


}