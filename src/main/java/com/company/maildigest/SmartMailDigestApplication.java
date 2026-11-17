package com.company.maildigest;

import com.company.maildigest.service.EmailFetchService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class SmartMailDigestApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure()
            .ignoreIfMissing()
            .load();

        dotenv.entries().forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue())
        );

        SpringApplication.run(
                SmartMailDigestApplication.class, args
        );
    }

    @Bean
    CommandLineRunner run(EmailFetchService emailFetchService) {
        return args -> emailFetchService.fetchLatestEmails();
    }
}
