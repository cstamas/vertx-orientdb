package org.cstamas.vertx.orientdb.impl;

import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import org.cstamas.vertx.orientdb.GraphDatabase;

import static org.cstamas.vertx.orientdb.OrientUtils.graph;

/**
 * Default implementation.
 */
public class GraphDatabaseImpl
    extends DatabaseSupport<GraphDatabase, OrientGraph>
    implements GraphDatabase
{
  public GraphDatabaseImpl(final String name, final ManagerImpl manager) {
    super(name, manager);
  }

  @Override
  public GraphDatabase exec(final Handler<AsyncResult<OrientGraph>> handler) {
    manager.exec(getName(), adb -> graph(handler).handle(adb));
    return this;
  }
}
