package org.cstamas.vertx.orientdb;

import java.util.List;
import java.util.Map;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * OrientDB service, registered whenever a {@link GraphDatabase} documentInstance is created using {@link
 * Manager#graphInstance(ConnectionOptions, Handler, Handler)} method, unregistered when documentInstance closed.
 */
@ProxyGen
public interface GraphDatabaseService
{
  static GraphDatabaseService createProxy(Vertx vertx, String address) {
    return new GraphDatabaseServiceVertxEBProxy(vertx, address);
  }

  @Fluent
  GraphDatabaseService gremlinScript(Map<String, String> params, String script, Handler<AsyncResult<List<String>>> handler);
}