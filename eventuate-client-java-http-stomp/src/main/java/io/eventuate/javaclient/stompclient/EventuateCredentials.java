package io.eventuate.javaclient.stompclient;

public class EventuateCredentials {

  private String apiKeyId;
  private String apiKeySecret;
  private String space;

  public EventuateCredentials(String apiKeyId, String apiKeySecret, String space) {
    this.apiKeyId = apiKeyId;
    this.apiKeySecret = apiKeySecret;
    this.space = space;
  }

  public String getApiKeyId() {
    return apiKeyId;
  }

  public String getApiKeySecret() {
    return apiKeySecret;
  }

  public String getSpace() {
    return space;
  }

  public void setApiKeyId(String apiKeyId) {
    this.apiKeyId = apiKeyId;
  }

  public void setApiKeySecret(String apiKeySecret) {
    this.apiKeySecret = apiKeySecret;
  }

  public void setSpace(String space) {
    this.space = space;
  }
}
