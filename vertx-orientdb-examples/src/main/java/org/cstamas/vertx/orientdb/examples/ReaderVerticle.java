package org.cstamas.vertx.orientdb.examples;

import java.util.List;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.cstamas.vertx.orientdb.DocumentDatabase;

/**
 * OrientDB test verticle.
 */
public class ReaderVerticle
    extends AbstractVerticle
{
  private static final Logger log = LoggerFactory.getLogger(ReaderVerticle.class);

  private final DocumentDatabase documentDatabase;

  private MessageConsumer<JsonObject> consumer;

  public ReaderVerticle(final DocumentDatabase documentDatabase) {
    this.documentDatabase = documentDatabase;
  }

  @Override
  public void start(final Future<Void> startFuture) throws Exception {
    consumer = vertx.eventBus().consumer("read",
        (Message<JsonObject> m) -> {
          documentDatabase.exec(adb -> {
            if (adb.failed()) {
              log.warn("DB failure", adb.cause());
            }
            else {
              ODatabaseDocumentTx db = adb.result();
              List<ODocument> res = db.query(new OSQLSynchQuery<ODocument>("select count(*) as count from test"));
              log.info("List size=" + res.get(0).field("count"));
            }
          });
        }
    );
    super.start(startFuture);
  }
}
