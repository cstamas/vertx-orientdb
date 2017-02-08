package org.cstamas.vertx.orientdb.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import org.cstamas.vertx.orientdb.Database;

import static java.util.Objects.requireNonNull;

/**
 * Support class.
 */
public abstract class DatabaseSupport<T, OT>
    implements Database<T, OT>
{
  protected final Context context;

  protected final String name;

  protected final ManagerImpl manager;

  public DatabaseSupport(final Context context, final String name, final ManagerImpl manager) {
    this.context = requireNonNull(context);
    this.name = requireNonNull(name);
    this.manager = requireNonNull(manager);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void close(final Handler<AsyncResult<Void>> handler) {
    manager.close(getName(), handler);
  }
}
