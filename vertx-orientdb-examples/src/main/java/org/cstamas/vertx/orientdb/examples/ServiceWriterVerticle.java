package org.cstamas.vertx.orientdb.examples;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.cstamas.vertx.orientdb.examples.service.DocumentDatabaseService;

/**
 * OrientDB test verticle.
 */
public class ServiceWriterVerticle
    extends AbstractVerticle
{
  private static final Logger log = LoggerFactory.getLogger(ServiceWriterVerticle.class);

  private MessageConsumer<JsonObject> consumer;

  @Override
  public void start(final Future<Void> startFuture) throws Exception {
    DocumentDatabaseService documentDatabaseService = DocumentDatabaseService.createProxy(vertx, "test");
    consumer = vertx.eventBus().consumer("write",
        (Message<JsonObject> m) -> {
          documentDatabaseService.insert(
              "test",
              new JsonObject().put("name", "name").put("value", "value"),
              h -> {
                if (h.succeeded()) {
                  log.info("Written OID " + h.result());
                }
                else {
                  log.error("Error", h.cause());
                }
              }
          );
        }
    );
    super.start(startFuture);
  }
}
