package org.cstamas.vertx.orientdb;

import javax.annotation.Nullable;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import io.vertx.core.AsyncResult;
import io.vertx.core.Closeable;
import io.vertx.core.Handler;

/**
 * Embedded OrientDB instance manager.
 */
public interface Manager
    extends Closeable
{
  /**
   * Opens the database manager, this may be lengthy operation (ie. Orient repairing itself on a large database).
   *
   * @param handler the handler invoked when open completed and manager is ready for use.
   */
  void open(Handler<AsyncResult<Manager>> handler);

  /**
   * Opens an existing or creates a new named {@link Database} with given {@code name}. Before creating pool, the
   * passed in {@code openHandler} is invoked if not {@code null} to perform possible maintenance, like schema
   * upgrade/initialization, etc if needed.
   *
   * @param name            the orientdb instance name.
   * @param openHandler     the handler to invoke in single-connection mode, useful to set up schema, upgrade schema or
   *                        so, if needed, may be {@code null}.
   * @param instanceHandler the handler invoked when instance is constructed.
   */
  Manager instance(String name,
                   @Nullable Handler<ODatabaseDocumentTx> openHandler,
                   Handler<AsyncResult<Database>> instanceHandler);
}
