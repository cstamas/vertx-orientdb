package org.cstamas.vertx.orientdb.examples;

import java.util.Objects;

import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.serviceproxy.ProxyHelper;
import org.cstamas.vertx.orientdb.ConnectionOptions;
import org.cstamas.vertx.orientdb.DocumentDatabase;
import org.cstamas.vertx.orientdb.Manager;
import org.cstamas.vertx.orientdb.ManagerOptions;
import org.cstamas.vertx.orientdb.examples.service.DocumentDatabaseService;
import org.cstamas.vertx.orientdb.examples.service.impl.DocumentDatabaseServiceImpl;

/**
 * OrientDB test verticle.
 */
public class TestVerticle
    extends AbstractVerticle
{
  private static final Logger log = LoggerFactory.getLogger(TestVerticle.class);

  private Manager manager;

  private ReaderVerticle readerVerticle;

  private WriterVerticle writerVerticle;

  private long readPeriodic;

  private long writePeriodic;

  private MessageConsumer<JsonObject> proxy;

  @Override
  public void start(final Future<Void> startFuture) throws Exception {
    this.manager = Manager.create(vertx, ManagerOptions.fromJsonObject(config()));
    manager.open(v -> {
      Future<Void> future = Future.future();
      future.setHandler(vv -> {
        // fire events that cause READ and WRITE operations
        readPeriodic = vertx.setPeriodic(50,
            t -> {
              vertx.eventBus().publish("read", null);
            });
        writePeriodic = vertx.setPeriodic(50,
            t -> {
              long now = System.currentTimeMillis();
              vertx.eventBus().publish("write", new JsonObject().put("name", String.valueOf(now)).put("value", now));
            });
        startFuture.complete(null);
      });
      createDocumentDatabase(future.completer());
    });
  }

  private void createDocumentDatabase(Handler<AsyncResult<Void>> handler) {
    ConnectionOptions connectionOptions = selectConnectionInfo();
    manager.createDocumentInstance(
        connectionOptions,
        adb -> {
          if (adb.failed()) {
            log.warn("Failed to create DB", adb.cause());
            handler.handle(Future.failedFuture(adb.cause()));
          }
          else {
            OSchema schema = adb.result().getMetadata().getSchema();
            if (!schema.existsClass("test")) {
              OClass oclass = schema.createClass("test");
              oclass.createProperty("name", OType.STRING);
              oclass.createProperty("value", OType.STRING);
            }
            deployVerticles(connectionOptions, handler);
          }
        }
    );
  }

  private void deployVerticles(ConnectionOptions connectionOptions, Handler<AsyncResult<Void>> handler) {
    manager.documentInstance(connectionOptions.name(), adb -> {
      if (adb.failed()) {
        log.warn("Failed to deploy verticles", adb.cause());
        handler.handle(Future.failedFuture(adb.cause()));
      }
      else {
        DocumentDatabase documentDatabase = adb.result();
        writerVerticle = new WriterVerticle(documentDatabase);
        vertx.deployVerticle(writerVerticle);

        readerVerticle = new ReaderVerticle(documentDatabase);
        vertx.deployVerticle(readerVerticle);

        // register app specific database service
        proxy = ProxyHelper.registerService(DocumentDatabaseService.class, vertx,
            new DocumentDatabaseServiceImpl(documentDatabase), "test");

        vertx.deployVerticle(ServiceReaderVerticle.class.getName());
        vertx.deployVerticle(ServiceWriterVerticle.class.getName());

        handler.handle(Future.succeededFuture());
      }
    });
  }

  private ConnectionOptions selectConnectionInfo() {
    String protocol = config().getString("protocol", "plocal");
    String name = Objects.requireNonNull(config().getString("name"));
    if (protocol.equals("plocal")) {
      return manager.plocalConnection(name).build();
    }
    else if (protocol.equals("memory")) {
      return manager.memoryConnection(name).build();
    }
    else if (protocol.equals("remote")) {
      String servername = name + "_server";
      manager.createDocumentInstance(
          manager.plocalConnection(servername).build(),
          db -> db.result().getMetadata().getSchema().createClass("test")
      );
      return manager.remoteConnection(name, "localhost", servername).build();
    }
    else {
      throw new IllegalArgumentException("Unknown protocol: " + protocol);
    }
  }
}
