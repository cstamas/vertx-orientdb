package org.cstamas.vertx.orientdb;

import java.util.ArrayList;

import com.orientechnologies.orient.core.record.impl.ODocument;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.cstamas.vertx.orientdb.DocumentDatabase.ResultHandler;

/**
 * OrientDB test verticle.
 */
public class ReaderVerticle
    extends AbstractVerticle
{
  private static final Logger log = LoggerFactory.getLogger(ReaderVerticle.class);

  private final DocumentDatabase documentDatabase;

  public ReaderVerticle(final DocumentDatabase documentDatabase) {
    this.documentDatabase = documentDatabase;
  }

  @Override
  public void start(final Future<Void> startFuture) throws Exception {
    vertx.eventBus().consumer("read",
        (Message<JsonObject> m) -> {
          documentDatabase.select(
              new ResultHandler<ODocument>()
              {
                ArrayList<String> arrayList = new ArrayList<>();

                @Override
                public boolean handle(final ODocument doc) {
                  arrayList.add(doc.field("name"));
                  return true;
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
