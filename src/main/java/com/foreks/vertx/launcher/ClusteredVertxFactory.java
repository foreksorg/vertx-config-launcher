/*     / \____  _    _  ____   ______  / \ ____  __    _______
 *    /  /    \/ \  / \/    \ /  /\__\/  //    \/  \  //  /\__\   JΛVΛSLΛNG
 *  _/  /  /\  \  \/  /  /\  \\__\\  \  //  /\  \ /\\/ \ /__\ \   Copyright 2014-2016 Javaslang, http://javaslang.io
 * /___/\_/  \_/\____/\_/  \_/\__\/__/\__\_/  \_//  \__/\_____/   Licensed under the Apache License, Version 2.0
 */
package com.foreks.vertx.launcher;

import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.core.ICountDownLatch;
import com.hazelcast.core.LifecycleEvent;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.concurrent.TimeUnit;

public class ClusteredVertxFactory implements VertxFactory {
    private final static Logger LOGGER = LoggerFactory.getLogger(ClusteredVertxFactory.class);
    private ICountDownLatch latch;

    @Override
    public Single<Vertx> createVertx(VertxOptions vertxOptions) {
        HazelcastClusterManager clusterManager = getClusterManager();
        this.latch = clusterManager.getHazelcastInstance().getCountDownLatch("shutdown.latch");
        latch.trySetCount(1);

        vertxOptions.setClusterManager(clusterManager);
        return Single.fromPublisher(publisher -> {
            Vertx.clusteredVertx(vertxOptions, response -> {
                if (response.succeeded()) {
                    Vertx vertx = response.result();
                    clusterManager.getHazelcastInstance().getLifecycleService().addLifecycleListener(state -> {
                        if (state.getState() == LifecycleEvent.LifecycleState.SHUTTING_DOWN) {
                            beforeLeaveUndeploy(vertx);
                        }
                    });
                    publisher.onNext(vertx);
                } else {
                    publisher.onError(response.cause());
                }

                publisher.onComplete();
            });
        });
    }

    private HazelcastClusterManager getClusterManager() {
        try {
            return new HazelcastClusterManager(new FileSystemXmlConfig(System.getProperty("cluster-xml")));
        } catch (FileNotFoundException f) {
            LOGGER.error("{}", f);
            throw new RuntimeException();
        }

    }

    public void beforeLeaveUndeploy(Vertx vertx) {
        Observable.fromIterable(vertx.deploymentIDs())
                  .flatMapCompletable(id -> Completable.fromSingle(s -> {
                                          vertx.undeploy(id, s::onSuccess);
                                      })
                                     )
                  .doOnComplete(latch::countDown)
                  .subscribe();
        try {
            latch.await(30000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
