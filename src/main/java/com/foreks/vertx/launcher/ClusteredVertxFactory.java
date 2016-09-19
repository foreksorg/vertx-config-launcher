/*     / \____  _    _  ____   ______  / \ ____  __    _______
 *    /  /    \/ \  / \/    \ /  /\__\/  //    \/  \  //  /\__\   JΛVΛSLΛNG
 *  _/  /  /\  \  \/  /  /\  \\__\\  \  //  /\  \ /\\/ \ /__\ \   Copyright 2014-2016 Javaslang, http://javaslang.io
 * /___/\_/  \_/\____/\_/  \_/\__\/__/\__\_/  \_//  \__/\_____/   Licensed under the Apache License, Version 2.0
 */
package com.foreks.vertx.launcher;

import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.core.ICountDownLatch;
import com.hazelcast.core.LifecycleEvent;
import io.vertx.core.VertxOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.FileNotFoundException;
import java.util.concurrent.TimeUnit;

public class ClusteredVertxFactory implements VertxFactory {
    private final static Logger LOGGER = LoggerFactory.getLogger(ClusteredVertxFactory.class);
    private ICountDownLatch latch;
    @Override
    public Observable<Vertx> createVertx(VertxOptions vertxOptions) {
        HazelcastClusterManager clusterManager = getClusterManager();
        ICountDownLatch latch = clusterManager.getHazelcastInstance().getCountDownLatch("shutdown.latch");
        latch.trySetCount(1);

        vertxOptions.setClusterManager(clusterManager);
        return Vertx.clusteredVertxObservable(vertxOptions).map(vertx -> {
            clusterManager.getHazelcastInstance().getLifecycleService().addLifecycleListener(state -> {
                if (state.getState() == LifecycleEvent.LifecycleState.SHUTTING_DOWN) {
                    beforeLeaveUndeploy(vertx);
                }
            });
            return vertx;
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
        Observable.from(vertx.deploymentIDs().stream().map(vertx::undeployObservable).toArray())
                  .doOnCompleted(latch::countDown)
                  .subscribe();
        try {
            latch.await(30000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
