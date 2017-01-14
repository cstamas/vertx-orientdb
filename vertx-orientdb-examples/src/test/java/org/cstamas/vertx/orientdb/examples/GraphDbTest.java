package org.cstamas.vertx.orientdb.examples;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.cstamas.vertx.orientdb.ConnectionOptions;
import org.cstamas.vertx.orientdb.GraphDatabase;
import org.cstamas.vertx.orientdb.Manager;
import org.cstamas.vertx.orientdb.ManagerOptions;
import org.junit.Test;

public class GraphDbTest
    extends TestSupport
{
  @Test
  public void graphCreation(final TestContext context) {
    //vertx.exceptionHandler(context.exceptionHandler());
    Manager manager = Manager.create(vertx, ManagerOptions.fromJsonObject(
        new JsonObject()
            .put("serverEnabled", false)
            .put("orientHome", "target/withoutServer")
            .put("protocol", "plocal")
            .put("name", testName.getMethodName())
    ));
    Async async = context.async();
    manager.open(v -> {
      ConnectionOptions conn = manager.memoryConnection("test").setUsernamePassword("admin", "admin").build();
      manager.createGraphInstance(conn, anotx -> {
        if (anotx.failed()) {
          context.fail(anotx.cause());
        }
        else {
          OrientGraphNoTx notx = anotx.result();
          notx.createKeyIndex("name", Vertex.class);
          notx.createEdgeType("related");
          manager.graphInstance(conn.name(), adb -> {
            if (adb.failed()) {
              context.fail(adb.cause());
            }
            else {
              GraphDatabase db = adb.result();
              db.exec(tx -> {
                if (tx.failed()) {
                  context.fail(tx.cause());
                }
                else {
                  OrientGraph graph = tx.result();
                  Vertex v1 = graph.addVertex(null);
                  v1.setProperty("name", "vertex1");
                  Vertex v2 = graph.addVertex(null);
                  v2.setProperty("name", "vertex2");
                  graph.addEdge(null, v1, v2, "related");
                  graph.commit();
                  async.complete();
                }
              });
            }
          });
        }
      });
    });
    async.await();
  }
}
