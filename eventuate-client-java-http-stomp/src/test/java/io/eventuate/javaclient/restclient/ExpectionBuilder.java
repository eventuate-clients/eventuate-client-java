package io.eventuate.javaclient.restclient;

import org.json.JSONObject;

public class ExpectionBuilder {
  private Expectations expectations;

  public ExpectionBuilder(Expectations expectations) {
    this.expectations = expectations;
  }

  public void respondingWith(int statusCode) {
    expectations.add(new ExpectedRequest(statusCode));
  }

  public void respondingWith(int statusCode, JSONObject json) {
    expectations.add(new ExpectedRequest(statusCode, json));
  }
}
