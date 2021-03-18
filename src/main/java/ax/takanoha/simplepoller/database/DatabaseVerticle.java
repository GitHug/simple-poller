package ax.takanoha.simplepoller.database;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import ax.takanoha.simplepoller.migrate.DBMigration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class DatabaseVerticle extends AbstractVerticle {
    public static final String CONFIG_SQL_QUERIES_RESOURCE_FILE = "pollerdb.sqlqueries.resource.file";
    public static final String CONFIG_POLLERDB_QUEUE = "pollerdb.queue";

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        Map<SqlQuery, String> queries = loadSqlQueries();
        DBConnector connector = new DBConnector(vertx, config());

        DBMigration migration = new DBMigration();
        migration.migrate(connector, queries.get(SqlQuery.CREATE_SERVICE_TABLE)).setHandler(promise -> {
           if (promise.succeeded()) {

               vertx.eventBus().consumer(CONFIG_POLLERDB_QUEUE, DatabaseService.create(connector, queries)::onMessage);
               startFuture.complete();
           } else {
               System.err.println("Failed to migrate");
               startFuture.fail(promise.cause());
           }
        });
    }

    private Map<SqlQuery, String> loadSqlQueries() throws IOException {
        Map<SqlQuery, String> queries = new HashMap<>();

        String queriesFile = config().getString(CONFIG_SQL_QUERIES_RESOURCE_FILE);

        InputStream queriesInputStream;
        if (queriesFile != null) {
            queriesInputStream = new FileInputStream(queriesFile);
        } else {
            queriesInputStream = getClass().getResourceAsStream("/db-queries.properties");
        }

        Properties queriesProps = new Properties();
        queriesProps.load(queriesInputStream);
        queriesInputStream.close();

        queries.put(SqlQuery.CREATE_SERVICE_TABLE, queriesProps.getProperty(SqlQuery.CREATE_SERVICE_TABLE.name));
        queries.put(SqlQuery.FETCH_SERVICES, queriesProps.getProperty(SqlQuery.FETCH_SERVICES.name));
        queries.put(SqlQuery.FETCH_SERVICE, queriesProps.getProperty(SqlQuery.FETCH_SERVICE.name));
        queries.put(SqlQuery.CREATE_SERVICE, queriesProps.getProperty(SqlQuery.CREATE_SERVICE.name));
        queries.put(SqlQuery.DELETE_SERVICE, queriesProps.getProperty(SqlQuery.DELETE_SERVICE.name));
        queries.put(SqlQuery.UPDATE_SERVICE_STATUS, queriesProps.getProperty(SqlQuery.UPDATE_SERVICE_STATUS.name));

        return queries;
    }
}
