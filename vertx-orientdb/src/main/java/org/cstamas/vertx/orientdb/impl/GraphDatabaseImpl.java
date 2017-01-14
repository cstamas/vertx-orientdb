package org.cstamas.vertx.orientdb.impl;

import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.cstamas.vertx.orientdb.GraphDatabase;

/**
 * Default implementation.
 */
public class GraphDatabaseImpl
    extends DatabaseSupport<GraphDatabase, OrientGraph>
    implements GraphDatabase
{
  public GraphDatabaseImpl(final Vertx vertx, final String name, final ManagerImpl manager) {
    super(vertx, name, manager);
  }

  @Override
  public GraphDatabase exec(final Handler<AsyncResult<OrientGraph>> handler) {
    manager.exec(getName(), adb -> {
      if (adb.succeeded()) {
        OrientGraph graph = new OrientGraph(adb.result());
        try {
          handler.handle(Future.succeededFuture(graph));
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
}
