package org.cstamas.vertx.orientdb;

import java.util.Map;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;

/**
 * OrientDB pooled document database instance.
 */
public interface DocumentDatabase
    extends Database<DocumentDatabase, ODatabaseDocumentTx>
{
  /**
   * Executes handler with pooled {@link ODatabaseDocumentTx} connection.
   */
  DocumentDatabase exec(Handler<AsyncResult<ODatabaseDocumentTx>> handler);

  /**
   * Executes a SELECT SQL command and passes it's result as {@link ReadStream} to handler.
   */
  <T> DocumentDatabase stream(String selectSql,
                              Map<String, Object> params,
                              Handler<AsyncResult<ReadStream<T>>> handler);
}
