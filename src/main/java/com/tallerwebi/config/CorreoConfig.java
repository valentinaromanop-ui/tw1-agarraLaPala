package com.tallerwebi.config;

import java.util.Properties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@PropertySource(value = "file:correo-local.properties", ignoreResourceNotFound = true)
@EnableAsync
public class CorreoConfig {

  @Bean
  public JavaMailSender mailSender(Environment entorno) {
    JavaMailSenderImpl sender = new JavaMailSenderImpl();
    sender.setHost(entorno.getProperty("smtp.host", "localhost"));
    sender.setPort(entorno.getProperty("smtp.port", Integer.class, 587));
    sender.setUsername(entorno.getProperty("smtp.usuario", ""));
    sender.setPassword(entorno.getProperty("smtp.password", ""));
    sender.setDefaultEncoding("UTF-8");
    Properties propiedades = sender.getJavaMailProperties();
    propiedades.setProperty("mail.smtp.auth", entorno.getProperty("smtp.auth", "true"));
    propiedades.setProperty("mail.smtp.starttls.enable", entorno.getProperty("smtp.tls", "true"));
    propiedades.setProperty("mail.smtp.starttls.required", entorno.getProperty("smtp.tls", "true"));
    propiedades.setProperty("mail.smtp.connectiontimeout", "5000");
    propiedades.setProperty("mail.smtp.timeout", "5000");
    propiedades.setProperty("mail.smtp.writetimeout", "5000");
    return sender;
  }

  @Bean
  public ThreadPoolTaskExecutor ejecutorCorreos() {
    ThreadPoolTaskExecutor ejecutor = new ThreadPoolTaskExecutor();
    ejecutor.setCorePoolSize(1);
    ejecutor.setMaxPoolSize(1);
    ejecutor.setQueueCapacity(100);
    ejecutor.setThreadNamePrefix("correo-");
    return ejecutor;
  }
}
