package org.cstamas.vertx.orientdb;

import java.util.Map;

import com.orientechnologies.orient.core.command.OCommandResultListener;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLNonBlockingQuery;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * OrientDB pooled document database instance.
 */
public class OrientUtils
{
  private OrientUtils() {
    // nop
  }

  /**
   * Result handler for Orient non-blocking query.
   *
   * @see <a href="http://orientdb.com/docs/2.1/Document-Database.html#non-blocking-query-since-v2-1">Non-Blocking query
   * (since v2.1)</a>
   */
  public interface ResultHandler<T>
  {
    boolean handle(T rec);

    void failure(Throwable t);

    void end();
  }

  /**
   * Handler that uses OrientDB {@link OSQLNonBlockingQuery} to execute SELECT query, to be used with {@link
   * DocumentDatabase#exec(Handler)} method.
   */
  public static <T> Handler<AsyncResult<ODatabaseDocumentTx>> nonBlockingQuery(final ResultHandler<T> handler,
                                                                               final String sql,
                                                                               final Map<String, Object> params)
  {
    checkNotNull(handler);
    checkNotNull(sql);
    return adb -> {
      if (adb.succeeded()) {
        OSQLNonBlockingQuery<ODocument> query = new OSQLNonBlockingQuery<>(
            sql,
            new OCommandResultListener()
            {
              @Override
              public boolean result(final Object iRecord) {
                final T doc = (T) iRecord;
                return handler.handle(doc);
              }

              @Override
              public void end() {
                handler.end();
              }
            }
        );
        adb.result().command(query).execute(params);
      }
      else {
        handler.failure(adb.cause());
      }
    };
  }
}
