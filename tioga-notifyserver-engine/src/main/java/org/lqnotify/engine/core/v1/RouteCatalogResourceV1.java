package org.lqnotify.engine.core.v1;

import org.lqnotify.kernel.domain.DomainKernel;
import org.lqnotify.kernel.execution.ExecutionContext;
import org.lqnotify.kernel.execution.ExecutionManager;
import org.lqnotify.pub.DomainProfile;
import org.lqnotify.pub.route.RouteCatalog;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

public class RouteCatalogResourceV1 {

  private final DomainKernel domainKernel;
  private final ExecutionManager executionManager;

  public RouteCatalogResourceV1(ExecutionManager executionManager, DomainKernel domainKernel) {
    this.domainKernel = domainKernel;
    this.executionManager = executionManager;
  }

  private DomainProfile getDomainProfile() {
    ExecutionContext ec = executionManager.context();
    return domainKernel.findByApiKey(ec.getApiKey());
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public RouteCatalog getRouteCatalog() {
    return getDomainProfile().getRouteCatalog();
  }

  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  public RouteCatalog putRouteCatalog(RouteCatalog routeCatalog) {
    // TODO - we need to dump the cache and force a reload
    DomainProfile returnProfile = domainKernel.updateRouteCatalog(getDomainProfile(), routeCatalog);
    return returnProfile.getRouteCatalog();
  }
}
