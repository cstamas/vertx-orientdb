package org.cstamas.vertx.orientdb;

import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.cstamas.vertx.orientdb.Manager.ConnectionInfo;

/**
 * OrientDB test verticle.
 */
public class TestVerticle
    extends AbstractVerticle
{
  private ManagerVerticle managerVerticle = new ManagerVerticle();

  private ReaderVerticle readerVerticle;

  private WriterVerticle writerVerticle;

  @Override
  public void start(final Future<Void> startFuture) throws Exception {
    managerVerticle = new ManagerVerticle();

    vertx.deployVerticle(managerVerticle, new DeploymentOptions().setConfig(config()),
        c -> {
          if (c.succeeded()) {
            final Manager manager = managerVerticle.getManager();
            manager.documentInstance(
                selectConnectionInfo(manager),
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
                    writerVerticle = new WriterVerticle(instance.result());
                    vertx.deployVerticle(writerVerticle);

                    readerVerticle = new ReaderVerticle(instance.result());
                    vertx.deployVerticle(readerVerticle);

                    vertx.deployVerticle(ServiceReaderVerticle.class.getName());
                    vertx.deployVerticle(ServiceWriterVerticle.class.getName());

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
                  }
                  else {
                    instance.cause().printStackTrace();
                  }
                }
            );
          }
          else {
            c.cause().printStackTrace();
          }
        }
    );
    super.start(startFuture);
  }

  private ConnectionInfo selectConnectionInfo(final Manager manager) {
    String protocol = config().getString("protocol", "plocal");
    if (protocol.equals("plocal")) {
      return manager.plocalConnection("test");
    }
    else if (protocol.equals("memory")) {
      return manager.memoryConnection("test");
    }
    else if (protocol.equals("remote")) {
      manager.documentInstance(manager.plocalConnection(
          "local_test"),
          db -> db.getMetadata().getSchema().createClass("test"),
          null
      );
      return manager.remoteConnection("test", "localhost", "local_test", "admin", "admin");
    }
    else {
      throw new IllegalArgumentException("Unknown protocol: " + protocol);
    }
  }
}
