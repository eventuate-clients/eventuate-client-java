package io.eventuate.javaclient.restclient;


import org.junit.Assert;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Expectations {

  private List<ExpectedRequest> expectedRequests= new LinkedList<>();
  private int expectedIndex = 0;


  public boolean hasNext() {
    return expectedIndex != expectedRequests.size();
  }

  public ExpectedRequest next() {
    return expectedRequests.get(expectedIndex++);
  }

  public ExpectionBuilder expect() {
    return new ExpectionBuilder(this);
  }

  public void assertSatisfied() {
    List<ExpectedRequest> unsatisified = expectedRequests.stream().filter(er -> !er.isSatisfied()).collect(Collectors.toList());
    if (!unsatisified.isEmpty()) {
      Assert.fail("Unsatisfied expections: " + unsatisified);
    }
  }


  public void add(ExpectedRequest expectedRequest) {
    expectedRequests.add(expectedRequest);
  }
}
