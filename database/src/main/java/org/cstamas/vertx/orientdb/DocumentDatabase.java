package org.cstamas.vertx.orientdb;

import java.util.Map;

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
   * Result handler for Orient non-blocking query.
   *
   * @see <a href="http://orientdb.com/docs/2.1/Document-Database.html#non-blocking-query-since-v2-1">Non-Blocking query
   * (since v2.1)</a>
   */
  interface ResultHandler<T>
  {
    boolean handle(T rec);

    void failure(Throwable t);

    void end();
  }

  /**
   * Returns the instance name.
   */
  String getName();

  /**
   * Executes handler with pooled {@link ODatabaseDocumentTx} connection.
   */
  DocumentDatabase exec(Handler<AsyncResult<ODatabaseDocumentTx>> handler);

  /**
   * Executes Orient async query.
   */
  <T> DocumentDatabase select(ResultHandler<T> handler, String sql, Map<String, Object> params);
}
