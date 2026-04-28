package com.chat.chat.Configuracion;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private boolean enabled = true;

    @NotBlank
    private String provider = "deepseek";

    @Min(1)
    @Max(10000)
    private int maxInputLength = 1000;

    @Min(1)
    @Max(20000)
    private int maxInputLengthResponder = 4000;

    private boolean rateLimitEnabled = true;

    @Min(1)
    @Max(1000)
    private int maxUsesPerUserPerMinute = 5;

    @Min(1)
    @Max(100000)
    private int maxUsesPerUserPerDay = 200;

    @Min(1)
    @Max(10000)
    private int maxGlobalUsesPerMinute = 20;

    @Min(1)
    @Max(1000000)
    private int maxGlobalUsesPerDay = 1000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public int getMaxInputLength() {
        return maxInputLength;
    }

    public void setMaxInputLength(int maxInputLength) {
        this.maxInputLength = maxInputLength;
    }

    public int getMaxInputLengthResponder() {
        return maxInputLengthResponder;
    }

    public void setMaxInputLengthResponder(int maxInputLengthResponder) {
        this.maxInputLengthResponder = maxInputLengthResponder;
    }

    public boolean isRateLimitEnabled() {
        return rateLimitEnabled;
    }

    public void setRateLimitEnabled(boolean rateLimitEnabled) {
        this.rateLimitEnabled = rateLimitEnabled;
    }

    public int getMaxUsesPerUserPerMinute() {
        return maxUsesPerUserPerMinute;
    }

    public void setMaxUsesPerUserPerMinute(int maxUsesPerUserPerMinute) {
        this.maxUsesPerUserPerMinute = maxUsesPerUserPerMinute;
    }

    public int getMaxUsesPerUserPerDay() {
        return maxUsesPerUserPerDay;
    }

    public void setMaxUsesPerUserPerDay(int maxUsesPerUserPerDay) {
        this.maxUsesPerUserPerDay = maxUsesPerUserPerDay;
    }

    public int getMaxGlobalUsesPerMinute() {
        return maxGlobalUsesPerMinute;
    }

    public void setMaxGlobalUsesPerMinute(int maxGlobalUsesPerMinute) {
        this.maxGlobalUsesPerMinute = maxGlobalUsesPerMinute;
    }

    public int getMaxGlobalUsesPerDay() {
        return maxGlobalUsesPerDay;
    }

    public void setMaxGlobalUsesPerDay(int maxGlobalUsesPerDay) {
        this.maxGlobalUsesPerDay = maxGlobalUsesPerDay;
    }
}
