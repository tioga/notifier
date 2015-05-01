package org.tiogasolutions.notify.engine.core;

import org.tiogasolutions.notify.engine.web.NotifyApplication;
import org.tiogasolutions.runners.jersey.support.JerseySpringBridge;
import org.tiogasolutions.runners.jersey.support.ResourceConfigAdapter;
import org.glassfish.jersey.test.JerseyTestNg;
import org.tiogasolutions.notify.kernel.TestFactory;
import org.tiogasolutions.notify.kernel.domain.DomainKernel;
import org.tiogasolutions.notify.kernel.execution.ExecutionManager;
import org.tiogasolutions.notify.kernel.notification.NotificationKernel;
import org.springframework.beans.factory.BeanFactory;

import javax.ws.rs.core.Application;

public class AbstractEngineJaxRsTest extends JerseyTestNg.ContainerPerClassTest {

  private NotifyApplication application;
  private TestFactory testFactory;

  @Override
  protected Application configure() {
    application = new NotifyApplication("test", "classpath:/config/spring-test-lq-engine.xml");

    ResourceConfigAdapter adapter = new ResourceConfigAdapter(application);
    adapter.register(new JerseySpringBridge(application.getBeanFactory()));

    testFactory = new TestFactory(getDomainKernel(), getNotificationKernel());

    return adapter;
  }

  public String toHttpAuth(String username, String password) {
    return getTestFactory().toHttpAuth(username, password);
  }

  public TestFactory getTestFactory() {
    return testFactory;
  }

  public NotifyApplication getApplication() {
    return application;
  }

  public BeanFactory getBeanFactory() {
    return application.getBeanFactory();
  }

  public DomainKernel getDomainKernel() {
    return getBeanFactory().getBean(DomainKernel.class);
  }

  public NotificationKernel getNotificationKernel() {
    return getBeanFactory().getBean(NotificationKernel.class);
  }

  public ExecutionManager getExecutionManager() {
    return getBeanFactory().getBean(ExecutionManager.class);
  }
}
