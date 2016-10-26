package com.example;

import java.io.File;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.config.SimpleJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableJms
public class MessagingServiceJmsApplication {

	public static void main(String[] args) {
		SpringApplication.run(MessagingServiceJmsApplication.class, args);
	}
	
	@Bean // Strictly speaking this bean is not necessary as boot creates a default
    JmsListenerContainerFactory<?> myJmsContainerFactory(ConnectionFactory connectionFactory) {
        SimpleJmsListenerContainerFactory factory = new SimpleJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        return factory;
    }
}

@RestController
@RequestMapping("/messenger")
class MessageRestController {

	@Autowired
	ConfigurableApplicationContext context;

	@RequestMapping("/message/{message}")
	public void send(@PathVariable String message) {

		MessageCreator messageCreator = getMessageCreator(message);
		JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
		System.out.println("Sending a new message.");
		jmsTemplate.send("mailbox-destination", messageCreator);
	}

	public MessageCreator getMessageCreator(String message) {
		// Clean out any ActiveMQ data from a previous run
		FileSystemUtils.deleteRecursively(new File("activemq-data"));

		MessageCreator messageCreator = new MessageCreator() {
			@Override
			public Message createMessage(Session session) throws JMSException {
				return session.createTextMessage(message);
			}
		};
		return messageCreator;
	}
}

@Component
class Receiver {

    /**
     * Get a copy of the application context
     */
    @Autowired
    ConfigurableApplicationContext context;

    /**
     * When you receive a message, print it out, then shut down the application.
     * Finally, clean up any ActiveMQ server stuff.
     */
    @JmsListener(destination = "mailbox-destination", containerFactory = "myJmsContainerFactory")
    public void receiveMessage(String message) {
        System.out.println("Received <" + message + ">");
        //context.close();
        FileSystemUtils.deleteRecursively(new File("activemq-data"));
    }
}