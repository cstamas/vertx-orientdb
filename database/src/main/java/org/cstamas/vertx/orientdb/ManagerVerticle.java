package org.cstamas.vertx.orientdb;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import org.cstamas.vertx.orientdb.impl.ManagerImpl;

/**
 * OrientDB verticle that manages purely the lifecycle of the orientdb.
 */
public class ManagerVerticle
    extends AbstractVerticle
{
  private Manager manager;

  public Manager getManager() {
    return manager;
  }

  @Override
  public void start(final Future<Void> startFuture) throws Exception {
    this.manager = new ManagerImpl(vertx, ManagerOptions.fromJsonObject(config()));
    startFuture.complete();
  }

  @Override
  public void stop(final Future<Void> stopFuture) throws Exception {
    manager.close(v -> {
      if (v.succeeded()) {
        stopFuture.complete();
      }
      else {
        stopFuture.fail(v.cause());
      }
    });
  }
}
