package io.eventuate.javaclient.spring.httpstomp;

import io.eventuate.javaclient.stompclient.EventuateCredentials;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * A Spring Boot type-safe configuration properties class for connecting to an Eventuate server.
 * <p>
 *   The required properties are
 *   <ul>
 *     <li>eventuate.apiKeyId - API key id (previously eventStore.userId) </li>
 *     <li>eventuate.apiKeySecret - API key secret (previously eventStore.password)</li>
 *   </ul>
 */
@ConfigurationProperties(prefix = "eventuate")
public class EventuateHttpStompClientConfigurationProperties {

  private String stompServerHost = "api.eventuate.io";

  private Integer stompServerPort = 61614;

  private URI url = makeDefaultUrl();

  private static URI makeDefaultUrl() {
    try {
      return new URI("https://api.eventuate.io");
    } catch (URISyntaxException e) {
      throw new RuntimeException();
    }
  }

  @NotBlank
  private String apiKeyId = null;

  @NotBlank
  private String apiKeySecret = null;

  @NotBlank
  private String space = "default";

  public String getStompServerHost() {
    return stompServerHost;
  }

  public void setStompServerHost(String stompServerHost) {
    this.stompServerHost = stompServerHost;
  }

  public Integer getStompServerPort() {
    return stompServerPort;
  }

  public void setStompServerPort(Integer stompServerPort) {
    this.stompServerPort = stompServerPort;
  }

  public URI getUrl() {
    return url;
  }

  public void setUrl(URI url) {
    this.url = url;
  }

  public String getApiKeyId() {
    return apiKeyId;
  }

  public void setApiKeyId(String apiKeyId) {
    this.apiKeyId = apiKeyId;
  }

  public String getApiKeySecret() {
    return apiKeySecret;
  }

  public void setApiKeySecret(String apiKeySecret) {
    this.apiKeySecret = apiKeySecret;
  }

  public String getSpace() {
    return space;
  }

  public void setSpace(String space) {
    this.space = space;
  }

  public EventuateCredentials makeCredentials() {
    return new EventuateCredentials(getApiKeyId(),
            getApiKeySecret(), getSpace());
  }

  public URI makeStompUrl() {
    String scheme = (url.getScheme().startsWith("https")) ? "stomp+ssl" : "stomp";
    try {
      return new URI(scheme, null, stompServerHost, stompServerPort, null, null, null);
//      return new URI("stomp", null, "192.168.99.100", 9999, null, null, null);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
