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

/**
 * Sensor reading values for every device reading.
 * 
 */
public class ReadingValueDto {
    final public Integer valueIndex;
    final public Integer componentIndex;
    final public Integer sensorIndex;
    final public Integer sensorValueDefinitionId;
    final public String label;
    final public String iconUrl;
    final public String units;
    final public Double value;
    final public String status;

    public ReadingValueDto(Integer valueIndex, Integer componentIndex, Integer sensorIndex,
            Integer sensorValueDefinitionId, String label, String iconUrl, String units, Double value, String status) {
        this.valueIndex = valueIndex;
        this.componentIndex = componentIndex;
        this.sensorIndex = sensorIndex;
        this.sensorValueDefinitionId = sensorValueDefinitionId;
        this.label = label;
        this.iconUrl = iconUrl;
        this.units = units;
        this.value = value;
        this.status = status;
    }

    @Override
    public String toString() {
        return "{componentIndex=" + componentIndex + ", iconUrl=" + iconUrl + ", label=" + label + ", sensorIndex="
                + sensorIndex + ", sensorValueDefinitionId=" + sensorValueDefinitionId + ", status=" + status
                + ", units=" + units + ", value=" + value + ", valueIndex=" + valueIndex + "}";
    }
}
