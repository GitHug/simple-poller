package ax.takanoha.simplepoller.migrate;

import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ax.takanoha.simplepoller.database.DBConnector;

public class DBMigration {
  private static final Logger LOGGER = LoggerFactory.getLogger(DBMigration.class);

  public Future<Void> migrate(DBConnector connector, String query) {
    Future<Void> promise = Future.future();
    connector.query(query).setHandler(done -> {
      if (done.succeeded()) {
        LOGGER.info("Completed DB migrations");
        promise.complete();
      } else {
        LOGGER.error("Failed to run DB migrations", done.cause());
        promise.fail(done.cause());
      }
    });

    return promise;
  }
}
