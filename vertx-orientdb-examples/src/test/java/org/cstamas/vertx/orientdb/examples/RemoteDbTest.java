package org.cstamas.vertx.orientdb.examples;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import org.junit.Test;

/**
 * Junit test.
 */
public class RemoteDbTest
    extends TestSupport
{
  @Test
  public void remote(TestContext context) throws Exception {
    vertx.deployVerticle(
        TestVerticle.class.getName(),
        new DeploymentOptions().setConfig(
            new JsonObject()
                .put("serverEnabled", true)
                .put("orientHome", "target/withServer")
                .put("protocol", "remote")
                .put("name", testName.getMethodName())
        )
    );
    Thread.sleep(5000);
  }
}
