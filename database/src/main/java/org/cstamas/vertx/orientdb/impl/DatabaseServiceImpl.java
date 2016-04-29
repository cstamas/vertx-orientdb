package org.cstamas.vertx.orientdb.impl;

import java.util.ArrayList;
import java.util.List;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import org.cstamas.vertx.orientdb.Database;
import org.cstamas.vertx.orientdb.DatabaseService;

/**
 * Default implementation.
 */
public class DatabaseServiceImpl
    implements DatabaseService
{
  private final Database database;

  public DatabaseServiceImpl(final Database database) {
    this.database = database;
  }

  @Override
  public DatabaseService insert(final String clazz,
                                final JsonObject document,
                                final Handler<AsyncResult<String>> handler)
  {
    database.exec(
        ah -> {
          if (ah.succeeded()) {
            ODatabaseDocumentTx db = ah.result();
            db.begin();
            ODocument doc = new ODocument(clazz);
            doc.fromJSON(document.toString());
            db.save(doc);
            db.commit();
            handler.handle(Future.succeededFuture(doc.getIdentity().toString()));
          }
          else {
            handler.handle(Future.failedFuture(ah.cause()));
          }
        }
    );
    return this;
  }

  @Override
  public DatabaseService delete(final String clazz, final String where, final Handler<AsyncResult<Void>> handler) {
    database.exec(
        ah -> {
          if (ah.succeeded()) {
            ODatabaseDocumentTx db = ah.result();
            db.begin();
            db.command(new OCommandSQL("delete from " + clazz + " where " + where)).execute();
            db.commit();
            handler.handle(Future.succeededFuture());
          }
          else {
            handler.handle(Future.failedFuture(ah.cause()));
          }
        }
    );
    return this;
  }

  @Override
  public DatabaseService select(final String clazz,
                                final String where,
                                final Handler<AsyncResult<List<JsonObject>>> handler)
  {
    database.exec(
        ah -> {
          if (ah.succeeded()) {
            ODatabaseDocumentTx db = ah.result();
            List<ODocument> result = db.query(new OSQLSynchQuery<>("select from " + clazz + " where " + where));
            ArrayList<JsonObject> jsonDocuments = new ArrayList<>(result.size());
            result.forEach(d -> jsonDocuments.add(new JsonObject(d.toJSON())));
            handler.handle(Future.succeededFuture(jsonDocuments));
          }
          else {
            handler.handle(Future.failedFuture(ah.cause()));
          }
        }
    );
    return this;
  }
}
