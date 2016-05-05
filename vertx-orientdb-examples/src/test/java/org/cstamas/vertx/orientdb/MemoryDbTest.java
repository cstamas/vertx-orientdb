package org.cstamas.vertx.orientdb;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import org.junit.Test;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * Junit test.
 */
public class MemoryDbTest
{
  static {
    System.setProperty("vertx.logger-delegate-factory-class-name", SLF4JLogDelegateFactory.class.getName());
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }

  @Test
  public void memory() throws Exception {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(
        TestVerticle.class.getName(),
        new DeploymentOptions().setConfig(
            new JsonObject().put("serverEnabled", true).put("orientHome", "target/withServer").put("protocol", "memory")
        )
    );
    Thread.sleep(10000);
    vertx.close();
  }
}
