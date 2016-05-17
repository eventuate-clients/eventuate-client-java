package io.eventuate.javaclient.restclient;

import io.vertx.core.http.HttpServerRequest;
import org.json.JSONObject;

public class ExpectedRequest {
  private int statusCode;
  private String body = null;
  private boolean satisfied;

  public ExpectedRequest(int statusCode) {
    this.statusCode = statusCode;
  }

  public ExpectedRequest(int statusCode, JSONObject json) {
    this.statusCode = statusCode;
    body = json.toString();
  }

  public void handleRequest(HttpServerRequest httpServerRequest) {
    this.satisfied = true;
    if (body != null)
      httpServerRequest.response().setStatusCode(statusCode).putHeader("content-type", "application/json").end(body);
    else
      httpServerRequest.response().setStatusCode(statusCode).end();
  }

  public boolean isSatisfied() {
    return satisfied;
  }
}
