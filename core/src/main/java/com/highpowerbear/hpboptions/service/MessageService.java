package com.highpowerbear.hpboptions.service;

import com.highpowerbear.hpboptions.common.HopSettings;
import com.highpowerbear.hpboptions.field.DataField;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.enums.WsTopic;
import com.highpowerbear.hpboptions.dataholder.MarketDataHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Created by robertk on 10/28/2018.
 */
@Service
public class MessageService {
    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    private final JavaMailSender emailSender;
    private final JmsTemplate jmsTemplate;
    private final SimpMessagingTemplate simpMessagingTemplate;

    @Value("${email.from}")
    private String emailFrom;
    @Value("${email.to}")
    private String emailTo;

    @Autowired
    public MessageService(JavaMailSender emailSender, JmsTemplate jmsTemplate, SimpMessagingTemplate simpMessagingTemplate) {
        this.emailSender = emailSender;
        this.jmsTemplate = jmsTemplate;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    public void sendEmailMessage(String subject, String text) {
        log.info("sending email message: " + subject);

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(emailFrom);
        message.setTo(emailTo);
        message.setSubject(subject);
        message.setText(text);

        emailSender.send(message);
    }

    public void sendJmsMesage(String destination, Object message) {
        //log.info("sending jms message " + destination + ": " + message);
        jmsTemplate.convertAndSend(destination, message);
    }

    public void sendWsMessage(WsTopic topic, String message) {
        simpMessagingTemplate.convertAndSend(HopSettings.WS_TOPIC_PREFIX + topic.suffix(), message);
    }

    public void sendWsMessage(DataHolderType type, String message) {
        WsTopic topic;

        switch (type) {
            case ACCOUNT: topic = WsTopic.ACCOUNT; break;
            case UNDERLYING: topic = WsTopic.UNDERLYING; break;
            case LINEAR: topic = WsTopic.LINEAR; break;
            case ORDER: topic = WsTopic.ORDER; break;
            case POSITION: topic = WsTopic.POSITION; break;
            case CHAIN: topic = WsTopic.CHAIN; break;
            case SCANNER: topic = WsTopic.SCANNER; break;
            default: throw new IllegalStateException("no ws topic for " + type);
        }
        sendWsMessage(topic, message);
    }

    public void sendWsMessage(MarketDataHolder mdh, DataField field) {
        if (mdh.isSendMessage(field)) {
            sendWsMessage(mdh.getType(), mdh.createMessage(field));
        }
    }

    public void sendWsReloadRequestMessage(DataHolderType type) {
        sendWsMessage(type, "reloadRequest");
    }

    public void sendWsReloadRequestMessage(WsTopic topic) {
        sendWsMessage(topic, "reloadRequest");
    }
}
