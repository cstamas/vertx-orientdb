package org.cstamas.vertx.orientdb.impl;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;

import com.orientechnologies.orient.core.command.OCommandResultListener;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLNonBlockingQuery;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;
import org.cstamas.vertx.orientdb.DocumentDatabase;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation.
 */
public class DocumentDatabaseImpl
    implements DocumentDatabase
{
  private final String name;

  private final ManagerImpl manager;

  public DocumentDatabaseImpl(final String name, final ManagerImpl manager) {
    this.name = checkNotNull(name);
    this.manager = checkNotNull(manager);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public DocumentDatabase exec(final Handler<AsyncResult<ODatabaseDocumentTx>> handler) {
    manager.exec(getName(), handler);
    return this;
  }

  @Override
  public <T> DocumentDatabase stream(final String selectSql,
                                     final Map<String, Object> params,
                                     final Handler<AsyncResult<ReadStream<T>>> handler)
  {
    checkNotNull(selectSql);
    checkNotNull(handler);
    manager.exec(getName(), adb -> {
      if (adb.succeeded()) {
        OrientReadStream<T> stream = new OrientReadStream<>();
        OSQLNonBlockingQuery<ODocument> query = new OSQLNonBlockingQuery<>(selectSql, stream);
        try {
          adb.result().command(query).execute(params);
          handler.handle(Future.succeededFuture(stream));
        }
        catch (Exception e) {
          handler.handle(Future.failedFuture(e));
        }
      }
      else {
        handler.handle(Future.failedFuture(adb.cause()));
      }
    });
    return this;
  }

  @Override
  public void close(final Handler<AsyncResult<Void>> completionHandler) {
    manager.close(getName(), completionHandler);
  }

  //

  private static class OrientReadStream<T>
      implements ReadStream<T>, OCommandResultListener, Runnable
  {
    private static final Object SENTINEL = new Object();

    private final ArrayBlockingQueue<Object> queue;

    private final Semaphore semaphore;

    private final Thread thread;

    private Handler<T> dataHandler;

    private Handler<Void> endHandler;

    private Handler<Throwable> exceptionHandler;

    /**
     * 0 = not paused
     * 1 = paused and permission taken from semaphore
     * -1 = paused, and permission not taken from semaphore
     */
    private volatile int paused;

    public OrientReadStream() {
      this.queue = new ArrayBlockingQueue<>(16);
      this.semaphore = new Semaphore(0);
      this.paused = 0;
      this.thread = new Thread(this, getClass().getSimpleName());
      this.thread.start();
    }

    @Override
    public ReadStream<T> exceptionHandler(final Handler<Throwable> handler) {
      this.exceptionHandler = handler;
      return this;
    }

    @Override
    public ReadStream<T> handler(final Handler<T> handler) {
      this.dataHandler = handler;
      if (paused == 0) {
        this.semaphore.release();
      }
      return this;
    }

    @Override
    public ReadStream<T> pause() {
      if (paused == 0) {
        if (semaphore.tryAcquire()) {
          paused = 1;
        }
        else {
          paused = -1;
        }
      }
      return this;
    }

    @Override
    public ReadStream<T> resume() {
      if (paused == 1 || paused == -1) {
        if (paused == 1) {
          semaphore.release();
        }
        paused = 0;
      }
      return this;
    }

    @Override
    public ReadStream<T> endHandler(final Handler<Void> handler) {
      this.endHandler = handler;
      return this;
    }

    //

    @Override
    public void run() {
      try {
        while (true) {
          semaphore.acquire();
          semaphore.release();
          Object o = queue.take();
          if (SENTINEL == o && endHandler != null) {
            endHandler.handle(null);
            break;
          }
          if (dataHandler != null) {
            dataHandler.handle((T) o);
          }
        }
      }
      catch (Throwable e) {
        if (exceptionHandler != null) {
          exceptionHandler.handle(e);
        }
      }
    }

    //

    @Override
    public boolean result(final Object o) {
      try {
        queue.put(o);
        return true;
      }
      catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void end() {
      try {
        queue.put(SENTINEL);
      }
      catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
