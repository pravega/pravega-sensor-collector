/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.leap;

import java.util.Date;
import java.util.List;

public class DeviceReadingsDto {
    final public Date receivedTimestamp;
    final public List<ReadingValueDto> values;
    final public String deviceId;
    final public Date readingTimestamp;

    public DeviceReadingsDto(Date receivedTimestamp, List<ReadingValueDto> values, String deviceId,
            Date readingTimestamp) {
        this.receivedTimestamp = receivedTimestamp;
        this.values = values;
        this.deviceId = deviceId;
        this.readingTimestamp = readingTimestamp;
    }

    @Override
    public String toString() {
        return "\n[receivedTimestamp=" + receivedTimestamp + ", \nvalues=" + values + ", \ndeviceId=" + deviceId
                + ", \nreadingTimestamp=" + readingTimestamp + "]";
    }

    public Date getReadingTimestamp() {
        return readingTimestamp;
    }

}
