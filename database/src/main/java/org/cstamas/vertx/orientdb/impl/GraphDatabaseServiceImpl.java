package org.cstamas.vertx.orientdb.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;

import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.gremlin.groovy.jsr223.GremlinGroovyScriptEngine;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import org.cstamas.vertx.orientdb.GraphDatabase;
import org.cstamas.vertx.orientdb.GraphDatabaseService;

/**
 * Default implementation.
 */
public class GraphDatabaseServiceImpl
    implements GraphDatabaseService
{
  private final GraphDatabase graphDatabase;

  public GraphDatabaseServiceImpl(final GraphDatabase graphDatabase) {
    this.graphDatabase = graphDatabase;
  }

  @Override
  public GraphDatabaseService gremlinScript(final Map<String, String> params,
                                            final String script,
                                            final Handler<AsyncResult<List<String>>> handler)
  {
    final GremlinGroovyScriptEngine scriptEngine = new GremlinGroovyScriptEngine();
    graphDatabase.exec(agr -> {
      Future<List<String>> future;
      if (agr.succeeded()) {
        OrientGraph g = agr.result();
        List<String> result = new ArrayList<>();
        Bindings bindings = scriptEngine.createBindings();
        bindings.put("g", g);
        bindings.put("result", result);
        params.entrySet().forEach(e -> bindings.put(e.getKey(), e.getValue()));
        try {
          scriptEngine.eval(script, bindings);
          future = Future.succeededFuture(result);
        }
        catch (Exception e) {
          future = Future.failedFuture(e);
        }
      }
      else {
        future = Future.failedFuture(agr.cause());
      }
      handler.handle(future);
    });
    return this;
  }
}
