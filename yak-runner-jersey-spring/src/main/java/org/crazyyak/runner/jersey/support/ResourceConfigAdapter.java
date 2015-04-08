package org.crazyyak.runner.jersey.support;

import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.core.Application;

public class ResourceConfigAdapter extends ResourceConfig {

  public ResourceConfigAdapter(Application application) {

    application.getClasses().forEach(this::register);

    application.getSingletons().forEach(this::register);

    addProperties(application.getProperties());
  }
}
