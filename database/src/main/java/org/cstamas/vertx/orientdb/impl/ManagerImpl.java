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
import org.cstamas.vertx.orientdb.Database;
import org.cstamas.vertx.orientdb.DatabaseService;
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
    private final Database database;

    private final OPartitionedDatabasePool databasePool;

    private final MessageConsumer<JsonObject> serviceMessageConsumer;

    public DatabaseInfo(final Database database,
                        final OPartitionedDatabasePool databasePool,
                        final MessageConsumer<JsonObject> serviceMessageConsumer)
    {
      this.database = database;
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
  public Manager instance(String name,
                          @Nullable Handler<ODatabaseDocumentTx> openHandler,
                          Handler<AsyncResult<Database>> instanceHandler)
  {
    vertx.executeBlocking(
        f -> {
          try {
            checkNotNull(name);
            Database database;
            if (databaseInfos.containsKey(name)) {
              database = databaseInfos.get(name).database;
            }
            else {
              OPartitionedDatabasePool pool = createPool(name, openHandler);
              database = new DatabaseImpl(name, this);
              MessageConsumer<JsonObject> serviceMessageConsumer = ProxyHelper
                  .registerService(DatabaseService.class, vertx, new DatabaseServiceImpl(database), name);
              DatabaseInfo databaseInfo = new DatabaseInfo(database, pool, serviceMessageConsumer);
              databaseInfos.put(name, databaseInfo);
            }
            f.complete(database);
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

    // instance startup
    OServer server = new OServer();
    server.setExtensionClassLoader(getClass().getClassLoader());
    server.setServerRootDirectory(orientHome.toString());
    server.startup(orientServerConfig.toFile());

    Orient.instance().removeShutdownHook();
    server.removeShutdownHook();

    // Orient.instance().addDbLifecycleListener(entityHook);

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
    // instance shutdown
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

  private OPartitionedDatabasePool createPool(final String name,
                                              @Nullable Handler<ODatabaseDocumentTx> openHandler)
  {
    String uri = "plocal:" + databasesDir.resolve(name).toAbsolutePath();
    try (ODatabaseDocumentTx db = new ODatabaseDocumentTx(uri)) {
      if (db.exists()) {
        db.open(ADMIN_USER, ADMIN_PASSWORD);
        log.debug("Opened orientdb: " + name + " -> " + uri);
      }
      else {
        db.create();
        log.debug("Created orientdb: " + name + " -> " + uri);
      }
      if (openHandler != null) {
        openHandler.handle(db);
      }
    }
    return new OPartitionedDatabasePool(uri, ADMIN_USER, ADMIN_PASSWORD);
  }

  void exec(final String name, final Handler<AsyncResult<ODatabaseDocumentTx>> handler) {
    vertx.executeBlocking(
        f -> {
          try {
            DatabaseInfo databaseInfo = databaseInfos.get(name);
            checkState(databaseInfo != null, "Non-existent database: %s", name);
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
            checkState(databaseInfo != null, "Non-existent database: %s", name);
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
}
