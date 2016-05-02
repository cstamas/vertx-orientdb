package org.cstamas.vertx.orientdb.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import javax.annotation.Nullable;

import com.orientechnologies.orient.core.OConstants;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.config.OServerConfiguration;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.serviceproxy.ProxyHelper;
import org.cstamas.vertx.orientdb.Configuration;
import org.cstamas.vertx.orientdb.DocumentDatabase;
import org.cstamas.vertx.orientdb.DocumentDatabaseService;
import org.cstamas.vertx.orientdb.Manager;

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
    private final DocumentDatabase documentDatabase;

    private final OPartitionedDatabasePool databasePool;

    private final MessageConsumer<JsonObject> serviceMessageConsumer;

    public DatabaseInfo(final DocumentDatabase documentDatabase,
                        final OPartitionedDatabasePool databasePool,
                        final MessageConsumer<JsonObject> serviceMessageConsumer)
    {
      this.documentDatabase = documentDatabase;
      this.databasePool = databasePool;
      this.serviceMessageConsumer = serviceMessageConsumer;
    }

    public void close() {
      ProxyHelper.unregisterService(serviceMessageConsumer);
      databasePool.close();
    }
  }

  private static final String ADMIN_USER = "admin";

  private static final String ADMIN_PASSWORD = "admin";

  private static final String PLOCAL_PREFIX = "plocal:";

  private static final String REMOTE_PREFIX = "remote:";

  private static final String MEMORY_PREFIX = "memory:";

  private static final Logger log = LoggerFactory.getLogger(ManagerImpl.class);

  private final Vertx vertx;

  private final Configuration configuration;

  private final HashMap<String, DatabaseInfo> databaseInfos;

  private Path orientHome;

  private Path databasesDir;

  private Path orientServerConfig;

  private OServer orientServer;

  public ManagerImpl(final Vertx vertx, final Configuration configuration) throws IOException
  {
    this.vertx = checkNotNull(vertx);
    this.configuration = checkNotNull(configuration);
    this.databaseInfos = new HashMap<>();
    log.info("OrientDB version " + OConstants.getVersion());
  }

  @Override
  public void open(final Handler<AsyncResult<Manager>> handler) {
    vertx.executeBlocking(
        f -> {
          try {
            if (configuration.isServerEnabled()) {
              openServer();
              log.info("OrientDB Server started");
            }
            else {
              log.info("OrientDB Server disabled.");
            }
            openManager();
            log.info("OrientDB Manager started");
            f.complete(this);
          }
          catch (Exception e) {
            f.fail(e);
          }
        },
        handler
    );
  }

  @Override
  public void close(Handler<AsyncResult<Void>> handler) {
    vertx.executeBlocking(
        f -> {
          try {
            if (configuration.isServerEnabled()) {
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
  public ConnectionInfo plocalConnection(final String name) {
    return new ConnectionInfoImpl(
        name,
        PLOCAL_PREFIX + databasesDir.resolve(name).toAbsolutePath(),
        ADMIN_USER,
        ADMIN_PASSWORD
    );
  }

  @Override
  public ConnectionInfo remoteConnection(final String name,
                                         final String hostname,
                                         final String remoteName,
                                         final String username,
                                         final String password)
  {
    return new ConnectionInfoImpl(name, REMOTE_PREFIX + hostname + '/' + remoteName, username, password);
  }

  @Override
  public ConnectionInfo memoryConnection(final String name) {
    return new ConnectionInfoImpl(name, MEMORY_PREFIX + name, ADMIN_USER, ADMIN_PASSWORD);
  }

  @Override
  public Manager documentInstance(ConnectionInfo connectionInfo,
                                  @Nullable Handler<ODatabaseDocumentTx> openHandler,
                                  @Nullable Handler<AsyncResult<DocumentDatabase>> instanceHandler)
  {
    checkNotNull(connectionInfo);
    vertx.executeBlocking(
        f -> {
          try {
            DocumentDatabase documentDatabase;
            if (databaseInfos.containsKey(connectionInfo.name())) {
              documentDatabase = databaseInfos.get(connectionInfo.name()).documentDatabase;
            }
            else {
              OPartitionedDatabasePool pool = createPool(connectionInfo, openHandler);
              documentDatabase = new DocumentDatabaseImpl(connectionInfo.name(), this);
              MessageConsumer<JsonObject> serviceMessageConsumer = ProxyHelper
                  .registerService(DocumentDatabaseService.class, vertx,
                      new DocumentDatabaseServiceImpl(documentDatabase), connectionInfo.name());
              DatabaseInfo databaseInfo = new DatabaseInfo(documentDatabase, pool, serviceMessageConsumer);
              databaseInfos.put(connectionInfo.name(), databaseInfo);
            }
            f.complete(documentDatabase);
          }
          catch (Exception e) {
            f.fail(e);
          }
        },
        instanceHandler
    );
    return this;
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
    this.orientHome = Paths.get(configuration.getOrientHome()).toAbsolutePath();
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
      log.info("OrientDB configuration defaulted");
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
      // server is running, obey server configuration
      this.databasesDir = Paths.get(orientServer.getDatabaseDirectory());
    }
    else {
      // just embedded, obey our configuration
      Path orientHome = Paths.get(configuration.getOrientHome()).toAbsolutePath();
      this.databasesDir = orientHome.resolve("databases");
    }
  }

  private void closeManager() {
    databaseInfos.values().forEach(DatabaseInfo::close);
    databaseInfos.clear();
  }

  private OPartitionedDatabasePool createPool(final ConnectionInfo connectionInfo,
                                              final @Nullable Handler<ODatabaseDocumentTx> openHandler)
  {
    final String uri = connectionInfo.uri();
    if (!uri.startsWith(REMOTE_PREFIX)) {
      try (ODatabaseDocumentTx db = new ODatabaseDocumentTx(uri)) {
        if (db.exists()) {
          db.open(connectionInfo.username(), connectionInfo.password());
          log.debug("Opened existing " + connectionInfo.name() + " -> " + uri);
        }
        else {
          db.create();
          log.debug("Created new " + connectionInfo.name() + " -> " + uri);
        }
        if (openHandler != null) {
          openHandler.handle(db);
        }
      }
    }
    return new OPartitionedDatabasePool(uri, connectionInfo.username(), connectionInfo.password());
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
            DatabaseInfo databaseInfo = databaseInfos.get(name);
            checkState(databaseInfo != null, "Non-existent documentDatabase: %s", name);
            databaseInfo.close();
            databaseInfos.remove(name);
            f.complete();
          }
          catch (Exception e) {
            f.fail(e);
          }
        },
        handler
    );
  }

  private static final class ConnectionInfoImpl
      implements ConnectionInfo
  {
    private final String name;

    private final String uri;

    private final String username;

    private final String password;

    public ConnectionInfoImpl(final String name, final String uri, final String username, final String password) {
      this.name = name;
      this.uri = uri;
      this.username = username;
      this.password = password;
    }

    public String name() {
      return name;
    }

    public String uri() {
      return uri;
    }

    public String username() {
      return username;
    }

    public String password() {
      return password;
    }
  }
}
