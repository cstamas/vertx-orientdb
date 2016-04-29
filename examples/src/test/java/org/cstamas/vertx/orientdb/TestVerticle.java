package org.cstamas.vertx.orientdb;

import java.util.HashSet;

import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OrientDB test verticle.
 */
public class TestVerticle
    extends AbstractVerticle
{
  private static final Logger log = LoggerFactory.getLogger(TestVerticle.class);

  private ManagerVerticle managerVerticle = new ManagerVerticle();

  private ReaderVerticle readerVerticle;

  private WriterVerticle writerVerticle;

  private HashSet<String> deploymentIds = new HashSet<>();

  @Override
  public void start(final Future<Void> startFuture) throws Exception {
    managerVerticle = new ManagerVerticle();
    vertx.deployVerticle(managerVerticle, new DeploymentOptions().setConfig(config()),
        c -> {
          if (c.succeeded()) {
            deploymentIds.add(c.result());
            Manager manager = managerVerticle.getManager();
            manager.instance(
                "test",
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
                    vertx.deployVerticle(writerVerticle, wd -> deploymentIds.add(wd.result()));

                    readerVerticle = new ReaderVerticle(instance.result());
                    vertx.deployVerticle(readerVerticle, rd -> deploymentIds.add(rd.result()));

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

  @Override
  public void stop(final Future<Void> stopFuture) throws Exception {
    super.stop(stopFuture);
  }
}
