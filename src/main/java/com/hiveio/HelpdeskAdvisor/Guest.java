package com.hiveio.HelpdeskAdvisor;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.MapMessage;
import javax.jms.MessageProducer;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;

public class Guest
{
  String guestName;
  Session session;
  MessageProducer producer;
  MessageConsumer consumer;
  ExecutorService executor;

  public Guest(String guestName, Session session, MessageProducer producer, MessageConsumer consumer) {
    this.guestName = guestName;
    this.session = session;
    this.producer = producer;
    this.consumer = consumer;
    this.executor = Executors.newFixedThreadPool(1);
  }

  public void sendMessage(String title, String message) throws Exception {
    MapMessage request = this.session.createMapMessage();
    request.setString("title", title);
    request.setString("message", message);
    request.setStringProperty("action", "MESSAGE");
    request.setJMSType(this.guestName);

    this.producer.send(request, DeliveryMode.NON_PERSISTENT, Message.DEFAULT_PRIORITY, Message.DEFAULT_TIME_TO_LIVE);
  }

  public Future<Map> getApplicationLogs() throws Exception {
    Message request = this.session.createMessage();
    request.setStringProperty("action", "PUBAPP");
    request.setJMSType(this.guestName);
    return this.executor.submit(new AgentMapRequest(request));
  }

  private class AgentMapRequest implements Callable<Map> {
    Message request;
    public AgentMapRequest(Message request) {
      this.request = request;
    }

    @Override
    public Map call() throws Exception {
      producer.send(this.request, DeliveryMode.NON_PERSISTENT, Message.DEFAULT_PRIORITY, Message.DEFAULT_TIME_TO_LIVE);

      // NOTE: generally what you would do here is actually check for the
      //       correlation id of the message you sent above, like so:
      /*
      while (true) {
        ObjectMessage message = (ObjectMessage)consumer.receive(5000);
        if (message.getJMSCorrelationID() == whatWeAreLookingFor)
          return (Map<String, ?>)message.getObject();
      }
      */

      // for simplicity I'll just use the immediately returned message
      ObjectMessage message = (ObjectMessage)consumer.receive(5000);
      return (Map<String, ?>)message.getObject();
    }
  }

}
