package org.cstamas.vertx.orientdb;

import java.util.ArrayList;

import com.orientechnologies.orient.core.record.impl.ODocument;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.cstamas.vertx.orientdb.Database.ResultHandler;

/**
 * OrientDB test verticle.
 */
public class ReaderVerticle
    extends AbstractVerticle
{
  private static final Logger log = LoggerFactory.getLogger(ReaderVerticle.class);

  private final Database database;

  private MessageConsumer<JsonObject> reader;

  public ReaderVerticle(final Database database) {
    this.database = database;
  }

  @Override
  public void start(final Future<Void> startFuture) throws Exception {
    reader = vertx.eventBus().consumer("read",
        (Message<JsonObject> m) -> {
          database.select(
              new ResultHandler<ODocument>()
              {
                ArrayList<String> arrayList = new ArrayList<>();

                @Override
                public void failure(final Throwable t) {
                  t.printStackTrace();
                }

                @Override
                public void end() {
                  log.info("List size=" + arrayList.size());
                }

                @Override
                public void handle(final ODocument doc) {
                  arrayList.add(doc.field("name"));
                }
              },
              "select from test",
              null
          );
          //database.exec(adb -> {
          //      if (adb.succeeded()) {
          //        ODatabaseDocumentTx db = adb.result();
          //        OSQLSynchQuery<Integer> q = new OSQLSynchQuery<>("select count(*) as count from test");
          //        List<ODocument> list = db.query(q);
          //        long count = list.get(0).field("count");
          //        System.out.println("Reader: count=" + count);
          //      }
          //      else {
          //        adb.cause().printStackTrace();
          //      }
          //    }
          //);
        }
    );
    super.start(startFuture);
  }

  @Override
  public void stop(final Future<Void> stopFuture) throws Exception {
    log.info("Stop reader");
    reader.unregister();
    super.stop(stopFuture);
  }
}
