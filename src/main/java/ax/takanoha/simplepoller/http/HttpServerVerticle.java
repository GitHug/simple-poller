package ax.takanoha.simplepoller.http;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.ext.web.handler.StaticHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ax.takanoha.simplepoller.Actions;

import java.util.Arrays;

public class HttpServerVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        OpenAPI3RouterFactory.create(vertx, "src/main/resources/api.yaml", res -> {
            if (res.succeeded()) {
                OpenAPI3RouterFactory routerFactory = res.result();

                addHandlers(routerFactory);

                Router router = routerFactory.getRouter();
                router.errorHandler(500, this::errorHandler);

                HttpServer server = vertx.createHttpServer();
                server
                    .requestHandler(router)
                    .listen(8080, promise -> {
                        if (promise.succeeded()) {
                            LOGGER.info("HTTP server running on port " + 8080);
                            startFuture.complete();
                        } else {
                            LOGGER.error("Failed to start HTTP server", promise.cause());
                            startFuture.fail(promise.cause());
                        }
                    });
            } else {
                LOGGER.error("Failed to read api contract", res.cause());
                startFuture.fail(res.cause());
            }
        });
    }

    private void addHandlers(OpenAPI3RouterFactory routerFactory) {
        routerFactory.addHandlerByOperationId("getStatic", StaticHandler.create());
        routerFactory.addHandlerByOperationId("getServices", this::fetchServicesHandler);
        routerFactory.addHandlerByOperationId("getService", this::fetchServiceHandler);
        routerFactory.addHandlerByOperationId("createService", this::createServiceHandler);
        routerFactory.addHandlerByOperationId("deleteService", this::deleteServiceHandler);
    }

    private void fetchServicesHandler(RoutingContext req) {
        sendMessage(new JsonObject(), Actions.FETCH_SERVICES, reply -> {
            if (reply.succeeded()) {
                JsonObject body = reply.result().body();

                req
                    .response()
                    .putHeader("content-type", "application/json")
                    .setStatusCode(200)
                    .end(body.getJsonArray("services").encode());
            } else {
                LOGGER.error(reply.cause().getMessage(), reply.cause());
                req.fail(reply.cause());
            }
        });
    }

    private void fetchServiceHandler(RoutingContext req) {
        String id = req.request().getParam("id");
        sendMessage(new JsonObject().put("id", Integer.parseInt(id)), Actions.FETCH_SERVICE, reply -> {
            if (reply.succeeded()) {
                JsonObject body = reply.result().body();
                JsonObject service = body.getJsonObject("service");

                if (service.getBoolean("found", true)) {
                    req
                        .response()
                        .putHeader("content-type", "application/json")
                        .setStatusCode(200)
                        .end(service.encode());
                } else {
                    req
                        .response()
                        .putHeader("content-type", "application/json")
                        .setStatusCode(404)
                        .end();
                }
            } else {
                LOGGER.error(reply.cause().getMessage(), reply.cause());
                req.fail(reply.cause());
            }
        });
    }

    private void createServiceHandler(RoutingContext req) {
        JsonObject jsonBody = req.getBodyAsJson();
        sendMessage(jsonBody, Actions.CREATE_SERVICE, reply -> {
            if (reply.succeeded()) {
                System.out.println("received reply");
                JsonObject body = reply.result().body();

                req
                    .response()
                    .putHeader("content-type", "application/json")
                    .setStatusCode(201)
                    .end(body.getJsonObject("service").encode());
            } else {
                LOGGER.error(reply.cause().getMessage(), reply.cause());
                req.fail(reply.cause());
            }
        });
    }

    private void deleteServiceHandler(RoutingContext req) {
        String id = req.request().getParam("id");
        sendMessage(new JsonObject().put("id", Integer.parseInt(id)), Actions.DELETE_SERVICE, reply -> {
            if (reply.succeeded()) {
                req.response().putHeader("content-type", "application/json").setStatusCode(200).end();
            } else {
                LOGGER.error(reply.cause().getMessage(), reply.cause());
                req.fail(reply.cause());
            }
        });
    }

    private DeliveryOptions createMessageOptions(Actions action) {
        return new DeliveryOptions().addHeader("action", action.name());
    }

    private void sendMessage(JsonObject messageBody, Actions action, Handler<AsyncResult<Message<JsonObject>>> replyHandler) {
        vertx.eventBus().send("pollerdb.queue", messageBody, createMessageOptions(action), replyHandler);
    }

    private void errorHandler(RoutingContext req) {
        req.response()
            .setStatusCode(500)
            .end(req.failure() + "\n" + Arrays.toString(req.failure().getStackTrace()));
    }
}
