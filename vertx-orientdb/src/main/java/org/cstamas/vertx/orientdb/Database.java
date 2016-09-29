package org.cstamas.vertx.orientdb;

import io.vertx.core.AsyncResult;
import io.vertx.core.Closeable;
import io.vertx.core.Handler;

/**
 * Database.
 */
public interface Database<T, OT>
    extends Closeable
{
  /**
   * Returns the instance name.
   */
  String getName();

  /**
   * Executes handler with pooled connection.
   */
  T exec(Handler<AsyncResult<OT>> handler);
}
