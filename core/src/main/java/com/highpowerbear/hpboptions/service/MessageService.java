package com.highpowerbear.hpboptions.service;

import com.highpowerbear.hpboptions.common.HopSettings;
import com.highpowerbear.hpboptions.enums.DataField;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.enums.WsTopic;
import com.highpowerbear.hpboptions.model.DataHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public MessageService(JavaMailSender emailSender, JmsTemplate jmsTemplate, SimpMessagingTemplate simpMessagingTemplate) {
        this.emailSender = emailSender;
        this.jmsTemplate = jmsTemplate;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    public void sendEmailMessage(String subject, String text) {
        log.info("sending email message: " + subject);

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(HopSettings.EMAIL_FROM);
        message.setTo(HopSettings.EMAIL_TO);
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
        String prefix = HopSettings.WS_TOPIC_PREFIX;
        String topic;

        switch (type) {
            case UNDERLYING: topic = prefix + WsTopic.UNDERLYING.suffix(); break;
            case ORDER: topic = prefix + WsTopic.ORDER.suffix(); break;
            case POSITION: topic = prefix + WsTopic.POSITION.suffix(); break;
            case CHAIN: topic = prefix + WsTopic.CHAIN.suffix(); break;
            default: throw new IllegalStateException("no ws topic for " + type);
        }
        simpMessagingTemplate.convertAndSend(topic, message);
    }

    public void sendWsMessage(DataHolder dataHolder, DataField field) {
        if (dataHolder.isSendMessage(field)) {
            sendWsMessage(dataHolder.getType(), dataHolder.createMessage(field));
        }
    }

    public void sendWsReloadRequestMessage(DataHolderType type) {
        sendWsMessage(type, "reloadRequest");
    }
}
