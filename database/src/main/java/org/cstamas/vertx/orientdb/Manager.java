package org.cstamas.vertx.orientdb;

import javax.annotation.Nullable;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import io.vertx.core.AsyncResult;
import io.vertx.core.Closeable;
import io.vertx.core.Handler;

/**
 * Embedded OrientDB orientdb instance manager.
 */
public interface Manager
    extends Closeable
{
  /**
   * Opens the orientdb manager.
   *
   * @param handler the handler invoked when orientdb opened.
   */
  void open(Handler<AsyncResult<Manager>> handler);

  /**
   * Opens or creates a named {@link Database} with given {@code name} and returns it. Before returning, the
   * passed in {@code openHandler} is invoked if non-{@code null} to perform upgrade/initialization if needed.
   *
   * @param name            the orientdb instance name.
   * @param openHandler     the handler to invoke in single-connection mode, useful to set up schema, upgrade schema or
   *                        so,if needed, may be {@code null}.
   * @param instanceHandler the handler invoked when instance is constructed.
   */
  Manager instance(String name,
                   @Nullable Handler<ODatabaseDocumentTx> openHandler,
                   Handler<AsyncResult<Database>> instanceHandler);
}
