package de.fhg.fokus.piveau.metrics.hub_to_postgres;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.contract.RouterFactoryOptions;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import io.vertx.ext.web.handler.StaticHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static de.fhg.fokus.piveau.metrics.hub_to_postgres.ApplicationConfig.*;


public class MainVerticle extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(MainVerticle.class);

    private JsonObject config;

    @Override
    public void start() {
        LOG.info("Launching MQA-Hub-2-Postgres...");

        // startup is only successful if no step failed
        Future<Void> steps = loadConfig()
                .compose(handler -> bootstrapVerticles())
                .compose(handler -> startServer());

        steps.setHandler(handler -> {
            if (handler.succeeded()) {
                LOG.info("MQA-Hub-2-Postgres successfully launched");
            } else {
                handler.cause().printStackTrace();
                LOG.error("Failed to launch MQA-Hub-2-Postgres: " + handler.cause());
            }
        });
    }

    private Future<Void> loadConfig() {
        Future<Void> future = Future.future();

        ConfigRetriever configRetriever = ConfigRetriever.create(vertx);

        configRetriever.getConfig(handler -> {
            if (handler.succeeded()) {
                config = handler.result();
                LOG.info(config.encodePrettily());
                future.complete();
            } else {
                future.fail("Failed to load config: " + handler.cause());
            }
        });

        configRetriever.listen(change ->
                config = change.getNewConfiguration());

        return future;
    }

    private CompositeFuture bootstrapVerticles() {
        DeploymentOptions options = new DeploymentOptions()
                .setConfig(config)
                .setInstances(config.getInteger(ApplicationConfig.ENV_WORKER_COUNT, ApplicationConfig.DEFAULT_WORKER_COUNT))
                .setWorker(true);

        List<Future> deploymentFutures = new ArrayList<>();
        deploymentFutures.add(startVerticle(options, FetchVerticle.class.getName()));

        return CompositeFuture.join(deploymentFutures);
    }

    private Future<Void> startServer() {
        Future<Void> startFuture = Future.future();
        Integer port = config.getInteger(ENV_APPLICATION_PORT, DEFAULT_APPLICATION_PORT);

        OpenAPI3RouterFactory.create(vertx, "webroot/openapi.yaml", handler -> {
            if (handler.succeeded()) {
                OpenAPI3RouterFactory routerFactory = handler.result();
                RouterFactoryOptions options = new RouterFactoryOptions().setMountNotImplementedHandler(true).setMountValidationFailureHandler(true);
                routerFactory.setOptions(options);

                routerFactory.addHandlerByOperationId("fetch", this::handleFetchRequest);
                routerFactory.addHandlerByOperationId("clean", this::handleCleanRequest);

                Router router = routerFactory.getRouter();
                router.route("/*").handler(StaticHandler.create());

                HttpServer server = vertx.createHttpServer(new HttpServerOptions().setPort(port));
                server.requestHandler(router).listen();

                LOG.info("Server successfully launched on port [{}]", port);
                startFuture.complete();
            } else {
                // Something went wrong during router factory initialization
                LOG.error("Failed to start server at [{}]: {}", port, handler.cause());
                startFuture.fail(handler.cause());
            }
        });

        return startFuture;
    }

    private void handleFetchRequest(RoutingContext context) {
        vertx.eventBus().send(ADDRESS_FETCH, "msg.fetch");
        context.response().setStatusCode(202).end();
    }

    private void handleCleanRequest(RoutingContext context) {
        String date = context.pathParams().get("date");

        if (date != null && date.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            vertx.eventBus().send(ADDRESS_CLEAN, date);
            context.response().setStatusCode(202).end();
        } else {
            context.response().setStatusCode(400).end("Invalid date format");
        }
    }

    private Future<Void> startVerticle(DeploymentOptions options, String className) {
        Future<Void> future = Future.future();

        vertx.deployVerticle(className, options, handler -> {
            if (handler.succeeded()) {
                future.complete();
            } else {
                LOG.error("Failed to deploy verticle [{}] : {}", className, handler.cause());
                future.fail("Failed to deploy [" + className + "] : " + handler.cause());
            }
        });

        return future;
    }
}
