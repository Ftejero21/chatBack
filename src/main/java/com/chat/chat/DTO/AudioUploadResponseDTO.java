package com.chat.chat.DTO;

public class AudioUploadResponseDTO {
    private String url;
    private String mime;
    private Integer durMs;

    public AudioUploadResponseDTO() {}
    public AudioUploadResponseDTO(String url, String mime, Integer durMs) {
        this.url = url;
        this.mime = mime;
        this.durMs = durMs;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getMime() { return mime; }
    public void setMime(String mime) { this.mime = mime; }

    public Integer getDurMs() { return durMs; }
    public void setDurMs(Integer durMs) { this.durMs = durMs; }
}
