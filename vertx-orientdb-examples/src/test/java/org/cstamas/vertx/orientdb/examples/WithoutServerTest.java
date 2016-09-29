package org.cstamas.vertx.orientdb.examples;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import org.junit.Test;

/**
 * Junit test.
 */
public class WithoutServerTest
    extends TestSupport
{
  @Test
  public void withoutServer(TestContext context) throws Exception {
    vertx.deployVerticle(
        TestVerticle.class.getName(),
        new DeploymentOptions().setConfig(
            new JsonObject()
                .put("serverEnabled", false)
                .put("orientHome", "target/withoutServer")
                .put("name", testName.getMethodName())
        )
    );
    Thread.sleep(5000);
  }
}
