package org.cstamas.vertx.orientdb;

import java.util.Objects;

import com.orientechnologies.common.concur.ONeedRetryException;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

/**
 * OrientDB utility handlers.
 */
public final class OrientUtils
{
  private OrientUtils() {
    // nop
  }

  /**
   * Wraps passed in Orient document database into Orient graph database.
   */
  public static Handler<AsyncResult<ODatabaseDocumentTx>> graph(final Handler<AsyncResult<OrientGraph>> handler) {
    Objects.requireNonNull(handler);
    return adb -> {
      if (adb.succeeded()) {
        OrientGraph graphDatabase = new OrientGraph(adb.result());
        handler.handle(Future.succeededFuture(graphDatabase));
      }
      else {
        handler.handle(Future.failedFuture(adb.cause()));
      }
    };
  }

  /**
   * Wraps passed in handler into a transaction.
   */
  public static Handler<AsyncResult<ODatabaseDocumentTx>> tx(final Handler<AsyncResult<ODatabaseDocumentTx>> handler) {
    Objects.requireNonNull(handler);
    return adb -> {
      if (adb.succeeded()) {
        ODatabaseDocumentTx db = adb.result();
        try {
          db.begin();
          handler.handle(adb);
          db.commit();
        }
        catch (Exception e) {
          db.rollback();
          handler.handle(Future.failedFuture(e));
        }
      }
      else {
        handler.handle(adb);
      }
    };
  }

  /**
   * Retries {@link Handler} multiple times, usable with OrientDB MVCC and {@link ONeedRetryException}.
   *
   * @see <a href="http://orientdb.com/docs/2.2/Java-Multi-Threading.html#multi-version-concurrency-control">MVCC</a>
   */
  public static Handler<AsyncResult<ODatabaseDocumentTx>> retry(final int retries,
                                                                final Handler<AsyncResult<ODatabaseDocumentTx>> handler)
  {
    if (retries < 1) {
      throw new IllegalArgumentException("Retries must be greater than zero: " + retries);
    }
    Objects.requireNonNull(handler);
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
              break;
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
}
