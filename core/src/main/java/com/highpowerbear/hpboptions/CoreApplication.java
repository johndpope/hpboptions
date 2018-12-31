package com.highpowerbear.hpboptions;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Created by robertk on 10/26/2018.
 */
@SpringBootApplication
@EnableJms
@EnableScheduling
@EnableAsync
public class CoreApplication {
}