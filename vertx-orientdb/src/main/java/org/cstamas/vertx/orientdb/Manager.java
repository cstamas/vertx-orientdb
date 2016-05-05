package org.cstamas.vertx.orientdb;

import javax.annotation.Nullable;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import io.vertx.core.AsyncResult;
import io.vertx.core.Closeable;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.cstamas.vertx.orientdb.impl.ManagerImpl;

/**
 * Embedded OrientDB documentInstance manager.
 */
public interface Manager
    extends Closeable
{
  /**
   * Creates a manager instance using passed in Vertx and options. This method will block and may take long time to
   * return.
   */
  static Manager create(Vertx vertx, ManagerOptions managerOptions) {
    return new ManagerImpl(vertx, managerOptions);
  }

  /**
   * Creates connection info using {@code plocal} prefix. It is a persistent disk based local database.
   */
  ConnectionOptions.Builder plocalConnection(String name);

  /**
   * Creates connection info using {@code remote} prefix. It connects to a remote OrientDB Server instance.
   */
  ConnectionOptions.Builder remoteConnection(String name, String hostname, String remoteName);

  /**
   * Creates connection info using {@code memory} prefix. It is a non-persistent database, held completely in memory.
   */
  ConnectionOptions.Builder memoryConnection(String name);

  /**
   * Opens or creates a new named {@link DocumentDatabase} with given {@code name}. Before creating pool, the
   * passed in {@code openHandler} is invoked if not {@code null} to perform possible maintenance, like schema
   * upgrade/initialization, pre-loading, etc if needed. Instances created by this method are held my manager, so any
   * subsequent call of this method will access already created/opened documentInstance that was cached quickly.
   * Instances are cached, hence any subsequent call will do just "get from cache", where {@code openHandler} will not
   * be invoked, and {@code instanceHandler} will receive the previously created instance.
   *
   * @param connectionOptions the orientdb connection information.
   * @param openHandler       the handler to invoke in single-connection mode, useful to set up schema, upgrade schema
   *                          or so, if needed, may be {@code null}.
   * @param instanceHandler   the handler invoked when documentInstance is constructed, may be {@code null}, as created
   *                          instances are held by the manager, so they can be queries later using this same method.
   */
  Manager documentInstance(ConnectionOptions connectionOptions,
                           @Nullable Handler<ODatabaseDocumentTx> openHandler,
                           @Nullable Handler<AsyncResult<DocumentDatabase>> instanceHandler);

  /**
   * Opens or creates a new named {@link GraphDatabase} with given {@code name}. Before creating pool, the
   * passed in {@code openHandler} is invoked if not {@code null} to perform possible maintenance, like schema
   * upgrade/initialization, pre-loading, etc if needed. Instances created by this method are held my manager, so any
   * subsequent call of this method will access already created/opened documentInstance that was cached quickly.
   * Instances are cached, hence any subsequent call will do just "get from cache", where {@code openHandler} will not
   * be invoked, and {@code instanceHandler} will receive the previously created instance.
   *
   * @param connectionOptions the orientdb connection information.
   * @param openHandler       the handler to invoke in single-connection mode, useful to set up schema, upgrade schema
   *                          or so, if needed, may be {@code null}.
   * @param instanceHandler   the handler invoked when documentInstance is constructed, may be {@code null}, as created
   *                          instances are held by the manager, so they can be queries later using this same method.
   */
  Manager graphInstance(ConnectionOptions connectionOptions,
                        @Nullable Handler<OrientGraphNoTx> openHandler,
                        @Nullable Handler<AsyncResult<GraphDatabase>> instanceHandler);
}
