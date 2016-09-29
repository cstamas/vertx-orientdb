package org.cstamas.vertx.orientdb;

import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * OrientDB pooled graph database instance.
 */
public interface GraphDatabase
    extends Database<GraphDatabase, OrientGraph>
{
  /**
   * Executes handler with pooled {@link OrientGraph} connection.
   */
  GraphDatabase exec(Handler<AsyncResult<OrientGraph>> handler);
}
