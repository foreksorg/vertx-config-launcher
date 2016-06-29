package com.foreks.vertx.launcher;

import com.hazelcast.config.FileSystemXmlConfig;
import freemarker.template.TemplateException;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Action1;

import java.io.FileNotFoundException;
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
		return Observable.from(verticles).flatMap(verticleConf -> deployVerticle(vertx, verticleConf));
	}

	private static Observable<String> deployVerticle(Vertx vertx, Map.Entry<String, Object> verticleConf) {
		DeploymentOptions deploymentOptions = new DeploymentOptions(((JsonObject) verticleConf.getValue()).getJsonObject("deploymentOptions"));
		return vertx.deployVerticleObservable(verticleConf.getKey(), deploymentOptions);
	}

	private static Observable<Vertx> createVertx(VertxOptions vertxOptions) {
		if (vertxOptions.isClustered()) {
			vertxOptions.setClusterManager(getClusterManager());
			return Vertx.clusteredVertxObservable(vertxOptions);
		} else {
			return Observable.just(Vertx.vertx(vertxOptions));
		}
	}

	private static HazelcastClusterManager getClusterManager() {
		try {
			return new HazelcastClusterManager(new FileSystemXmlConfig(System.getProperty("cluster-xml")));
		} catch (FileNotFoundException f) {
			LOGGER.error("{}", f);
			throw new RuntimeException();
		}

	}

	private static Action1<String> logSuccess = message -> LOGGER.info("Verticle Deployed {}", message);

	private static Action1<Throwable> logError = throwable -> LOGGER.error("Verticle Could not be Deployed {}", throwable);

	public static void main(String[] args) {
		readConf(System.getProperty("conf")).ifPresent(c -> {
			createVertx(new VertxOptions(c.getJsonObject("vertxOptions"))).flatMap(vertx -> deployVerticles(vertx,
																											c.getJsonObject("verticles")))
			.subscribe(logSuccess, logError);
		});
	}

}
