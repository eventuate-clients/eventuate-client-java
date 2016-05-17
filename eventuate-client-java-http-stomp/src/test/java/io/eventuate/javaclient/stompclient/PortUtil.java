package io.eventuate.javaclient.stompclient;

import java.io.IOException;
import java.net.ServerSocket;

public class PortUtil {
  public static int findPort(){
    try {
      ServerSocket s = new ServerSocket();
      s.bind(null);
      int localPort = s.getLocalPort();
      s.close();
      return localPort;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
