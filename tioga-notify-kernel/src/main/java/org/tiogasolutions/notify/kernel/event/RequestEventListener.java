package org.tiogasolutions.notify.kernel.event;

import org.tiogasolutions.notify.kernel.request.NotificationRequestEntity;

public interface RequestEventListener {

    void requestCreated(String domainName, NotificationRequestEntity request);

}
