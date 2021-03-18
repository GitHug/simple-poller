package ax.takanoha.simplepoller.poller;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class BackgroundPoller {
  private final WebClient client;

  private static final Logger LOGGER = LoggerFactory.getLogger(PollerVerticle.class);

  public BackgroundPoller(WebClient client) {
    this.client = client;
  }

  public void pollServices(List<JsonObject> services, Handler<AsyncResult<JsonObject>> handler) {
    services.forEach(service -> {
      String url = service.getString("url");

      makeRequest(url, res -> {
        JsonObject outcome = new JsonObject()
            .put("id", service.getInteger("id"))
            .put("status", res.succeeded() ? "OK" : "FAIL");

        handler.handle(Future.succeededFuture(outcome));
      });
    });
  }

  private void makeRequest(String url, Handler<AsyncResult<Void>> handler) {
    try {
      client.requestAbs(HttpMethod.HEAD, url).send(promise -> {
        if (promise.failed() || promise.result().statusCode() != 200) {
          handler.handle(Future.failedFuture("FAIL"));
        } else {
          handler.handle(Future.succeededFuture());
        }
      });
    } catch (Exception ex) {
      LOGGER.error("Failed to make request", ex.getMessage());
      handler.handle(Future.failedFuture(ex.getMessage()));
    }
  }
}
