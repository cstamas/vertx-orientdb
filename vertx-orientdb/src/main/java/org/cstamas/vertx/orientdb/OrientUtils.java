package org.cstamas.vertx.orientdb;

import com.orientechnologies.common.concur.ONeedRetryException;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * OrientDB utility handlers.
 */
public final class OrientUtils
{
  private OrientUtils() {
    // nop
  }

  /**
   * Wraps passed in handler into a transaction.
   */
  public static Handler<AsyncResult<ODatabaseDocumentTx>> tx(final Handler<AsyncResult<ODatabaseDocumentTx>> handler) {
    checkNotNull(handler);
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
          throw e;
        }
      }
      else {
        handler.handle(adb);
      }
    };
  }

  /**
   * Retries {@link Handler} multiple times, usable with OrientDB MVCC and {@link ONeedRetryException}.
   */
  public static Handler<AsyncResult<ODatabaseDocumentTx>> retry(final int retries,
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
}
