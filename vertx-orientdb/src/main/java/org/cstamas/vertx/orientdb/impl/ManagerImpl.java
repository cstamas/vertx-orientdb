package org.cstamas.vertx.orientdb.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import com.orientechnologies.orient.core.OConstants;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.config.OServerConfiguration;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.cstamas.vertx.orientdb.ConnectionOptions;
import org.cstamas.vertx.orientdb.DocumentDatabase;
import org.cstamas.vertx.orientdb.GraphDatabase;
import org.cstamas.vertx.orientdb.Manager;
import org.cstamas.vertx.orientdb.ManagerOptions;

import static java.util.Objects.requireNonNull;

/**
 * Default implementation.
 */
public class ManagerImpl
    implements Manager
{
  private class DatabaseInfo
  {
    private final OPartitionedDatabasePool databasePool;

    DatabaseInfo(final OPartitionedDatabasePool databasePool)
    {
      this.databasePool = databasePool;
    }

    void close() {
      databasePool.close();
    }
  }

  private static final String PLOCAL_PREFIX = "plocal:";

  private static final String REMOTE_PREFIX = "remote:";

  private static final String MEMORY_PREFIX = "memory:";

  private static final Logger log = LoggerFactory.getLogger(ManagerImpl.class);

  private final Vertx vertx;

  private final ManagerOptions managerOptions;

  private final HashMap<String, DatabaseInfo> databaseInfos;

  private Path orientHome;

  private Path databasesDir;

  private Path orientServerConfig;

  private OServer orientServer;

  public ManagerImpl(final Vertx vertx, final ManagerOptions managerOptions)
  {
    this.vertx = requireNonNull(vertx);
    this.managerOptions = requireNonNull(managerOptions);
    this.databaseInfos = new HashMap<>();
  }

  private <T> void executeBlocking(Handler<Future<T>> blockingCodeHandler, Handler<AsyncResult<T>> resultHandler) {
    vertx.executeBlocking(blockingCodeHandler, resultHandler);
  }

  @Override
  public Manager open(final Handler<AsyncResult<Void>> handler) {
    executeBlocking(
        f -> {
          try {
            open();
            f.complete();
          }
          catch (Exception e) {
            f.fail(e);
          }
        },
        handler
    );
    return this;
  }

  @Override
  public void close(final Handler<AsyncResult<Void>> handler) {
    executeBlocking(
        f -> {
          try {
            if (managerOptions.isServerEnabled()) {
              closeServer();
              log.info("OrientDB Server shutdown");
            }
            closeManager();
            log.info("OrientDB shutdown");
            f.complete();
          }
          catch (Exception e) {
            f.fail(e);
          }
        },
        handler
    );
  }

  private void closeServer() {
    // documentInstance shutdown
    orientServer.shutdown();
    orientServer = null;
  }

  private void closeManager() {
    synchronized (databaseInfos) {
      databaseInfos.values().forEach(DatabaseInfo::close);
      databaseInfos.clear();
    }

    // global shutdown
    Orient.instance().shutdown();
  }

  @Override
  public ConnectionOptions.Builder plocalConnection(final String name) {
    return new ConnectionOptions.Builder(
        name, PLOCAL_PREFIX + databasesDir.resolve(name).toAbsolutePath()
    );
  }

  @Override
  public ConnectionOptions.Builder remoteConnection(final String name, final String hostname, final String remoteName) {
    return new ConnectionOptions.Builder(
        name, REMOTE_PREFIX + hostname + '/' + remoteName
    );
  }

  @Override
  public ConnectionOptions.Builder memoryConnection(final String name) {
    return new ConnectionOptions.Builder(
        name, MEMORY_PREFIX + name
    );
  }

  @Override
  public Manager createDocumentInstance(final ConnectionOptions connectionOptions,
                                        final Handler<ODatabaseDocumentTx> handler,
                                        final Handler<AsyncResult<Void>> resultHandler)
  {
    create(connectionOptions, handler, resultHandler);
    return this;
  }

  @Override
  public Manager documentInstance(final String name, final Handler<AsyncResult<DocumentDatabase>> handler)
  {
    executeBlocking(
        f -> {
          try {
            DatabaseInfo databaseInfo = databaseInfos.get(name);
            if (databaseInfo == null) {
              f.fail(new IllegalArgumentException("Doc: Non existent database:" + name));
            }
            else {
              f.complete(new DocumentDatabaseImpl(name, this));
            }
          }
          catch (Exception e) {
            f.fail(e);
          }
        },
        handler
    );
    return this;
  }

  @Override
  public Manager createGraphInstance(final ConnectionOptions connectionOptions,
                                     final Handler<OrientGraphNoTx> handler,
                                     final Handler<AsyncResult<Void>> resultHandler)
  {
    Handler<ODatabaseDocumentTx> handlerWrapper = adb -> {
      handler.handle(new OrientGraphNoTx(adb));
    };
    create(connectionOptions, handlerWrapper, resultHandler);
    return this;
  }

  @Override
  public Manager graphInstance(final String name,
                               final Handler<AsyncResult<GraphDatabase>> handler)
  {
    executeBlocking(
        f -> {
          try {
            DatabaseInfo databaseInfo = databaseInfos.get(name);
            if (databaseInfo == null) {
              f.fail(new IllegalArgumentException("Graph: Non existent database:" + name));
            }
            else {
              f.complete(new GraphDatabaseImpl(name, this));
            }
          }
          catch (Exception e) {
            f.fail(e);
          }
        },
        handler
    );
    return this;
  }

  private void open() throws Exception {
    try {
      log.info("OrientDB " + OConstants.getVersion() + " manager started");
      openManager();
      if (managerOptions.isServerEnabled()) {
        openServer();
      }
    }
    catch (Exception e) {
      log.error("Could not open database", e);
      throw e;
    }
  }

  private void openManager() throws IOException {
    if (orientServer != null) {
      // server is running, obey server managerOptions
      this.databasesDir = Paths.get(orientServer.getDatabaseDirectory());
    }
    else {
      // just embedded, obey our managerOptions
      Path orientHome = Paths.get(managerOptions.getOrientHome()).toAbsolutePath();
      this.databasesDir = orientHome.resolve("databases");
    }
    // global startup
    Orient.instance().startup();
    Orient.instance().removeShutdownHook();
  }

  private void openServer() throws Exception {
    configureServer();

    // documentInstance startup
    OServer server = new OServer();
    server.setExtensionClassLoader(getClass().getClassLoader());
    server.setServerRootDirectory(orientHome.toString());
    server.startup(orientServerConfig.toFile());

    server.removeShutdownHook();

    // Orient.documentInstance().addDbLifecycleListener(entityHook);

    server.activate();

    this.orientServer = server;
  }

  private void configureServer() throws IOException {
    this.orientHome = Paths.get(managerOptions.getOrientHome()).toAbsolutePath();
    log.info(Orient.ORIENTDB_HOME + "=" + orientHome);
    this.orientServerConfig = orientHome.resolve(OServerConfiguration.DEFAULT_CONFIG_FILE);
    log.info("OrientDB server configuration: " + orientServerConfig);
    mayDefaultServer();

    System.setProperty("orient.home", orientHome.toString());
    System.setProperty(Orient.ORIENTDB_HOME, orientHome.toString());
  }

  private void mayDefaultServer() throws IOException {
    if (!Files.isDirectory(orientHome)) {
      Files.createDirectories(orientHome);
    }
    if (!Files.isRegularFile(orientServerConfig)) {
      Files.createDirectories(orientServerConfig.getParent());
      copy("defaults/orientdb-server-config.xml", orientServerConfig);
      copy("defaults/automatic-backup.json", orientServerConfig.getParent().resolve("automatic-backup.json"));
      copy("defaults/default-distributed-db-config.json",
          orientServerConfig.getParent().resolve("default-distributed-db-config.json"));
      copy("defaults/security.json", orientServerConfig.getParent().resolve("security.json"));
      log.info("OrientDB Server defaulted!");
    }
  }

  private void copy(final String name, final Path target) throws IOException {
    try (InputStream defaultConfig = getClass().getClassLoader()
        .getResourceAsStream(name)) {
      Files.copy(defaultConfig, target);
    }
  }

  private void create(final ConnectionOptions connectionOptions,
                      final Handler<ODatabaseDocumentTx> handler,
                      final Handler<AsyncResult<Void>> resultHandler)
  {
    executeBlocking(
        f -> {
          try {
            synchronized (databaseInfos) {
              final String uri = connectionOptions.uri();
              if (!uri.startsWith(REMOTE_PREFIX)) {
                try (ODatabaseDocumentTx db = new ODatabaseDocumentTx(uri)) {
                  if (db.exists()) {
                    db.open(connectionOptions.username(), connectionOptions.password());
                    log.debug("Opened existing " + connectionOptions.name() + " -> " + uri);
                  }
                  else {
                    db.create();
                    log.debug("Created new " + connectionOptions.name() + " -> " + uri);
                  }
                  try {
                    handler.handle(db);
                  }
                  catch (Exception e) {
                    log.warn("Creation/Open handler failure", e);
                  }
                }
              }
              OPartitionedDatabasePool pool = new OPartitionedDatabasePool(
                  uri,
                  connectionOptions.username(),
                  connectionOptions.password(),
                  connectionOptions.maxPartitionSize(),
                  connectionOptions.maxPoolSize()
              );
              if (uri.startsWith(REMOTE_PREFIX)) {
                try (ODatabaseDocumentTx db = pool.acquire()) {
                  handler.handle(db);
                }
              }
              DatabaseInfo info = new DatabaseInfo(pool);
              databaseInfos.put(connectionOptions.name(), info);
              f.complete();
            }
          }
          catch (Exception e) {
            f.fail(e);
          }
        },
        resultHandler
    );
  }

  void exec(final String name, final Handler<AsyncResult<ODatabaseDocumentTx>> handler) {
    executeBlocking(
        f -> {
          try {
            DatabaseInfo databaseInfo = databaseInfos.get(name);
            if (databaseInfo == null) {
              IllegalArgumentException iaex = new IllegalArgumentException("Exec: Non existent database: " + name);
              handler.handle(Future.failedFuture(iaex));
              f.fail(iaex);
            }
            else {
              OPartitionedDatabasePool pool = databaseInfo.databasePool;
              try (ODatabaseDocumentTx db = pool.acquire()) {
                handler.handle(Future.succeededFuture(db));
              }
              f.complete();
            }
          }
          catch (Exception e) {
            handler.handle(Future.failedFuture(e));
            f.fail(e);
          }
        },
        v -> {
        }
    );
  }

  void close(final String name, final Handler<AsyncResult<Void>> handler) {
    executeBlocking(
        f -> {
          try {
            synchronized (databaseInfos) {
              DatabaseInfo databaseInfo = databaseInfos.get(name);
              if (databaseInfo == null) {
                f.fail(new IllegalArgumentException("Close: Non existent database: " + name));
              }
              else {
                databaseInfo.close();
                databaseInfos.remove(name);
                f.complete();
              }
            }
          }
          catch (Exception e) {
            f.fail(e);
          }
        },
        handler
    );
  }
}
