package ax.takanoha.simplepoller;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import ax.takanoha.simplepoller.database.DatabaseVerticle;
import ax.takanoha.simplepoller.poller.PollerVerticle;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Future<Void> startFuture) {
    Future<String> databaseVerticleDeployment = Future.future();
    vertx.deployVerticle(new DatabaseVerticle(), databaseVerticleDeployment.completer());

    databaseVerticleDeployment.compose(id -> {
      Future<String> httpVerticleDeployment = Future.future();
      vertx.deployVerticle("ax.takanoha.simplepoller.http.HttpServerVerticle", new DeploymentOptions().setInstances(2), httpVerticleDeployment);

      return httpVerticleDeployment;
    }).compose(id -> {
      Future<String> pollerVerticleDeployment = Future.future();
      vertx.deployVerticle(new PollerVerticle(), pollerVerticleDeployment.completer());

      return pollerVerticleDeployment;
    }).setHandler(promise -> {
      if (promise.succeeded()) {
        startFuture.complete();
      } else {
        startFuture.fail(promise.cause());
      }
    });
  }
}



