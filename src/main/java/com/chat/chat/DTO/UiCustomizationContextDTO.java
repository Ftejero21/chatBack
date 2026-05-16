package com.chat.chat.DTO;

import java.util.Map;
import java.util.List;

public class UiCustomizationContextDTO {

    private String version;
    private String themeMode;
    private String scope;
    private Map<String, Map<String, String>> currentStyles;
    private Map<String, Object> areaCatalog;
    private Map<String, Object> domState;
    private Map<String, Map<String, String>> computedStyles;
    private Map<String, List<String>> groupExpansionHints;

    public UiCustomizationContextDTO() {}

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getThemeMode() { return themeMode; }
    public void setThemeMode(String themeMode) { this.themeMode = themeMode; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public Map<String, Map<String, String>> getCurrentStyles() { return currentStyles; }
    public void setCurrentStyles(Map<String, Map<String, String>> currentStyles) { this.currentStyles = currentStyles; }

    public Map<String, Object> getAreaCatalog() { return areaCatalog; }
    public void setAreaCatalog(Map<String, Object> areaCatalog) { this.areaCatalog = areaCatalog; }

    public Map<String, Object> getDomState() { return domState; }
    public void setDomState(Map<String, Object> domState) { this.domState = domState; }

    public Map<String, Map<String, String>> getComputedStyles() { return computedStyles; }
    public void setComputedStyles(Map<String, Map<String, String>> computedStyles) { this.computedStyles = computedStyles; }

    public Map<String, List<String>> getGroupExpansionHints() { return groupExpansionHints; }
    public void setGroupExpansionHints(Map<String, List<String>> groupExpansionHints) { this.groupExpansionHints = groupExpansionHints; }
}
