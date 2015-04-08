package org.lqnotify.processor.logger;

import org.lqnotify.kernel.processor.ProcessorType;
import org.lqnotify.kernel.processor.TaskProcessor;
import org.lqnotify.pub.DomainProfile;
import org.lqnotify.pub.Notification;
import org.lqnotify.pub.Task;
import org.lqnotify.pub.TaskResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;

import javax.inject.Named;

@Named
public class LoggerTaskProcessor implements TaskProcessor {

  private static final ProcessorType PROVIDER_TYPE = new ProcessorType("logger");
  private static final Logger log = LoggerFactory.getLogger(LoggerTaskProcessor.class);

  @Override
  public boolean isReady() {
    return true;
  }

  @Override
  public void init(BeanFactory beanFactory) {
  }

  @Override
  public ProcessorType getType() {
    return PROVIDER_TYPE;
  }

  @Override
  public TaskResponse processTask(DomainProfile domainProfile, Notification notification, Task task) {
    String message = notification.getSummary();
    log.warn("\n  **\n  ** {}\n  **", message.trim());
    return TaskResponse.complete("Ok");
  }
}
