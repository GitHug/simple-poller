package ax.takanoha.simplepoller.database;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.UpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ax.takanoha.simplepoller.Actions;
import ax.takanoha.simplepoller.ErrorCodes;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DatabaseServiceImpl implements DatabaseService {
    private final DBConnector connector;
    private final Map<SqlQuery, String> queries;

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseServiceImpl.class);

    public DatabaseServiceImpl(DBConnector connector, Map<SqlQuery, String> queries) {
        this.connector = connector;
        this.queries = queries;
    }

    public void onMessage(Message<JsonObject> message) {
        LOGGER.info("Message inbound: " + message.body().encodePrettily());
        if (!message.headers().contains("action")) {
            LOGGER.error("No Action in header");
            message.fail(ErrorCodes.NO_ACTION_SPECIFIED.ordinal(), "No Action in header");
            return;
        }

        String actionString = message.headers().get("action");
        Actions action = Actions.fromString(actionString);

        switch (action) {
            case FETCH_SERVICES:
                fetchServices(message);
                break;
            case FETCH_SERVICE:
                fetchService(message);
                break;
            case CREATE_SERVICE:
                createService(message);
                break;
            case DELETE_SERVICE:
                deleteService(message);
                break;
            case STORE_POLLING_RESULT:
                updateServiceStatus(message);
                break;
            default:
                LOGGER.error("Unknown action " + actionString);
                message.fail(ErrorCodes.UNKNOWN_ACTION.ordinal(), "Unknown action " + actionString);
        }
    }

    private void fetchServices(Message<JsonObject> message) {
        connector.query(queries.get(SqlQuery.FETCH_SERVICES)).setHandler(res -> {
            if (res.succeeded()) {
                JsonArray reply = new JsonArray();
                res.result().getRows().forEach(row -> {
                    reply.add(row);
                });

                message.reply(new JsonObject().put("services", reply));
            } else {
                message.fail(ErrorCodes.DB_ERROR.ordinal(), res.cause().getMessage());
            }
        });
    }

    private void createService(Message<JsonObject> message) {
        JsonObject request = message.body();
        JsonArray params = new JsonArray().add(request.getString("url"));
        if (request.containsKey("name")) {
            params.add(request.getString("name"));
        }

        connector.update(queries.get(SqlQuery.CREATE_SERVICE), params).setHandler(res -> {
            if (res.succeeded()) {
                UpdateResult result = res.result();
                int id = result.getKeys().getInteger(0);
                fetchService(id, message);
            } else {
                message.fail(ErrorCodes.DB_ERROR.ordinal(), "Failed to create service");
            }
        });
    }

    private void fetchService(Message<JsonObject> message) {
        JsonObject request = message.body();
        int id = request.getInteger("id");
        fetchService(id, message);
    }

    private void fetchService(int id, Message<JsonObject> message) {
        connector.query(queries.get(SqlQuery.FETCH_SERVICE), new JsonArray().add(id)).setHandler(res -> {
            if (res.succeeded()) {
                List<JsonObject> reply = res.result().getRows();
                if (reply.isEmpty()) {
                    reply = Arrays.asList(new JsonObject().put("found", false));
                }

                message.reply(new JsonObject().put("service", reply.get(0)));
            } else {
                res.cause().printStackTrace();
                message.fail(ErrorCodes.DB_ERROR.ordinal(), "Failed to find service with id " + id);
            }
        });
    }

    private void deleteService(Message<JsonObject> message) {
        JsonObject request = message.body();
        int id = request.getInteger("id");

        JsonArray params = new JsonArray().add(id);
        connector.update(queries.get(SqlQuery.DELETE_SERVICE), params).setHandler(res -> {
            if (res.succeeded()) {
                message.reply("ok");
            } else {
                res.cause().printStackTrace();
                message.fail(ErrorCodes.DB_ERROR.ordinal(), "Failed to delete service with id " + id);
            }
        });
    }

    private void updateServiceStatus(Message<JsonObject> message) {
        JsonObject request = message.body();
        int id = request.getInteger("id");
        String status = request.getString("status");

        JsonArray params = new JsonArray().add(status).add(id);

        connector.update(queries.get(SqlQuery.UPDATE_SERVICE_STATUS), params).setHandler(res -> {
            if (res.succeeded()) {
                message.reply("ok");
            } else {
                res.cause().printStackTrace();
                message.fail(ErrorCodes.DB_ERROR.ordinal(), "Failed to update status for service with id " + id);
            }
        });
    }
}
