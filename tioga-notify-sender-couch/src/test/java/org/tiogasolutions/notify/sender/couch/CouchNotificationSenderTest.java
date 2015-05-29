package org.tiogasolutions.notify.sender.couch;

import org.tiogasolutions.couchace.core.api.CouchDatabase;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tiogasolutions.notify.kernel.request.NotificationRequestEntity;
import org.tiogasolutions.notify.kernel.request.NotificationRequestStore;
import org.tiogasolutions.notify.kernel.test.TestFactory;
import org.tiogasolutions.notify.notifier.Notifier;
import org.tiogasolutions.notify.notifier.request.NotificationRequest;
import org.tiogasolutions.notify.notifier.request.NotificationResponse;
import org.tiogasolutions.notify.notifier.request.NotificationResponseType;
import org.tiogasolutions.notify.pub.domain.DomainProfile;
import org.tiogasolutions.notify.pub.request.NotificationRequestStatus;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.testng.Assert.*;

/**
 * User: Harlan
 * Date: 1/28/2015
 * Time: 10:30 PM
 */
@Test
public class CouchNotificationSenderTest extends org.tiogasolutions.notify.kernel.KernelAbstractTest {

  @Inject
  private org.tiogasolutions.notify.kernel.domain.DomainKernel domainKernel;

  private static int lastTrackingId = 4400;

  private static String nextTrackingId() {
    return String.valueOf(lastTrackingId++);
  }

  private Notifier notifier;
  private NotificationRequestStore requestStore;

  @BeforeClass
  public void setup() {
    DomainProfile domainProfile = domainKernel.findByApiKey(TestFactory.API_KEY);
    CouchDatabase requestDb = domainKernel.requestDb(domainProfile);
    requestStore = new NotificationRequestStore(requestDb);

    CouchNotificationSenderSetup couchSenderSetup = new CouchNotificationSenderSetup(
      requestDb.getHttpClient().getBaseUrl(),
      requestDb.getDatabaseName(),
      domainProfile.getApiKey(),
      domainProfile.getApiPassword()
    );

    CouchNotificationSender sender = new CouchNotificationSender(couchSenderSetup);
    sender.onFailure(f -> fail("Failure in sending request: " + f.getThrowable().getMessage()));
    sender.onFailure(f -> fail("Failure in sending attachment: " + f.getThrowable().getMessage()));

    notifier = new Notifier(sender);
    notifier.onBegin(b -> b.topic("test topic").trackingId(nextTrackingId()));
  }

  public void requestEntityLifeCycle() throws Exception {

    // Send a notification
    Future<NotificationResponse> responseFuture = notifier.begin()
      .summary("Test message")
      .trait("key1", "value1")
      .link("example", "http://example.com")
      .link("Tioga YouTrack", "http://tioga.myjetbrains.com/")
      .exception(new Throwable("Some kind of trouble"))
      .attach("attachOne", MediaType.TEXT_PLAIN, "this is attachment one")
      .attach("attachTwo", MediaType.TEXT_PLAIN, "this is attachment two")
      .send();

    NotificationResponse response = responseFuture.get();
    assertEquals(response.getResponseType(), NotificationResponseType.SUCCESS);
    assertNotificationCreated(response.getRequest());

  }

  private void assertNotificationCreated(NotificationRequest notificationRequest) {

    // Retrieve the NotificationRequestEntity and verify.
    assertNotNull(notificationRequest.getTrackingId());
    NotificationRequestEntity notificationRequestEntity = requestStore.findByTrackingId(notificationRequest.getTrackingId());
    Assert.assertEquals(notificationRequestEntity.getTopic(), notificationRequest.getTopic());
    assertTrue(notificationRequestEntity.getCreatedAt().isEqual(notificationRequest.getCreatedAt()));
    Assert.assertEquals(notificationRequestEntity.getSummary(), notificationRequest.getSummary());
    Assert.assertEquals(notificationRequestEntity.getTrackingId(), notificationRequest.getTrackingId());
    Assert.assertEquals(notificationRequestEntity.getRequestStatus(), NotificationRequestStatus.READY);

    Assert.assertEquals(notificationRequestEntity.getLinks().size(), 2);
    assertTrue(notificationRequestEntity.getLinks().stream().anyMatch(l -> l.getName().equals("example") && l.getHref().equals("http://example.com")));
    assertTrue(notificationRequestEntity.getLinks().stream().anyMatch(l -> l.getName().equals("google") && l.getHref().equals("http://google.com")));

    Assert.assertEquals(notificationRequestEntity.listAttachmentInfo().size(), 2);
    assertTrue(notificationRequestEntity.listAttachmentInfo().stream().anyMatch(a -> a.getName().equals("attachOne")));
    assertTrue(notificationRequestEntity.listAttachmentInfo().stream().anyMatch(a -> a.getName().equals("attachTwo")));

    // Retrieve by requestId
    notificationRequestEntity = requestStore.findByRequestId(notificationRequestEntity.getRequestId());
    Assert.assertEquals(notificationRequestEntity.getTopic(), notificationRequest.getTopic());
    assertTrue(notificationRequestEntity.getCreatedAt().isEqual(notificationRequest.getCreatedAt()));
    Assert.assertEquals(notificationRequestEntity.getSummary(), notificationRequest.getSummary());
    Assert.assertEquals(notificationRequestEntity.getTrackingId(), notificationRequest.getTrackingId());
    Assert.assertEquals(notificationRequestEntity.getRequestStatus(), NotificationRequestStatus.READY);

    // Check attachments
    notificationRequestEntity = requestStore.findByRequestId(notificationRequestEntity.getRequestId());
    Assert.assertEquals(notificationRequestEntity.getTopic(), notificationRequest.getTopic());
    assertTrue(notificationRequestEntity.getCreatedAt().isEqual(notificationRequest.getCreatedAt()));
    Assert.assertEquals(notificationRequestEntity.getSummary(), notificationRequest.getSummary());
    Assert.assertEquals(notificationRequestEntity.getTrackingId(), notificationRequest.getTrackingId());
    Assert.assertEquals(notificationRequestEntity.getRequestStatus(), NotificationRequestStatus.READY);

    // Check links
    assertTrue(notificationRequestEntity.getLinks().stream().anyMatch(l -> l.getName().equals("example") && l.getHref().equals("http://example.com")));
    assertTrue(notificationRequestEntity.getLinks().stream().anyMatch(l -> l.getName().equals("google") && l.getHref().equals("http://google.com")));

    // Mark processing.
    notificationRequestEntity.processing();
    notificationRequestEntity = requestStore.saveAndReload(notificationRequestEntity);
    Assert.assertEquals(notificationRequestEntity.getRequestStatus(), NotificationRequestStatus.PROCESSING);

    // Mark Completed.
    notificationRequestEntity.completed();
    notificationRequestEntity = requestStore.saveAndReload(notificationRequestEntity);
    Assert.assertEquals(notificationRequestEntity.getRequestStatus(), NotificationRequestStatus.COMPLETED);

    // Check attachments
    notificationRequestEntity = requestStore.findByRequestId(notificationRequestEntity.getRequestId());
    Assert.assertEquals(notificationRequestEntity.getTopic(), notificationRequest.getTopic());
    assertTrue(notificationRequestEntity.getCreatedAt().isEqual(notificationRequest.getCreatedAt()));
    Assert.assertEquals(notificationRequestEntity.getSummary(), notificationRequest.getSummary());
    Assert.assertEquals(notificationRequestEntity.getTrackingId(), notificationRequest.getTrackingId());

  }


  @Test(dependsOnMethods = "requestEntityLifeCycle")
  public void processingQueries() throws ExecutionException, InterruptedException {
    // Send two notifications
    Future<NotificationResponse> responseFuture = notifier.begin()
        .summary("Test message")
        .trait("key1", "value1")
        .link("example", "http://example.com")
        .link("google", "http://google.com")
        .exception(new Throwable("Some kind of trouble"))
        .attach("attachOne", MediaType.TEXT_PLAIN, "this is attachment one")
        .attach("attachTwo", MediaType.TEXT_PLAIN, "this is attachment two")
        .send();
    NotificationResponse response = responseFuture.get();
    NotificationRequest request1 = response.getRequest();
    assertEquals(response.getResponseType(), NotificationResponseType.SUCCESS);
    responseFuture = notifier.begin()
        .summary("Another Test message")
        .trait("key1", "value1")
        .link("example", "http://example.com")
        .link("google", "http://google.com")
        .exception(new Throwable("Some kind of trouble"))
        .attach("attachOne", MediaType.TEXT_PLAIN, "this is another attachment one")
        .attach("attachTwo", MediaType.TEXT_PLAIN, "this is another attachment two")
        .send();
    response = responseFuture.get();
    assertEquals(response.getResponseType(), NotificationResponseType.SUCCESS);
    NotificationRequest request2 = response.getRequest();
    assertEquals(response.getResponseType(), NotificationResponseType.SUCCESS);

    // Query for ready, should only find two.
    List<NotificationRequestEntity> readyRequests = requestStore.findByStatus(NotificationRequestStatus.READY);
    assertEquals(readyRequests.size(), 2);
    assertTrue(readyRequests.stream().anyMatch(r -> r.getTrackingId().equals(request1.getTrackingId())));
    assertTrue(readyRequests.stream().anyMatch(r -> r.getTrackingId().equals(request2.getTrackingId())));

    // Mark one as processing.
    NotificationRequestEntity entity = readyRequests.get(0);
    entity.processing();
    requestStore.save(entity);

    // Query for ready, should only find one.
    readyRequests = requestStore.findByStatus(NotificationRequestStatus.READY);
    assertEquals(readyRequests.size(), 1);
    assertTrue(readyRequests.stream().anyMatch(r -> r.getTrackingId().equals(request2.getTrackingId())));
  }
}
