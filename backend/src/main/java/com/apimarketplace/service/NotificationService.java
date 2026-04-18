package com.apimarketplace.service;

import com.apimarketplace.dto.notifications.NotificationEventResponse;
import com.apimarketplace.entity.NotificationEvent;
import com.apimarketplace.entity.UserAccount;
import com.apimarketplace.entity.enums.NotificationChannel;
import com.apimarketplace.entity.enums.NotificationStatus;
import com.apimarketplace.repository.NotificationEventRepository;
import com.apimarketplace.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationEventRepository notificationEventRepository;
    private final UserRepository userRepository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean emailEnabled;
    private final String emailFrom;
    private final boolean smsEnabled;
    private final String smsEndpoint;
    private final String smsApiKey;
    private final String smsSenderId;
    private final RestClient restClient;

    public NotificationService(
        NotificationEventRepository notificationEventRepository,
        UserRepository userRepository,
        ObjectProvider<JavaMailSender> mailSenderProvider,
        @Value("${app.notification.email.enabled:false}") boolean emailEnabled,
        @Value("${app.notification.email.from:no-reply@apimarketplace.local}") String emailFrom,
        @Value("${app.notification.sms.enabled:false}") boolean smsEnabled,
        @Value("${app.notification.sms.endpoint:}") String smsEndpoint,
        @Value("${app.notification.sms.api-key:}") String smsApiKey,
        @Value("${app.notification.sms.sender-id:APIMKT}") String smsSenderId
    ) {
        this.notificationEventRepository = notificationEventRepository;
        this.userRepository = userRepository;
        this.mailSenderProvider = mailSenderProvider;
        this.emailEnabled = emailEnabled;
        this.emailFrom = emailFrom;
        this.smsEnabled = smsEnabled;
        this.smsEndpoint = smsEndpoint;
        this.smsApiKey = smsApiKey;
        this.smsSenderId = smsSenderId;
        this.restClient = RestClient.builder().build();
    }

    public NotificationEventResponse sendEmail(UUID userId, String recipient, String subject, String message, String eventType) {
        String provider = "SIMULATED";
        if (emailEnabled && StringUtils.hasText(recipient)) {
            JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
            if (mailSender != null) {
                try {
                    SimpleMailMessage mail = new SimpleMailMessage();
                    mail.setFrom(emailFrom);
                    mail.setTo(recipient);
                    mail.setSubject(subject == null ? "" : subject);
                    mail.setText(message == null ? "" : message);
                    mailSender.send(mail);
                    provider = "SMTP";
                } catch (Exception ex) {
                    log.warn("SMTP delivery failed for {}: {}", recipient, ex.getMessage());
                }
            }
        }
        return save(userId, NotificationChannel.EMAIL, recipient, subject, message, eventType, provider);
    }

    public NotificationEventResponse sendSms(UUID userId, String recipient, String message, String eventType) {
        String provider = "SIMULATED";
        if (smsEnabled && StringUtils.hasText(recipient) && StringUtils.hasText(smsEndpoint)) {
            try {
                SmsGatewayRequest payload = new SmsGatewayRequest(recipient, message, smsSenderId);
                restClient.post()
                    .uri(smsEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + smsApiKey)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
                provider = "SMS_GATEWAY";
            } catch (Exception ex) {
                log.warn("SMS gateway delivery failed for {}: {}", recipient, ex.getMessage());
            }
        }
        return save(userId, NotificationChannel.SMS, recipient, null, message, eventType, provider);
    }

    public NotificationEventResponse notifyUserEmail(UUID userId, String subject, String message, String eventType) {
        UserAccount account = userRepository.findById(userId).orElse(null);
        String recipient = account == null ? null : account.getEmail();
        if (StringUtils.hasText(recipient)) {
            return sendEmail(userId, recipient, subject, message, eventType);
        }
        return save(userId, NotificationChannel.EMAIL, null, subject, message, eventType, "SIMULATED");
    }

    public List<NotificationEventResponse> listForUser(UUID userId) {
        return notificationEventRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<NotificationEventResponse> listAll() {
        return notificationEventRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public Page<NotificationEventResponse> pageNotifications(
        UUID userId,
        NotificationChannel channel,
        NotificationStatus status,
        String eventType,
        String recipient,
        Instant from,
        Instant to,
        Pageable pageable
    ) {
        Specification<NotificationEvent> spec = (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (userId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("userId"), userId));
            }
            if (channel != null) {
                predicate = cb.and(predicate, cb.equal(root.get("channel"), channel));
            }
            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(eventType)) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("eventType")), "%" + eventType.trim().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(recipient)) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("recipient")), "%" + recipient.trim().toLowerCase() + "%"));
            }
            if (from != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            return predicate;
        };
        return notificationEventRepository.findAll(spec, pageable).map(this::toResponse);
    }

    private NotificationEventResponse save(UUID userId, NotificationChannel channel, String recipient, String subject, String message, String eventType, String provider) {
        NotificationEvent event = new NotificationEvent();
        event.setUserId(userId);
        event.setChannel(channel);
        event.setRecipient(recipient);
        event.setSubject(subject);
        event.setMessage(message);
        event.setEventType(eventType);
        event.setStatus(NotificationStatus.SENT);
        event.setProvider(provider);
        event.setDeliveredAt(Instant.now());
        NotificationEvent saved = notificationEventRepository.save(event);
        log.info("Notification [{}] -> {}: {}", channel, StringUtils.hasText(recipient) ? recipient : "n/a", message);
        return toResponse(saved);
    }

    private NotificationEventResponse toResponse(NotificationEvent event) {
        return new NotificationEventResponse(
            event.getId(),
            event.getUserId(),
            event.getChannel(),
            event.getRecipient(),
            event.getSubject(),
            event.getMessage(),
            event.getEventType(),
            event.getStatus(),
            event.getProvider(),
            event.getDeliveredAt(),
            event.getCreatedAt(),
            event.getUpdatedAt()
        );
    }

    private record SmsGatewayRequest(String to, String message, String senderId) {}
}
