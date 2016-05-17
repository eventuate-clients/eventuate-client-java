/*
 *  Copyright (c) 2011-2015 The original author or authors
 *  ------------------------------------------------------
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *       The Eclipse Public License is available at
 *       http://www.eclipse.org/legal/epl-v10.html
 *
 *       The Apache License v2.0 is available at
 *       http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

package io.eventuate.javaclient.stompclient;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.NetClient;
import io.vertx.ext.stomp.Frame;
import io.vertx.ext.stomp.StompClient;
import io.vertx.ext.stomp.StompClientConnection;
import io.vertx.ext.stomp.StompClientOptions;
import io.vertx.ext.stomp.impl.FrameParser;
import io.vertx.ext.stomp.impl.StompClientConnectionImpl;
import io.vertx.ext.stomp.utils.Headers;

/**
 * Default implementation of {@link StompClient}.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class MyStompClientImpl implements StompClient {

  private static final Logger log = LoggerFactory.getLogger(MyStompClientImpl.class);

  private final Vertx vertx;
  private final StompClientOptions options;
  private NetClient client;
  private Handler<Void> closeHandler;


  public MyStompClientImpl(Vertx vertx, StompClientOptions options) {
    this.vertx = vertx;
    this.options = options;
  }

  @Override
  public StompClient connect(int port, String host, Handler<AsyncResult<StompClientConnection>> resultHandler) {
    return connect(port, host, vertx.createNetClient(options), resultHandler);
  }

  @Override
  public StompClient connect(Handler<AsyncResult<StompClientConnection>> resultHandler) {
    return connect(options.getPort(), options.getHost(), vertx.createNetClient(options), resultHandler);
  }

  @Override
  public StompClient connect(NetClient netClient, Handler<AsyncResult<StompClientConnection>> resultHandler) {
    return connect(options.getPort(), options.getHost(), netClient, resultHandler);
  }

  @Override
  public synchronized void close() {
    if (client != null) {
      client.close();
      client = null;
    }
  }


  @Override
  public StompClientOptions options() {
    return options;
  }

  @Override
  public Vertx vertx() {
    return vertx;
  }

  @Override
  public synchronized StompClient connect(int port, String host, NetClient net, Handler<AsyncResult<StompClientConnection>>
          resultHandler) {
    client = net.connect(port, host, ar -> {
      if (ar.failed()) {
        if (resultHandler != null) {
          resultHandler.handle(Future.failedFuture(ar.cause()));
        } else {
          log.error(ar.cause());
        }
      } else {
        // Create the connection, the connection attach a handler on the socket.
        StompClientConnectionImpl scci = new StompClientConnectionImpl(vertx, ar.result(), this, resultHandler);
        scci.errorHandler(frame -> {
                  resultHandler.handle(Future.failedFuture(new RuntimeException("Error")));
                }
        );
        scci.closeHandler(connection -> {
                  handleClose();
                }
        );
        // Socket connected - send "CONNECT" Frame
        ar.result().write(getConnectFrame(host));
      }
    });
    return this;
  }

  public MyStompClientImpl closeHandler(Handler<Void> handler) {
    this.closeHandler = handler;
    return this;
  }

  private void handleClose() {
     closeHandler.handle(null);
  }

  private Buffer getConnectFrame(String host) {
    Headers headers = Headers.create();
    String accepted = getAcceptedVersions();
    if (accepted != null) {
      headers.put(Frame.ACCEPT_VERSION, accepted);
    }
    if (!options.isBypassHostHeader()) {
      headers.put(Frame.HOST, host);
    }
    if (options.getVirtualHost() != null) {
      headers.put(Frame.HOST, options.getVirtualHost());
    }
    if (options.getLogin() != null) {
      headers.put(Frame.LOGIN, options.getLogin());
    }
    if (options.getPasscode() != null) {
      headers.put(Frame.PASSCODE, options.getPasscode());
    }
    headers.put(Frame.HEARTBEAT, Frame.Heartbeat.create(options.getHeartbeat()).toString());

    Frame.Command cmd = options.isUseStompFrame() ? Frame.Command.STOMP : Frame.Command.CONNECT;
    final Frame frame = new Frame(cmd, headers, null);
    return frame.toBuffer();
  }

  private String getAcceptedVersions() {
    if (options.getAcceptedVersions() == null || options.getAcceptedVersions().isEmpty()) {
      return null;
    }
    StringBuilder builder = new StringBuilder();
    options.getAcceptedVersions().stream().forEach(
            version -> builder.append(builder.length() == 0 ? version : FrameParser.COMMA + version)
    );
    return builder.toString();
  }


}
