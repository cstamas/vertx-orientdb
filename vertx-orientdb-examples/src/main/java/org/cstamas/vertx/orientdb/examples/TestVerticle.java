package org.cstamas.vertx.orientdb.examples;

import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
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
  private Manager manager;

  private ReaderVerticle readerVerticle;

  private WriterVerticle writerVerticle;

  @Override
  public void start(final Future<Void> startFuture) throws Exception {
    this.manager = Manager.create(vertx, ManagerOptions.fromJsonObject(config()));

    manager.documentInstance(
        selectConnectionInfo(),
        db -> {
          OSchema schema = db.getMetadata().getSchema();
          if (!schema.existsClass("test")) {
            OClass oclass = schema.createClass("test");
            oclass.createProperty("name", OType.STRING);
            oclass.createProperty("value", OType.STRING);
          }
        },
        instance -> {
          if (instance.succeeded()) {
            DocumentDatabase documentDatabase = instance.result();
            writerVerticle = new WriterVerticle(documentDatabase);
            vertx.deployVerticle(writerVerticle);

            readerVerticle = new ReaderVerticle(documentDatabase);
            vertx.deployVerticle(readerVerticle);

            // register app specific database service
            ProxyHelper.registerService(DocumentDatabaseService.class, vertx,
                new DocumentDatabaseServiceImpl(documentDatabase), "test");

            vertx.deployVerticle(ServiceReaderVerticle.class.getName());
            vertx.deployVerticle(ServiceWriterVerticle.class.getName());

            // fire events that cause READ and WRITE operations
            vertx.setPeriodic(500,
                t -> {
                  vertx.eventBus().publish("read", new JsonObject().put("name", "foo").put("value", t));
                }
            );
            vertx.setPeriodic(500,
                t -> {
                  vertx.eventBus().publish("write", new JsonObject().put("name", "foo").put("value", t));
                }
            );
            startFuture.complete();
          }
          else {
            startFuture.fail(instance.cause());
          }
        }
    );
  }

  private ConnectionOptions selectConnectionInfo() {
    String protocol = config().getString("protocol", "plocal");
    if (protocol.equals("plocal")) {
      return manager.plocalConnection("test").build();
    }
    else if (protocol.equals("memory")) {
      return manager.memoryConnection("test").build();
    }
    else if (protocol.equals("remote")) {
      manager.documentInstance(
          manager.plocalConnection("local_test").build(),
          db -> db.getMetadata().getSchema().createClass("test"),
          null
      );
      return manager.remoteConnection("test", "localhost", "local_test").build();
    }
    else {
      throw new IllegalArgumentException("Unknown protocol: " + protocol);
    }
  }
}
