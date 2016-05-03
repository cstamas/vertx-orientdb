package org.cstamas.vertx.orientdb;

import java.util.List;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * OrientDB service, registered whenever a {@link DocumentDatabase} documentInstance is created using {@link
 * Manager#documentInstance(ConnectionOptions, Handler, Handler)} method, unregistered when documentInstance closed.
 */
@ProxyGen
public interface DocumentDatabaseService
{
  static DocumentDatabaseService createProxy(Vertx vertx, String address) {
    return new DocumentDatabaseServiceVertxEBProxy(vertx, address);
  }

  @Fluent
  DocumentDatabaseService insert(String clazz, JsonObject document, Handler<AsyncResult<String>> handler);

  @Fluent
  DocumentDatabaseService delete(String clazz, String where, Handler<AsyncResult<Void>> handler);

  @Fluent
  DocumentDatabaseService select(String clazz, String where, Handler<AsyncResult<List<JsonObject>>> handler);
}
