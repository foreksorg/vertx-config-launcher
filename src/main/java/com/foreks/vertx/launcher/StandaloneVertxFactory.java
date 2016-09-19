/*     / \____  _    _  ____   ______  / \ ____  __    _______
 *    /  /    \/ \  / \/    \ /  /\__\/  //    \/  \  //  /\__\   JΛVΛSLΛNG
 *  _/  /  /\  \  \/  /  /\  \\__\\  \  //  /\  \ /\\/ \ /__\ \   Copyright 2014-2016 Javaslang, http://javaslang.io
 * /___/\_/  \_/\____/\_/  \_/\__\/__/\__\_/  \_//  \__/\_____/   Licensed under the Apache License, Version 2.0
 */
package com.foreks.vertx.launcher;

import io.vertx.core.VertxOptions;
import io.vertx.rxjava.core.Vertx;
import rx.Observable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class StandaloneVertxFactory implements VertxFactory {
    private CountDownLatch latch;
    @Override
    public Observable<Vertx> createVertx(VertxOptions vertxOptions) {
        this.latch = new CountDownLatch(1);
        return Observable.just(Vertx.vertx(vertxOptions)).map(vertx -> {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> beforeLeaveUndeploy(vertx)));
            return vertx;
        });
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
