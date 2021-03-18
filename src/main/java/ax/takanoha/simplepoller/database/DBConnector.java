package ax.takanoha.simplepoller.database;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.UpdateResult;

public class DBConnector {

  private final String DB_PATH = "poller.db";
  public static final String CONFIG_JDBC_URL = "pollerdb.jdbc.url";
  public static final String CONFIG_JDBC_DRIVER_CLASS = "pollerdb.jdbc.driver";
  public static final String CONFIG_JDBC_MAX_POOL_SIZE = "pollerdb.jdbc.pool";

  private final SQLClient client;

  public DBConnector(Vertx vertx, JsonObject config) {
    String url = config.getString(CONFIG_JDBC_URL, "jdbc:sqlite:" + DB_PATH);
    String driver = config.getString(CONFIG_JDBC_DRIVER_CLASS, "org.sqlite.JDBC");
    int pool = config.getInteger(CONFIG_JDBC_MAX_POOL_SIZE, 30);

    client = JDBCClient.createShared(vertx, new JsonObject()
      .put("url", url)
      .put("driver_class", driver)
      .put("max_pool_size", pool));
  }

  public Future<ResultSet> query(String query) {
    return query(query, new JsonArray());
  }


  public Future<ResultSet> query(String query, JsonArray params) {
    if(query == null || query.isEmpty()) {
      return Future.failedFuture("Query is null or empty");
    }
    if(!query.endsWith(";")) {
      query = query + ";";
    }

    Future<ResultSet> queryResultFuture = Future.future();

    client.queryWithParams(query, params, result -> {
      if(result.failed()){
        queryResultFuture.fail(result.cause());
      } else {
        queryResultFuture.complete(result.result());
      }
    });
    return queryResultFuture;
  }

  public Future<UpdateResult> update(String query, JsonArray params) {
    if(query == null || query.isEmpty()) {
      return Future.failedFuture("Query is null or empty");
    }
    if(!query.endsWith(";")) {
      query = query + ";";
    }

    Future<UpdateResult> updateResultFuture = Future.future();

    client.updateWithParams(query, params, result -> {
      if(result.failed()){
        updateResultFuture.fail(result.cause());
      } else {
        updateResultFuture.complete(result.result());
      }
    });
    return updateResultFuture;
  }
}
