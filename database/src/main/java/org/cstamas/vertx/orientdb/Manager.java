package org.cstamas.vertx.orientdb;

import javax.annotation.Nullable;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import io.vertx.core.AsyncResult;
import io.vertx.core.Closeable;
import io.vertx.core.Handler;

/**
 * Embedded OrientDB documentInstance manager.
 */
public interface Manager
    extends Closeable
{
  /**
   * Opens the database manager, this may be lengthy operation (ie. Orient repairing itself on a large database).
   *
   * @param handler the handler invoked when open completed and manager is ready for use.
   */
  void open(Handler<AsyncResult<Manager>> handler);

  /**
   * Connection descriptor.
   */
  interface ConnectionInfo
  {
    /**
     * The connection name. Should be URL safe name, and databases and connections are keyed by this property.
     */
    String name();

    /**
     * The connection URI.
     */
    String uri();

    /**
     * Username to be used with connection.
     */
    String username();

    /**
     * Password to be used with connection.
     */
    String password();
  }

  /**
   * Creates connection info using {@code plocal} prefix. It is a persistent disk based local database.
   */
  ConnectionInfo plocalConnection(String name);

  /**
   * Creates connection info using {@code remote} prefix. It connects to a remote OrientDB Server instance.
   */
  ConnectionInfo remoteConnection(String name, String hostname, String remoteName, String username, String password);

  /**
   * Creates connection info using {@code memory} prefix. It is a non-persistent database, held completely in memory.
   */
  ConnectionInfo memoryConnection(String name);

  /**
   * Opens or creates a new named {@link DocumentDatabase} with given {@code name}. Before creating pool, the
   * passed in {@code openHandler} is invoked if not {@code null} to perform possible maintenance, like schema
   * upgrade/initialization, etc if needed. Instances created by this method are held my manager, so any subsequent
   * call of this method will access already created/opened instance that was cached quickly.
   *
   * @param connectionInfo  the orientdb connection information.
   * @param openHandler     the handler to invoke in single-connection mode, useful to set up schema, upgrade schema or
   *                        so, if needed, may be {@code null}.
   * @param instanceHandler the handler invoked when documentInstance is constructed, may be {@code null}, as created
   *                        instances are held by the manager, so they can be queries later using this same method.
   */
  Manager documentInstance(ConnectionInfo connectionInfo,
                           @Nullable Handler<ODatabaseDocumentTx> openHandler,
                           @Nullable Handler<AsyncResult<DocumentDatabase>> instanceHandler);
}
