package org.cstamas.vertx.orientdb.impl;

import java.util.Map;

import com.orientechnologies.orient.core.command.OCommandResultListener;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLNonBlockingQuery;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
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
  public <T> DocumentDatabase select(final ResultHandler<T> handler,
                                     final String sql,
                                     final Map<String, Object> params)
  {
    manager.exec(getName(), adb -> {
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
    });
    return this;
  }

  @Override
  public void close(final Handler<AsyncResult<Void>> completionHandler) {
    manager.close(getName(), completionHandler);
  }
}
