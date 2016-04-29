package org.cstamas.vertx.orientdb;

import java.io.IOException;

import com.google.common.base.Throwables;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
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
  public void init(final Vertx vertx, final Context context) {
    super.init(vertx, context);
    try {
      this.manager = new ManagerImpl(vertx, Configuration.fromJsonObject(context.config()));
    }
    catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public void start(final Future<Void> startFuture) throws Exception {
    manager.open(v -> {
      if (v.succeeded()) {
        startFuture.complete();
      }
      else {
        startFuture.fail(v.cause());
      }
    });
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
