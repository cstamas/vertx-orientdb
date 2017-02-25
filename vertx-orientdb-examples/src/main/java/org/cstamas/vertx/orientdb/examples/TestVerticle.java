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
    manager.open(opened -> {
      if (opened.failed()) {
        startFuture.fail(opened.cause());
      }
      else {
        Future<Void> future = Future.future();
        future.setHandler(vv -> {
          if (vv.failed()) {
            log.info("Deploy failed", vv.cause());
            startFuture.fail(vv.cause());
          }
          else {
            log.info("Starting periodics");
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
            startFuture.complete();
          }
        });
        createDocumentDatabase(future.completer());
      }
    });
  }

  @Override
  public void stop(final Future<Void> stopFuture) throws Exception {
    manager.close(v -> {
      if (v.failed()) {
        stopFuture.fail(v.cause());
      }
      else {
        stopFuture.complete();
      }
    });
  }

  private void createDocumentDatabase(final Handler<AsyncResult<Void>> handler) {
    selectConnectionInfo(co -> {
      if (co.failed()) {
        handler.handle(Future.failedFuture(co.cause()));
      }
      else {
        ConnectionOptions connectionOptions = co.result();
        manager.createDocumentInstance(
            connectionOptions,
            adb -> {
              log.info("Schema update");
              OSchema schema = adb.getMetadata().getSchema();
              if (!schema.existsClass("test")) {
                OClass oclass = schema.createClass("test");
                oclass.createProperty("name", OType.STRING);
                oclass.createProperty("value", OType.STRING);
              }
              log.info("Schema done");
            },
            v -> {
              if (v.failed()) {
                log.info("Test subject DB creation failed", v.cause());
                handler.handle(Future.failedFuture(v.cause()));
              }
              else {
                log.info("Test subject DB creation done");
                deployVerticles(connectionOptions, handler);
              }
            }
        );
      }
    });
  }

  private void deployVerticles(final ConnectionOptions connectionOptions, final Handler<AsyncResult<Void>> handler) {
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

        log.info("Verticles deployed?");
        handler.handle(Future.succeededFuture());
      }
    });
  }

  private void selectConnectionInfo(final Handler<AsyncResult<ConnectionOptions>> handler) {
    String protocol = config().getString("protocol", "plocal");
    String name = Objects.requireNonNull(config().getString("name"));
    if (protocol.equals("plocal")) {
      handler.handle(Future.succeededFuture(manager.plocalConnection(name).build()));
    }
    else if (protocol.equals("memory")) {
      handler.handle(Future.succeededFuture(manager.memoryConnection(name).build()));
    }
    else if (protocol.equals("remote")) {
      String servername = name + "_server";
      manager.createDocumentInstance(
          manager.plocalConnection(servername).build(),
          db -> db.getMetadata().getSchema().createClass("test"),
          v -> {
            if (v.failed()) {
              log.info("Start of REMOTE target: {} failed", servername, v.cause());
              handler.handle(Future.failedFuture(v.cause()));
            }
            else {
              log.info("Started REMOTE target: {}", servername);
              handler.handle(Future.succeededFuture(manager.remoteConnection(name, "localhost", servername).build()));
            }
          }
      );
    }
    else {
      handler.handle(Future.failedFuture(new IllegalArgumentException("Unknown protocol: " + protocol)));
    }
  }
}
