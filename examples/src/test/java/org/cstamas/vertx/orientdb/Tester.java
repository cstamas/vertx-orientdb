package org.cstamas.vertx.orientdb;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * Created by cstamas on 25/04/16.
 */
public class Tester
{
  static {
    System.setProperty("vertx.logger-delegate-factory-class-name", SLF4JLogDelegateFactory.class.getName());
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }

  public static void main(String[] a) throws Exception {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(TestVerticle.class.getName(),
        new DeploymentOptions().setConfig(new JsonObject("{\"serverEnabled\":true}")));
    Thread.sleep(10000);

    vertx.close();
  }
}
