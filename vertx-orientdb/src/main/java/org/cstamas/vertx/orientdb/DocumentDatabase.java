package org.cstamas.vertx.orientdb;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import io.vertx.core.AsyncResult;
import io.vertx.core.Closeable;
import io.vertx.core.Handler;

/**
 * OrientDB pooled document database instance.
 */
public interface DocumentDatabase
    extends Closeable
{
  /**
   * Returns the instance name.
   */
  String getName();

  /**
   * Executes handler with pooled {@link ODatabaseDocumentTx} connection.
   */
  DocumentDatabase exec(Handler<AsyncResult<ODatabaseDocumentTx>> handler);
}
