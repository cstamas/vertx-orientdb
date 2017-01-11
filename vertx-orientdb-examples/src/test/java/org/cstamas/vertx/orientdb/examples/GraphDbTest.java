package org.cstamas.vertx.orientdb.examples;

import java.util.ArrayList;
import java.util.List;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
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
    ConnectionOptions conn = manager.memoryConnection("test").setUsernamePassword("admin", "admin").build();
    Async creation = context.async();
    manager.graphInstance(
        conn,
        notx -> {
          notx.createKeyIndex("name", Vertex.class);
          notx.createEdgeType("related");
        },
        txr -> {
          if (txr.failed()) {
            context.fail(txr.cause());
            creation.complete();
            return;
          }
          GraphDatabase gd = txr.result();

          gd.exec(og -> {
            if (og.failed()) {
              context.fail(og.cause());
              return;
            }
            OrientGraph graph = og.result();
            Vertex v1 = graph.addVertex(null);
            v1.setProperty("name", "vertex1");
            Vertex v2 = graph.addVertex(null);
            v2.setProperty("name", "vertex2");
            graph.addEdge(null, v1, v2, "related");
            graph.commit();
            creation.complete();
          });
        }
    );
    creation.await();

    Async select = context.async();
    manager.graphInstance(conn, null, txr -> {
      if (txr.failed()) {
        context.fail(txr.cause());
        select.complete();
        return;
      }
      GraphDatabase gd = txr.result();
      gd.exec(og -> {
        if (og.failed()) {
          context.fail(og.cause());
          return;
        }
        OrientGraph graph = og.result();
        List<Vertex> result = new ArrayList<>();
        Iterable<Vertex> vs = graph.getVertices("name", "vertex1");
        vs.forEach(result::add);
        context.assertEquals(result.size(), 1);
        select.complete();
      });
    });
    select.await();
  }
}
