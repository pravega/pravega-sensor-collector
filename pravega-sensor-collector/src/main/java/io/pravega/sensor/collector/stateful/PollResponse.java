/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.stateful;

import java.util.List;

import com.google.common.base.Preconditions;
import io.pravega.sensor.collector.simple.PersistentQueueElement;

public class PollResponse<S> {
    public final List<PersistentQueueElement> events;
    public final S state;

    public PollResponse(List<PersistentQueueElement> events, S state) {
        this.events = Preconditions.checkNotNull(events, "events");
        this.state = Preconditions.checkNotNull(state, "state");
    }

    @Override
    public String toString() {
        return "PollResponse{" +
                "events=" + events +
                ", state=" + state +
                '}';
    }
}
