package org.cstamas.vertx.orientdb;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * OrientDB test verticle.
 */
public class ServiceWriterVerticle
    extends AbstractVerticle
{
  private static final Logger log = LoggerFactory.getLogger(ServiceWriterVerticle.class);

  @Override
  public void start(final Future<Void> startFuture) throws Exception {
    DocumentDatabaseService documentDatabaseService = DocumentDatabaseService.createProxy(vertx, "test");
    vertx.eventBus().consumer("write",
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
