package org.cstamas.vertx.orientdb.impl;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import org.cstamas.vertx.orientdb.DocumentDatabase;

/**
 * Default implementation.
 */
public class DocumentDatabaseImpl
    extends DatabaseSupport<DocumentDatabase, ODatabaseDocumentTx>
    implements DocumentDatabase
{
  public DocumentDatabaseImpl(final String name, final ManagerImpl manager) {
    super(name, manager);
  }

  @Override
  public DocumentDatabase exec(final Handler<AsyncResult<ODatabaseDocumentTx>> handler) {
    manager.exec(getName(), handler);
    return this;
  }
}
