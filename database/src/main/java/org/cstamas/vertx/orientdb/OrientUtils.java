package org.cstamas.vertx.orientdb;

import java.util.Map;

import com.orientechnologies.common.concur.ONeedRetryException;
import com.orientechnologies.orient.core.command.OCommandResultListener;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLNonBlockingQuery;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

import static com.google.common.base.Preconditions.checkArgument;
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
   * Wraps passed in handler into a transaction.
   */
  public static <T> Handler<AsyncResult<ODatabaseDocumentTx>> tx(final Handler<AsyncResult<ODatabaseDocumentTx>> handler) {
    checkNotNull(handler);
    return adb -> {
      if (adb.succeeded()) {
        ODatabaseDocumentTx db = adb.result();
        db.begin();
        handler.handle(adb);
        db.commit();
      }
      else {
        handler.handle(adb);
      }
    };
  }

  /**
   * Retries {@link Handler} multiple times, usable with OrientDB MVCC and {@link ONeedRetryException}.
   */
  public static <T> Handler<AsyncResult<ODatabaseDocumentTx>> retry(final int retries,
                                                                    final Handler<AsyncResult<ODatabaseDocumentTx>> handler)
  {
    checkArgument(retries > 0);
    checkNotNull(handler);
    return adb -> {
      if (adb.succeeded()) {
        int retry = 0;
        Throwable throwable = null;
        try {
          while (retry < retries) {
            try {
              retry++;
              handler.handle(adb);
              throwable = null;
            }
            catch (ONeedRetryException e) {
              // try again
              throwable = e;
            }
          }
        }
        catch (Exception e) {
          throwable = e;
        }
        if (throwable != null) {
          handler.handle(Future.failedFuture(throwable));
        }
      }
      else {
        handler.handle(adb);
      }
    };
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
