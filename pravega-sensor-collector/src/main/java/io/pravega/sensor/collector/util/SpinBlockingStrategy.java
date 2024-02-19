/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.util;

import io.github.bucket4j.UninterruptibleBlockingStrategy;

/**
 * A blocking strategy that spins the CPU until the desired time.
 * This is very precise but CPU intensive.
 */
public class SpinBlockingStrategy implements UninterruptibleBlockingStrategy {
    @Override
    public void parkUninterruptibly(long nanosToPark) {
        final long endNanos = System.nanoTime() + nanosToPark;
        while (endNanos - System.nanoTime() > 0) { }
    }
}
