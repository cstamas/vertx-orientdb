package org.cstamas.vertx.orientdb;

import java.util.List;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * OrientDB service, registered whenever a {@link Database} is created, unregistered when closed.
 */
@ProxyGen
public interface DatabaseService
{
  static DatabaseService createProxy(Vertx vertx, String address) {
    return new DatabaseServiceVertxEBProxy(vertx, address);
  }

  @Fluent
  DatabaseService insert(String clazz, JsonObject document, Handler<AsyncResult<String>> handler);

  @Fluent
  DatabaseService delete(String clazz, String where, Handler<AsyncResult<Void>> handler);

  @Fluent
  DatabaseService select(String clazz, String where, Handler<AsyncResult<List<JsonObject>>> handler);
}
