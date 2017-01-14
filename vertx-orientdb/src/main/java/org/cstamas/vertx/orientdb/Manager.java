package org.cstamas.vertx.orientdb;

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
   * Creates a manager instance using passed in Vertx and options. The manager must be {@link #open(Handler)} before
   * use.
   */
  static Manager create(Vertx vertx, ManagerOptions managerOptions) {
    return new ManagerImpl(vertx, managerOptions);
  }

  /**
   * Opens database (and server is {@link ManagerOptions} says so), this might take a while.
   */
  Manager open(Handler<AsyncResult<Void>> handler);

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
   * passed in {@code handler} is invoked to perform possible maintenance, like schema upgrade/initialization,
   * pre-loading, etc if needed. Instances created by this method are held my manager, so any
   * subsequent call of {@link #documentInstance(String, Handler)} method will access already created/opened
   * documentInstance that was cached.
   *
   * @param connectionOptions the orientdb connection information.
   * @param handler           the handler to invoke in single-connection mode, useful to set up schema, upgrade schema
   *                          or so.
   */
  Manager createDocumentInstance(ConnectionOptions connectionOptions,
                                 Handler<AsyncResult<ODatabaseDocumentTx>> handler);

  /**
   * Gets an instance created with {@link #createDocumentInstance(ConnectionOptions, Handler)}.
   *
   * @param name    the orientdb database name as defined in {@link ConnectionOptions} when created.
   * @param handler the handler invoked when documentInstance is constructed.
   */
  Manager documentInstance(String name, Handler<AsyncResult<DocumentDatabase>> handler);

  /**
   * Opens or creates a new named {@link GraphDatabase} with given {@code name}. Before creating pool, the
   * passed in {@code handler} is invoked if not {@code null} to perform possible maintenance, like schema
   * upgrade/initialization, pre-loading, etc if needed. Instances created by this method are held my manager, so any
   * subsequent call of {@link #graphInstance(String, Handler)} method will access already
   * created/opened documentInstance that was cached.
   *
   * @param connectionOptions the orientdb connection information.
   * @param handler           the handler to invoke in single-connection mode, useful to set up schema, upgrade schema
   *                          or so.
   */
  Manager createGraphInstance(ConnectionOptions connectionOptions,
                              Handler<AsyncResult<OrientGraphNoTx>> handler);

  /**
   * Gets an instance created with {@link #createGraphInstance(ConnectionOptions, Handler)}.
   *
   * @param name    the orientdb database name as defined in {@link ConnectionOptions} when created.
   * @param handler the handler invoked when documentInstance is constructed.
   */
  Manager graphInstance(String name, Handler<AsyncResult<GraphDatabase>> handler);
}
