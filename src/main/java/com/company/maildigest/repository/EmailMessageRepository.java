package com.company.maildigest.repository;

import com.company.maildigest.entity.EmailMessage; 
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailMessageRepository extends JpaRepository<EmailMessage, Long> {

Optional<EmailMessage> findByMessageId(String messageId);

boolean existsByMessageId(String messageId);


}