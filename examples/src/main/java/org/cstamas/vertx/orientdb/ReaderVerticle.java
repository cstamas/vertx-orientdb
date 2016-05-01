package org.cstamas.vertx.orientdb;

import java.util.ArrayList;

import com.orientechnologies.orient.core.record.impl.ODocument;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
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

  public ReaderVerticle(final Database database) {
    this.database = database;
  }

  @Override
  public void start(final Future<Void> startFuture) throws Exception {
    vertx.eventBus().consumer("read",
        (Message<JsonObject> m) -> {
          database.select(
              new ResultHandler<ODocument>()
              {
                ArrayList<String> arrayList = new ArrayList<>();

                @Override
                public void handle(final ODocument doc) {
                  arrayList.add(doc.field("name"));
                }

                @Override
                public void failure(final Throwable t) {
                  t.printStackTrace();
                }

                @Override
                public void end() {
                  log.info("List size=" + arrayList.size());
                }
              },
              "select from test",
              null
          );
        }
    );
    super.start(startFuture);
  }
}
