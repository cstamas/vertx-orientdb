package org.cstamas.vertx.orientdb.impl;

import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import org.cstamas.vertx.orientdb.GraphDatabase;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation.
 */
public class GraphDatabaseImpl
    implements GraphDatabase
{
  private final String name;

  private final ManagerImpl manager;

  public GraphDatabaseImpl(final String name, final ManagerImpl manager) {
    this.name = checkNotNull(name);
    this.manager = checkNotNull(manager);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public GraphDatabase exec(final Handler<AsyncResult<OrientGraph>> handler) {
    manager.exec(getName(), adb -> {
      if (adb.succeeded()) {
        OrientGraph graph = new OrientGraph(adb.result());
        try {
          handler.handle(Future.succeededFuture());
        }
        finally {
          graph.shutdown();
        }
      }
      else {
        handler.handle(Future.failedFuture(adb.cause()));
      }
    });
    return this;
  }

  @Override
  public void close(final Handler<AsyncResult<Void>> completionHandler) {
    manager.close(getName(), completionHandler);
  }
}
