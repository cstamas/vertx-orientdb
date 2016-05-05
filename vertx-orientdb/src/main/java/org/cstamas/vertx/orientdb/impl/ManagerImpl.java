package org.cstamas.vertx.orientdb.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.base.Throwables;
import com.orientechnologies.orient.core.OConstants;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.config.OServerConfiguration;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import io.vertx.core.AsyncResult;
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Default implementation.
 */
public class ManagerImpl
    implements Manager
{
  private class DatabaseInfo
  {
    private final OPartitionedDatabasePool databasePool;

    private final List<Handler<Void>> closeHandlers;

    DatabaseInfo(final OPartitionedDatabasePool databasePool)
    {
      this.databasePool = databasePool;
      this.closeHandlers = new ArrayList<>();
    }

    void close() {
      closeHandlers.forEach(h -> h.handle(null));
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
    this.vertx = checkNotNull(vertx);
    this.managerOptions = checkNotNull(managerOptions);
    this.databaseInfos = new HashMap<>();
    log.info("OrientDB version " + OConstants.getVersion());
    open();
  }

  private void open() {
    try {
      if (managerOptions.isServerEnabled()) {
        openServer();
        log.info("OrientDB Server started");
      }
      else {
        log.info("OrientDB Server disabled.");
      }
      openManager();
      log.info("OrientDB Manager started");
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public void close(Handler<AsyncResult<Void>> handler) {
    vertx.executeBlocking(
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
  public Manager documentInstance(ConnectionOptions connectionOptions,
                                  @Nullable Handler<ODatabaseDocumentTx> openHandler,
                                  @Nullable Handler<AsyncResult<DocumentDatabase>> instanceHandler)
  {
    instance(
        connectionOptions,
        openHandler,
        instance -> {
          Future<DocumentDatabase> future;
          if (instance.succeeded()) {
            DocumentDatabase documentDatabase = new DocumentDatabaseImpl(connectionOptions.name(), this);
            future = Future.succeededFuture(documentDatabase);
          }
          else {
            future = Future.failedFuture(instance.cause());
          }
          if (instanceHandler != null) {
            instanceHandler.handle(future);
          }
        }
    );
    return this;
  }

  @Override
  public Manager graphInstance(final ConnectionOptions connectionOptions,
                               @Nullable final Handler<OrientGraphNoTx> openHandler,
                               @Nullable final Handler<AsyncResult<GraphDatabase>> instanceHandler)
  {
    instance(
        connectionOptions,
        db -> {
          OrientGraphNoTx notx = new OrientGraphNoTx(db);
          try {
            openHandler.handle(notx);
            notx.commit();
          }
          finally {
            notx.shutdown();
          }
        },
        instance -> {
          Future<GraphDatabase> future;
          if (instance.succeeded()) {
            GraphDatabase graphDatabase = new GraphDatabaseImpl(connectionOptions.name(), this);
            future = Future.succeededFuture(graphDatabase);
          }
          else {
            future = Future.failedFuture(instance.cause());
          }
          if (instanceHandler != null) {
            instanceHandler.handle(future);
          }
        }
    );
    return this;
  }

  private void instance(ConnectionOptions connectionOptions,
                        @Nullable Handler<ODatabaseDocumentTx> openHandler,
                        @Nullable Handler<AsyncResult<DatabaseInfo>> instanceHandler)
  {
    checkNotNull(connectionOptions);
    vertx.executeBlocking(
        f -> {
          try {
            DatabaseInfo databaseInfo;
            synchronized (databaseInfos) {
              checkArgument(!databaseInfos.containsKey(connectionOptions.name()),
                  "Database %s already exists", connectionOptions.name());
              if (databaseInfos.containsKey(connectionOptions.name())) {
                databaseInfo = databaseInfos.get(connectionOptions.name());
              }
              else {
                OPartitionedDatabasePool databasePool = createDocumentDbPool(connectionOptions, openHandler);
                databaseInfo = new DatabaseInfo(databasePool);
                databaseInfos.put(connectionOptions.name(), databaseInfo);
              }
            }
            f.complete(databaseInfo);
          }
          catch (Exception e) {
            f.fail(e);
          }
        },
        instanceHandler
    );
  }

  private void openServer() throws Exception {
    configureServer();

    // global startup
    Orient.instance().startup();

    // documentInstance startup
    OServer server = new OServer();
    server.setExtensionClassLoader(getClass().getClassLoader());
    server.setServerRootDirectory(orientHome.toString());
    server.startup(orientServerConfig.toFile());

    Orient.instance().removeShutdownHook();
    server.removeShutdownHook();

    // Orient.documentInstance().addDbLifecycleListener(entityHook);

    server.activate();

    this.orientServer = server;
  }

  private void configureServer() throws IOException {
    this.orientHome = Paths.get(managerOptions.getOrientHome()).toAbsolutePath();
    log.info("OrientDB home " + orientHome);
    this.orientServerConfig = orientHome.resolve(OServerConfiguration.DEFAULT_CONFIG_FILE);
    log.info("OrientDB config " + orientServerConfig);
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
      try (InputStream defaultConfig = getClass().getClassLoader()
          .getResourceAsStream("default-orientdb-server-config.xml")) {
        Files.copy(defaultConfig, orientServerConfig);
      }
      log.info("OrientDB managerOptions defaulted");
    }
  }

  private void closeServer() {
    // documentInstance shutdown
    orientServer.shutdown();
    orientServer = null;

    // global shutdown
    Orient.instance().shutdown();
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
  }

  private void closeManager() {
    databaseInfos.values().forEach(DatabaseInfo::close);
    databaseInfos.clear();
  }

  private OPartitionedDatabasePool createDocumentDbPool(final ConnectionOptions connectionOptions,
                                                        final @Nullable Handler<ODatabaseDocumentTx> openHandler)
  {
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
        if (openHandler != null) {
          openHandler.handle(db);
        }
      }
    }
    return new OPartitionedDatabasePool(
        uri,
        connectionOptions.username(),
        connectionOptions.password(),
        connectionOptions.maxPartitionSize(),
        connectionOptions.maxPoolSize()
    );
  }

  void exec(final String name, final Handler<AsyncResult<ODatabaseDocumentTx>> handler) {
    vertx.executeBlocking(
        f -> {
          try {
            DatabaseInfo databaseInfo = databaseInfos.get(name);
            checkState(databaseInfo != null, "Non-existent documentDatabase: %s", name);
            OPartitionedDatabasePool pool = databaseInfo.databasePool;
            try (ODatabaseDocumentTx db = pool.acquire()) {
              handler.handle(Future.succeededFuture(db));
            }
          }
          catch (Exception e) {
            handler.handle(Future.failedFuture(e));
          }
        },
        null
    );
  }

  synchronized void close(final String name, final Handler<AsyncResult<Void>> handler) {
    vertx.executeBlocking(
        f -> {
          try {
            synchronized (databaseInfos) {
              DatabaseInfo databaseInfo = databaseInfos.get(name);
              checkState(databaseInfo != null, "Non-existent documentDatabase: %s", name);
              databaseInfo.close();
              databaseInfos.remove(name);
            }
            f.complete();
          }
          catch (Exception e) {
            f.fail(e);
          }
        },
        handler
    );
  }
}
