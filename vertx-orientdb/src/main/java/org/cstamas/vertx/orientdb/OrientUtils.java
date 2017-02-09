package org.cstamas.vertx.orientdb;

import java.util.Objects;

import javax.annotation.Nullable;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
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
   * Wraps passed in {@link OrientGraph} handler into {@link ODatabaseDocumentTx} handler.
   */
  public static Handler<AsyncResult<ODatabaseDocumentTx>> graph(final Handler<AsyncResult<OrientGraph>> handler) {
    Objects.requireNonNull(handler);
    return adb -> {
      if (adb.failed()) {
        handler.handle(Future.failedFuture(adb.cause()));
      }
      else {
        OrientGraph gd = new OrientGraph(adb.result());
        try {
          handler.handle(Future.succeededFuture(gd));
        }
        finally {
          gd.shutdown();
        }
      }
    };
  }

  /**
   * Wraps passed in handler into a transaction.
   */
  public static Handler<ODatabaseDocumentTx> tx(final Handler<ODatabaseDocumentTx> handler)
      throws Exception
  {
    Objects.requireNonNull(handler);
    return db -> {
      try {
        db.begin();
        handler.handle(db);
        db.commit();
      }
      catch (Exception e) {
        db.rollback();
        throw e;
      }
    };
  }

  /**
   * Wraps passed in handler into a transaction.
   */
  public static Handler<OrientGraph> graphTx(final Handler<OrientGraph> handler)
      throws Exception
  {
    Objects.requireNonNull(handler);
    return db -> {
      db.setAutoStartTx(false);
      try {
        db.begin();
        handler.handle(db);
        db.commit();
      }
      catch (Exception e) {
        db.rollback();
        throw e;
      }
    };
  }

  /**
   * Helper for method {@link #retry(int, Variance, Handler)} and {@link #retryGraph(int, Variance, Handler)}. On
   * retries, is invoked and may for example sleep few millis to improve conflict resolution on simultaneous updates.
   */
  public interface Variance
  {
    void vary(int retry, OConcurrentModificationException e);
  }

  /**
   * Retries {@link Handler} multiple times, usable with OrientDB MVCC and {@link OConcurrentModificationException}. In
   * case of any exception (and if retries were exceeded), this handler will throw the original exception wrapped into
   * {@link RuntimeException}. The passed in handler <string>must commit</string> to achieve the goal, otherwise the
   * {@link OConcurrentModificationException} will never be raised. The {@link #retry(int, Variance, Handler)} wrapped
   * handler must be guarded for exceptions in surrounding block, hence it declares it throws {@link Exception}.
   *
   * @see <a href="http://orientdb.com/docs/2.2/Java-Multi-Threading.html#multi-version-concurrency-control">MVCC</a>
   */
  public static Handler<ODatabaseDocumentTx> retry(final int retries,
                                                   @Nullable final Variance variance,
                                                   final Handler<ODatabaseDocumentTx> handler) throws Exception
  {
    if (retries < 1) {
      throw new IllegalArgumentException("Retries must be greater than zero: " + retries);
    }
    Objects.requireNonNull(handler);
    return adb -> {
      int retry = 0;
      Exception throwable = null;
      try {
        while (retry < retries) {
          try {
            retry++;
            handler.handle(adb);
            throwable = null;
            break;
          }
          catch (OConcurrentModificationException e) {
            // kick it out of cache and try again
            throwable = e;
            adb.getLocalCache().deleteRecord(e.getRid());
            if (variance != null) {
              variance.vary(retry, e);
            }
          }
        }
      }
      catch (Exception e) {
        throwable = e;
      }
      if (throwable != null) {
        throw new RuntimeException("Failed after " + retries + " retries", throwable);
      }
    };
  }

  /**
   * Retries {@link Handler} multiple times, usable with OrientDB MVCC and {@link OConcurrentModificationException}. In
   * case of any exception (and if retries were exceeded), this handler will throw the original exception wrapped into
   * {@link RuntimeException}. The passed in handler <string>must commit</string> to achieve the goal, otherwise the
   * {@link OConcurrentModificationException} will never be raised. The {@link #retryGraph(int, Variance, Handler)}
   * wrapped handler must be guarded for exceptions in surrounding block, hence it declares it throws {@link Exception}.
   *
   * @see <a href="http://orientdb.com/docs/2.2/Java-Multi-Threading.html#multi-version-concurrency-control">MVCC</a>
   */
  public static Handler<OrientGraph> retryGraph(final int retries,
                                                @Nullable final Variance variance,
                                                final Handler<OrientGraph> handler) throws Exception
  {
    if (retries < 1) {
      throw new IllegalArgumentException("Retries must be greater than zero: " + retries);
    }
    Objects.requireNonNull(handler);
    return adb -> {
      int retry = 0;
      Exception throwable = null;
      try {
        while (retry < retries) {
          try {
            retry++;
            handler.handle(adb);
            throwable = null;
            break;
          }
          catch (OConcurrentModificationException e) {
            // kick it out of cache and try again
            throwable = e;
            adb.getRawGraph().getLocalCache().deleteRecord(e.getRid());
            if (variance != null) {
              variance.vary(retry, e);
            }
          }
        }
      }
      catch (Exception e) {
        throwable = e;
      }
      if (throwable != null) {
        throw new RuntimeException("Failed after " + retries + " retries", throwable);
      }
    };
  }
}
