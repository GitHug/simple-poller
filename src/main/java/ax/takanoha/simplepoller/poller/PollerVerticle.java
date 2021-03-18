package ax.takanoha.simplepoller.poller;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ax.takanoha.simplepoller.Actions;

import java.util.ArrayList;
import java.util.List;

public class PollerVerticle extends AbstractVerticle {
    private BackgroundPoller poller;

    private static final Logger LOGGER = LoggerFactory.getLogger(PollerVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        poller = new BackgroundPoller(WebClient.create(vertx));
        vertx.setPeriodic(1000 * 60, timerId -> executePolling());
    }

    private void executePolling() {
        fetchServices(res -> {
            if (res.succeeded()) {
                List<JsonObject> services = res.result();
                poller.pollServices(services, this::handlePollingResult);
            }
        });
    }

    private void fetchServices(Handler<AsyncResult<List<JsonObject>>> handler) {
        sendMessage(new JsonObject(), Actions.FETCH_SERVICES, reply -> {
            if (reply.succeeded()) {
                JsonObject body = reply.result().body();
                JsonArray jsonArray = body.getJsonArray("services");

                List<JsonObject> services = new ArrayList<>();
                jsonArray.forEach(service -> {
                    services.add((JsonObject) service);
                });

                handler.handle(Future.succeededFuture(services));
            } else {
                handler.handle(Future.failedFuture(reply.cause()));
            }
        });
    }

    private void handlePollingResult(AsyncResult<JsonObject> promise) {
        if (promise.succeeded()) {
            JsonObject result = promise.result();
            sendMessage(result, Actions.STORE_POLLING_RESULT, reply -> {
                if (reply.failed()) {
                    LOGGER.error("Failed to store polling result", reply.cause());
                }
            });
        } else {
            LOGGER.error("Failed to handle polling result", promise.cause());
        }
    }

    private DeliveryOptions createMessageOptions(Actions action) {
        return new DeliveryOptions().addHeader("action", action.name());
    }

    private void sendMessage(JsonObject messageBody, Actions action, Handler<AsyncResult<Message<JsonObject>>> replyHandler) {
        vertx.eventBus().send("pollerdb.queue", messageBody, createMessageOptions(action), replyHandler);
    }

}
