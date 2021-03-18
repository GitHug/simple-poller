package ax.takanoha.simplepoller.database;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

import java.util.Map;

public interface DatabaseService {
    static DatabaseService create(DBConnector connector, Map<SqlQuery, String> queries) {
        return new DatabaseServiceImpl(connector, queries);
    }

    void onMessage(Message<JsonObject> message);
}
