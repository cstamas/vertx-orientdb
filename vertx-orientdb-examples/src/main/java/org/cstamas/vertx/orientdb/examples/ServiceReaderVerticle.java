package org.cstamas.vertx.orientdb.examples;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.serviceproxy.ProxyHelper;
import org.cstamas.vertx.orientdb.examples.service.DocumentDatabaseService;

/**
 * OrientDB test verticle.
 */
public class ServiceReaderVerticle
    extends AbstractVerticle
{
  private static final Logger log = LoggerFactory.getLogger(ServiceReaderVerticle.class);

  private MessageConsumer<JsonObject> consumer;

  @Override
  public void start(final Future<Void> startFuture) throws Exception {
    DocumentDatabaseService documentDatabaseService = DocumentDatabaseService.createProxy(vertx, "test");
    consumer = vertx.eventBus().consumer("read",
        (Message<JsonObject> m) -> {
          documentDatabaseService.select("test", "1=1", ar -> {
                if (ar.succeeded()) {
                  log.info("List size=" + ar.result().size());
                }
                else {
                  log.error("Error", ar.cause());
                }
              }
          );
        }
    );
    super.start(startFuture);
  }
}
