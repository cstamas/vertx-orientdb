package org.cstamas.vertx.orientdb;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * OrientDB test verticle.
 */
public class WriterVerticle
    extends AbstractVerticle
{
  private static final Logger log = LoggerFactory.getLogger(WriterVerticle.class);

  private final Database database;

  private MessageConsumer<JsonObject> writer;

  public WriterVerticle(final Database database) {
    this.database = database;
  }

  @Override
  public void start(final Future<Void> startFuture) throws Exception {
    writer = vertx.eventBus().consumer("write",
        (Message<JsonObject> m) -> {
          database.exec(adb -> {
                if (adb.succeeded()) {
                  ODatabaseDocumentTx db = adb.result();
                  JsonObject message = m.body();
                  db.begin();
                  ODocument doc = new ODocument("test");
                  doc.field("name", message.getValue("name"));
                  doc.field("value", message.getValue("value"));
                  db.save(doc);
                  db.commit();
                  log.info("Written " + message);
                }
                else {
                  log.error("Error", adb.cause());
                }
              }
          );
        }
    );
    super.start(startFuture);
  }

  @Override
  public void stop(final Future<Void> stopFuture) throws Exception {
    log.info("Stop writer");
    writer.unregister();
    super.stop(stopFuture);
  }
}
