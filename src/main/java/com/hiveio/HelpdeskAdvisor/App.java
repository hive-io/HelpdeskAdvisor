package com.hiveio.HelpdeskAdvisor;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.MapMessage;
import javax.jms.MessageProducer;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;

import java.util.Map;
import java.util.Enumeration;
import java.util.concurrent.Future;

public class App
{
    private static final String USER = "system";
    private static final String PASSWORD = "manager";
    private static final int DEFAULT_COUNT = 10;
    private static final int DELIVERY_MODE = DeliveryMode.NON_PERSISTENT;

    public static void printMapMessage(ObjectMessage msg) throws Exception {
      Map<String, ?> message = (Map<String, ?>)msg.getObject();
      for (Map.Entry<String, ?> entry : message.entrySet()) {
        System.out.println(entry.getKey() + "=" + entry.getValue());
      }
    }

    public static void main(String[] args) throws Exception {
        String guestName;
        if (args.length == 0) {
            guestName = "W770008";
        } else {
            guestName = args[0];
        }

        try {
            Context context = new InitialContext();
            ConnectionFactory factory = (ConnectionFactory) context.lookup("myFactoryLookup");
            Destination guestActionRequestTopic = (Destination) context.lookup("guestActionRequest");
            Destination guestActionResponseTopic = (Destination) context.lookup("guestActionResponse");

            Connection connection = factory.createConnection(USER, PASSWORD);
            connection.setExceptionListener(new MyExceptionListener());
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer messageProducer = session.createProducer(guestActionRequestTopic);
            MessageConsumer messageConsumer = session.createConsumer(guestActionResponseTopic);

            System.out.println("Sending message command to: " + guestName);
            Guest testGuest = new Guest(guestName, session, messageProducer, messageConsumer);

            // sending a message
            // testGuest.sendMessage("some title", "some message");

            // getting application logs
            Future<Map> logs = testGuest.getApplicationLogs();
            while (!logs.isDone()) {
              // simulate some waiting and other processing
              Thread.sleep(1); // adding delay for example
            }
            System.out.println("received a response: " + logs.get());

            connection.close();
        } catch (Exception exp) {
            System.out.println("Caught exception, exiting.");
            exp.printStackTrace(System.out);
            System.exit(1);
        }
    }

    private static class MyExceptionListener implements ExceptionListener {
        @Override
        public void onException(JMSException exception) {
            System.out.println("Connection ExceptionListener fired, exiting.");
            exception.printStackTrace(System.out);
            System.exit(1);
        }
    }
}
