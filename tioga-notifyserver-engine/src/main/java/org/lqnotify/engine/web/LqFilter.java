package org.lqnotify.engine.web;

import org.tiogasolutions.dev.common.EqualsUtils;
import org.crazyyak.dev.common.exceptions.ApiNotFoundException;
import org.lqnotify.kernel.admin.AdminKernel;
import org.lqnotify.kernel.domain.DomainKernel;
import org.lqnotify.kernel.execution.ExecutionManager;
import org.lqnotify.pub.DomainProfile;
import org.springframework.util.StringUtils;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.*;
import javax.ws.rs.core.*;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Map;

import static org.tiogasolutions.dev.common.exceptions.ExceptionUtils.*;

/**
 * This is the "global" filter for the Engine. It's primary responsibility is
 * for managing the execution context over the entire lifecycle of a request.
 *
 * All other filters should be processed after this one.
 */
@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class LqFilter implements ContainerRequestFilter, ContainerResponseFilter {

  @Context
  private UriInfo uriInfo;
  @Context
  private HttpHeaders headers;
  @Context
  Application application;

  @Inject // Injected by CDI, not Spring
  @SuppressWarnings("SpringJavaAutowiringInspection")
  private AdminKernel adminKernel;

  @Inject // Injected by CDI, not Spring
  @SuppressWarnings("SpringJavaAutowiringInspection")
  private ExecutionManager executionManager;

  @Inject // Injected by CDI, not Spring
  @SuppressWarnings("SpringJavaAutowiringInspection")
  private DomainKernel domainKernel;

  public LqFilter() {
  }

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    String baseUri = uriInfo.getBaseUri().toString();
    String requestUri = uriInfo.getRequestUri().toString();

    String path = requestUri.substring(baseUri.length()-1);

    // CRITICAL - why are these values coming from the properties, and this will be done on every request - HN
    // TODO - not sure on default values either.
    Map<String,Object> properties = application.getProperties();
    String clientContext = assertNotZeroLength((String) properties.get("app.client.context"), "app.client.context");
    String adminContext = assertNotZeroLength((String) properties.get("app.admin.context"), "app.admin.context");

    if (path.equals(clientContext) || path.startsWith(clientContext+"/")) {
      authenticateClientRequest(requestContext);
    } else if (path.equals(adminContext) || path.startsWith(adminContext+"/")) {
      authenticateAdminRequest(requestContext);
    } else if (path.startsWith("/app")) {
      authenticateAdminRequest(requestContext);
    }
  }

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
    executionManager.clearContext();

  }

  private void authenticateClientRequest(ContainerRequestContext requestContext) {
    String authHeader = requestContext.getHeaderString("Authorization");

    if (authHeader == null || authHeader.startsWith("Basic ") == false) {
      throw new NotAuthorizedException("Notify");
    } else {
      authHeader = authHeader.substring(6);
    }

    byte[] bytes = DatatypeConverter.parseBase64Binary(authHeader);
    String basicAuth = new String(bytes, StandardCharsets.UTF_8);

    int pos = basicAuth.indexOf(":");

    String apiKey;
    String apiPassword;

    if (pos < 0) {
      throw new NotAuthorizedException("API");
    }

    apiKey = basicAuth.substring(0, pos);
    apiPassword = basicAuth.substring(pos+1);

    if (StringUtils.isEmpty(apiKey) || StringUtils.isEmpty(apiPassword)) {
      throw new NotAuthorizedException("API");
    }

    DomainProfile domainProfile;

    if (apiKey.equals("system") && apiPassword.equals("changeme")) {
      // HACK/TODO - remove hard coded password.
      domainProfile = domainKernel.getSystemDomain();

    } else {

      try {
        domainProfile = domainKernel.findByApiKey(apiKey);
      } catch (ApiNotFoundException e) {
        throw new NotAuthorizedException("API");
      }

      if (EqualsUtils.objectsNotEqual(apiPassword, domainProfile.getApiPassword())) {
        throw new NotAuthorizedException("API");
        // Specific message allows for probing of accounts.
        // throw new NotAuthorizedException("Invalid password");
      }
    }

    if (EqualsUtils.objectsNotEqual(apiPassword, domainProfile.getApiPassword())) {
      throw new NotAuthorizedException("API");
      // Specific message allows for probing of accounts.
      // throw new NotAuthorizedException("Invalid password");
    }

    final SecurityContext securityContext = requestContext.getSecurityContext();
    requestContext.setSecurityContext(new ClientSecurityContext(securityContext, domainProfile));

    executionManager.newApiContext(domainProfile, uriInfo, headers);
  }

  private void authenticateAdminRequest(ContainerRequestContext requestContext) {
    String authHeader = requestContext.getHeaderString("Authorization");

    if (authHeader == null) {
      throw new NotAuthorizedException("API");
    } else if (authHeader.startsWith("Basic ") == false) {
      throw new NotAuthorizedException("API");
    } else {
      authHeader = authHeader.substring(6);
    }

    byte[] bytes = DatatypeConverter.parseBase64Binary(authHeader);
    String basicAuth = new String(bytes, StandardCharsets.UTF_8);

    int pos = basicAuth.indexOf(":");

    String username;
    String password;

    if (pos < 0) {
      username = basicAuth;
      password = null;

    } else {
      username = basicAuth.substring(0, pos);
      password = basicAuth.substring(pos+1);
    }

    // throws NotAuthorizedException if not a valid username and password
    adminKernel.authorize(username, password);

    final SecurityContext securityContext = requestContext.getSecurityContext();
    requestContext.setSecurityContext(new AdminSecurityContext(securityContext, username));
  }

  private class AdminSecurityContext implements SecurityContext {
    private final boolean secure;
    private final String username;
    public AdminSecurityContext(SecurityContext securityContext, String username) {
      this.username = username;
      this.secure = securityContext.isSecure();
    }
    public String getUsername() { return username; }
    @Override public boolean isUserInRole(String role) { return false; }
    @Override public boolean isSecure() { return secure; }
    @Override public String getAuthenticationScheme() { return "BASIC_AUTH"; }
    @Override public Principal getUserPrincipal() { return this::getUsername; }
  }

  private class ClientSecurityContext implements SecurityContext {
    private final boolean secure;
    private final String domainName;
    private final Principal principal;
    public ClientSecurityContext(SecurityContext securityContext, DomainProfile domain) {
      this.secure = securityContext.isSecure();
      this.domainName = domain.getDomainName();
      this.principal = this::getDomainName;
    }
    public String getDomainName() {
      return domainName;
    }
    @Override public boolean isUserInRole(String role) {
      return false;
    }
    @Override public boolean isSecure() {
      return secure;
    }
    @Override public String getAuthenticationScheme() {
      return "BASIC_AUTH";
    }
    @Override public Principal getUserPrincipal() { return principal;}
  }
}
