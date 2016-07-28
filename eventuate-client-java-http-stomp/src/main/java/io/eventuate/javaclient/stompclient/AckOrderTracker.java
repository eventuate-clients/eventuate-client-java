package io.eventuate.javaclient.stompclient;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.*;
import java.util.stream.Collectors;

public class AckOrderTracker {

  private Logger logger = LoggerFactory.getLogger(AckOrderTracker.class);

  List<PendingAckHeader> pendingHeaders = new LinkedList<>();

  synchronized public void add(String ackHeader) {
      pendingHeaders.add(new PendingAckHeader(ackHeader));
  }

  synchronized public List<String> ack(String ackHeader) {
    Optional<PendingAckHeader> first = pendingHeaders.stream().filter(ph -> ph.ackHeader.equals(ackHeader)).findFirst();
    if (first.isPresent()) {
      first.get().acked = true;
      List<String> acked =
              pendingHeaders.stream()
                      .filter(ph -> !ph.acked).findFirst()
                      .map(ph -> new ArrayList<>(pendingHeaders.subList(0, pendingHeaders.indexOf(ph))))
                      .orElse(new ArrayList<>(pendingHeaders))
                      .stream()
                      .map(ph -> ph.ackHeader)
                      .collect(Collectors.toList());
      pendingHeaders.subList(0, acked.size()).clear();
      return acked;
    } else {
      logger.error("Didn't find " + ackHeader);
      return Collections.emptyList();
    }
  }

  public List<PendingAckHeader> getPendingHeaders() {
    return pendingHeaders;
  }

  public class PendingAckHeader {
    private String ackHeader;
    private boolean acked;

    @Override
    public String toString() {
      return ToStringBuilder.reflectionToString(this);
    }

    public PendingAckHeader(String ackHeader) {
      this.ackHeader = ackHeader;
    }
  }
}
