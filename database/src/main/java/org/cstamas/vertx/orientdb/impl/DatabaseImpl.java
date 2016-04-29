package org.cstamas.vertx.orientdb.impl;

import java.util.Map;

import com.orientechnologies.orient.core.command.OCommandResultListener;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLAsynchQuery;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import org.cstamas.vertx.orientdb.Database;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation.
 */
public class DatabaseImpl
    implements Database
{
  private final String name;

  private final ManagerImpl manager;

  public DatabaseImpl(final String name, final ManagerImpl manager) {
    this.name = checkNotNull(name);
    this.manager = checkNotNull(manager);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Database exec(final Handler<AsyncResult<ODatabaseDocumentTx>> handler) {
    manager.exec(getName(), handler);
    return this;
  }

  @Override
  public <T> Database select(final ResultHandler<T> handler,
                             final String sql,
                             final Map<String, Object> params)
  {
    manager.exec(getName(), adb -> {
      if (adb.succeeded()) {
        OSQLAsynchQuery<ODocument> query = new OSQLAsynchQuery<>(
            sql,
            new OCommandResultListener()
            {
              @Override
              public boolean result(final Object iRecord) {
                final T doc = (T) iRecord;
                handler.handle(doc);
                return true;
              }

              @Override
              public void end() {
                handler.end();
              }
            }
        );
        adb.result().query(query, params);
      }
      else {
        handler.failure(adb.cause());
      }
    });
    return this;
  }

  @Override
  public void close(final Handler<AsyncResult<Void>> completionHandler) {
    manager.close(getName(), completionHandler);
  }
}
