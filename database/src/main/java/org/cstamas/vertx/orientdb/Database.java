package org.cstamas.vertx.orientdb;

import java.util.Map;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.core.AsyncResult;
import io.vertx.core.Closeable;
import io.vertx.core.Handler;

/**
 * OrientDB orientdb instance.
 */
//@ProxyGen
public interface Database
    extends Closeable
{
  interface ResultHandler<T>
      extends Handler<T>
  {
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
  @Fluent
  Database exec(Handler<AsyncResult<ODatabaseDocumentTx>> handler);

  /**
   * Executes Orient async query.
   */
  @Fluent
  <T> Database select(ResultHandler<T> handler, String sql, Map<String, Object> params);
}
