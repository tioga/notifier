package org.lqnotify.notifier;

import org.lqnotify.notifier.builder.LqBuilder;
import org.lqnotify.notifier.builder.LqBuilderCallback;
import org.lqnotify.notifier.builder.LqBuilderCallbacks;
import org.lqnotify.notifier.sender.LqSender;

/**
 * User: Harlan
 * Date: 1/26/2015
 * Time: 11:13 PM
 */
public class LqNotifier {
  private final LqSender sender;
  private final LqBuilderCallbacks builderCallbacks;

  public LqNotifier(LqSender sender) {
    this.sender = sender;
    builderCallbacks = new LqBuilderCallbacks();
  }

  public LqBuilder begin() {
    return new LqBuilder(sender, builderCallbacks);
  }

  public LqNotifier onBegin(LqBuilderCallback callback) {
    builderCallbacks.onBegin(callback);
    return this;
  }

  public LqNotifier onBeforeSend(LqBuilderCallback callback) {
    builderCallbacks.onBeforeSend(callback);
    return this;
  }


}
