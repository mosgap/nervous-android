package ch.ethz.soms.nervous.virtualsensors.model;

import com.google.gson.annotations.SerializedName;

public class VirtualSensorModel {

    public static final String MORE_OPTIONS_STR = " ... ";
    private String sensorId;
    private String message;
    private String category;

    private OptionsType optionsType;
    private String[] options;

    private String[] defaultInterval; //Default defaultInterval value in milliseconds
    private String[] intervalTypes; //Interval name to be shown in the settings list
    private String[] intervalValues; //Interval values in minutes

    public String getSensorId() {
        return sensorId;
    }

    public VirtualSensorModel setSensorId(String sensorId) {
        this.sensorId = sensorId;

        return this;
    }

    public String getMessage() {
        return message;
    }

    public VirtualSensorModel setMessage(String message) {
        this.message = message;

        return this;
    }

    public String getCategory() {
        return category;
    }

    public VirtualSensorModel setCategory(String category) {
        this.category = category;

        return this;
    }

    public OptionsType getOptionsType() {
        return optionsType;
    }

    public VirtualSensorModel setOptionsType(OptionsType optionsType) {
        this.optionsType = optionsType;

        return this;
    }

    public String[] getOptions() {
        return options;
    }

    public VirtualSensorModel setOptions(String[] options) {
        this.options = options;

        return this;
    }

    public String[] getDefaultInterval() {
        return defaultInterval;
    }

    public VirtualSensorModel setDefaultInterval(String[] defaultInterval) {
        this.defaultInterval = defaultInterval;

        return this;
    }

    public String[] getIntervalTypes() {
        return intervalTypes;
    }

    public String getIntervalType(String value) {
        for(int index = 0; index < intervalValues.length; index++) {
            if(intervalValues[index].equals(value)) {
                return intervalTypes[index];
            }
        }

        return null;
    }

    public VirtualSensorModel setIntervalTypes(String[] intervalTypes) {
        this.intervalTypes = intervalTypes;

        return this;
    }

    public String[] getIntervalValues() {
        return intervalValues;
    }

    public VirtualSensorModel setIntervalValues(String[] intervalValues) {
        this.intervalValues = intervalValues;

        return this;
    }

    public enum OptionsType {
        @SerializedName("0")
        MANUAL_STRING_INPUT,
        @SerializedName("1")
        MULTYPLE_OPTIONS,
        @SerializedName("2")
        MULTYPLE_OPTIONS_PLUS_INPUT
    }
}
