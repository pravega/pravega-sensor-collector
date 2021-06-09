package io.pravega.sensor.collector.leap;

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
        return "{componentIndex=" + componentIndex + ", iconUrl=" + iconUrl + ", label=" + label
                + ", sensorIndex=" + sensorIndex + ", sensorValueDefinitionId=" + sensorValueDefinitionId + ", status="
                + status + ", units=" + units + ", value=" + value + ", valueIndex=" + valueIndex + "}";
    }
    
    
}
