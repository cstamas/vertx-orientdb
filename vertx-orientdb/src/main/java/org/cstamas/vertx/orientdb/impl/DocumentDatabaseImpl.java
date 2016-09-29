package org.cstamas.vertx.orientdb.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;

import com.orientechnologies.orient.core.command.OCommandResultListener;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLNonBlockingQuery;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.streams.ReadStream;
import org.cstamas.vertx.orientdb.DocumentDatabase;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation.
 */
public class DocumentDatabaseImpl
    extends DatabaseSupport<DocumentDatabase, ODatabaseDocumentTx>
    implements DocumentDatabase
{
  public DocumentDatabaseImpl(final Vertx vertx, final String name, final ManagerImpl manager) {
    super(vertx, name, manager);
  }

  @Override
  public DocumentDatabase exec(final Handler<AsyncResult<ODatabaseDocumentTx>> handler) {
    manager.exec(vertx.getOrCreateContext(), getName(), handler);
    return this;
  }

  @Override
  public <T> DocumentDatabase stream(final String selectSql,
                                     final Map<String, Object> params,
                                     final Handler<AsyncResult<ReadStream<T>>> handler)
  {
    checkNotNull(selectSql);
    checkNotNull(handler);
    Context context = vertx.getOrCreateContext();
    manager.exec(vertx.getOrCreateContext(), getName(), adb -> {
      if (adb.succeeded()) {
        OrientReadStream<T> stream = new OrientReadStream<>(context);
        OSQLNonBlockingQuery<ODocument> query = new OSQLNonBlockingQuery<>(selectSql, stream);
        try {
          adb.result().command(query).execute(params);
          context.runOnContext(v -> handler.handle(Future.succeededFuture(stream)));
        }
        catch (Exception e) {
          context.runOnContext(v -> handler.handle(Future.failedFuture(e)));
        }
      }
      else {
        context.runOnContext(v -> handler.handle(Future.failedFuture(adb.cause())));
      }
    });
    return this;
  }

  //

  private static class OrientReadStream<T>
      implements ReadStream<T>, OCommandResultListener, Runnable
  {
    private static final Object SENTINEL = new Object();

    private final Context context;

    private final ConcurrentLinkedDeque<Object> queue;

    private final Semaphore queueSemaphore;

    private final Semaphore cycleSemaphore;

    private final Thread thread;

    private Handler<T> dataHandler;

    private Handler<Void> endHandler;

    private Handler<Throwable> exceptionHandler;

    /**
     * Permit count to release on cycle end to cycleSemaphore.
     *
     * 0 = paused
     * 1 = not paused
     */
    private volatile int cycleSemaphorePerms;

    public OrientReadStream(final Context context) {
      this.context = context;
      this.queue = new ConcurrentLinkedDeque<>();
      this.queueSemaphore = new Semaphore(0);
      this.cycleSemaphore = new Semaphore(0);
      this.cycleSemaphorePerms = 0;
      this.thread = new Thread(this, getClass().getSimpleName());
      this.thread.start();
    }

    // ReadStream

    @Override
    public ReadStream<T> exceptionHandler(final Handler<Throwable> handler) {
      this.exceptionHandler = handler;
      return this;
    }

    @Override
    public ReadStream<T> handler(final Handler<T> handler) {
      this.dataHandler = handler;
      return resume();
    }

    @Override
    public ReadStream<T> pause() {
      cycleSemaphorePerms = 0;
      return this;
    }

    @Override
    public ReadStream<T> resume() {
      if (cycleSemaphorePerms == 0) {
        cycleSemaphorePerms = 1;
        cycleSemaphore.release();
      }
      return this;
    }

    @Override
    public ReadStream<T> endHandler(final Handler<Void> handler) {
      this.endHandler = handler;
      return this;
    }

    // Runnable

    @Override
    public void run() {
      try {
        while (true) {
          cycleSemaphore.acquire();
          queueSemaphore.acquire();
          Object o = queue.pop();
          if (SENTINEL == o && endHandler != null) {
            context.runOnContext(v -> endHandler.handle(null));
            break;
          }
          if (dataHandler != null) {
            context.runOnContext(v -> dataHandler.handle((T) o));
          }
          cycleSemaphore.release(cycleSemaphorePerms);
        }
      }
      catch (Throwable e) {
        if (exceptionHandler != null) {
          context.runOnContext(v -> exceptionHandler.handle(e));
        }
      }
    }

    // OCommandResultListener

    @Override
    public boolean result(final Object o) {
      queue.push(o);
      queueSemaphore.release();
      return true;
    }

    @Override
    public void end() {
      queue.push(SENTINEL);
      queueSemaphore.release();
    }
  }
}
