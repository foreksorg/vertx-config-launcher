package com.foreks.vertx.launcher;

import freemarker.template.TemplateException;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;


public class VertxConfigLauncher {

    private final static Logger LOGGER = LoggerFactory.getLogger(VertxConfigLauncher.class);

    private static Optional<JsonObject> readConf(String path) {
        return readConf(Paths.get(path));
    }

    private static Optional<JsonObject> readConf(Path path) {
        try {
            JsonObject conf = new JsonObject(FreemarkerHelper.processTemplate(path));
            LOGGER.info("Config -> {}", conf.encodePrettily());
            return Optional.of(conf);
        } catch (IOException e) {
            LOGGER.error("Config file doesn't exist {}", e);
            return Optional.empty();
        } catch (TemplateException e) {
            LOGGER.error("Config provided as Template but JVM argument does not! Please check JVM arguments to match it with template {}",
                         e);
            return Optional.empty();
        }
    }

    private static Observable<String> deployVerticles(Vertx vertx, JsonObject verticles) {
        return Observable.fromIterable(verticles).flatMap(verticleConf -> deployVerticle(vertx, verticleConf));
    }

    private static Observable<String> deployVerticle(Vertx vertx, Map.Entry<String, Object> verticleConf) {
        DeploymentOptions deploymentOptions = new DeploymentOptions(((JsonObject) verticleConf.getValue()).getJsonObject("deploymentOptions"));
        return Observable.fromPublisher(observer -> {
            vertx.deployVerticle(verticleConf.getKey(), deploymentOptions, result -> {
                if (result.succeeded()) {
                    observer.onNext(result.result());
                } else {
                    observer.onError(result.cause());
                }
                observer.onComplete();
            });
        });
    }

    private static Consumer<String> logSuccess = message -> LOGGER.info("Verticle Deployed {}", message);

    private static Consumer<Throwable> logError = throwable -> LOGGER.error("Verticle Could not be Deployed {}", throwable);

    public static void main(String[] args) {
        readConf(System.getProperty("config.file"))
                .ifPresent(c -> VertxFactory.create(new VertxOptions(c.getJsonObject("vertxOptions")))
                                            .flatMapObservable(vertx -> deployVerticles(vertx, c.getJsonObject("verticles")))
                                            .subscribe(logSuccess, logError));
    }

}
