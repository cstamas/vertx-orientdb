package org.cstamas.vertx.orientdb.impl;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
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
  public void close(final Handler<AsyncResult<Void>> completionHandler) {
    manager.close(getName(), completionHandler);
  }
}
