package com.chat.chat.DTO;

public class UiCustomizationChangeDTO {

    private String area;
    private String property;
    private String value;
    private String valuePreset;

    public UiCustomizationChangeDTO() {}

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getProperty() { return property; }
    public void setProperty(String property) { this.property = property; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getValuePreset() { return valuePreset; }
    public void setValuePreset(String valuePreset) { this.valuePreset = valuePreset; }
}
