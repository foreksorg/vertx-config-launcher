/*     / \____  _    _  ____   ______  / \ ____  __    _______
 *    /  /    \/ \  / \/    \ /  /\__\/  //    \/  \  //  /\__\   JΛVΛSLΛNG
 *  _/  /  /\  \  \/  /  /\  \\__\\  \  //  /\  \ /\\/ \ /__\ \   Copyright 2014-2016 Javaslang, http://javaslang.io
 * /___/\_/  \_/\____/\_/  \_/\__\/__/\__\_/  \_//  \__/\_____/   Licensed under the Apache License, Version 2.0
 */
package com.foreks.vertx.launcher;

import io.reactivex.Single;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

public interface VertxFactory {
    static Single<Vertx> create(VertxOptions vertxOptions) {
        if (vertxOptions.isClustered()) {
            return new ClusteredVertxFactory().createVertx(vertxOptions);
        } else {
            return new StandaloneVertxFactory().createVertx(vertxOptions);
        }
    }

    Single<Vertx> createVertx(VertxOptions vertxOptions);

    void beforeLeaveUndeploy(Vertx vertx);
}
