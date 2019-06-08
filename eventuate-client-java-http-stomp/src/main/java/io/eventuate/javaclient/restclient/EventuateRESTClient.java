package io.eventuate.javaclient.restclient;

import io.eventuate.*;
import io.eventuate.common.id.Int128;
import io.eventuate.common.json.mapper.JSonMapper;
import io.eventuate.javaclient.commonimpl.*;
import io.eventuate.javaclient.stompclient.EventuateCredentials;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang.StringUtils;

import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class EventuateRESTClient implements AggregateCrud {

  private static Logger logger = LoggerFactory.getLogger(EventuateRESTClient.class);

  private HttpClient httpClient;
  private final String authorizationHeader;
  private final Vertx vertx;
  private final Context context;
  private EventuateCredentials credentials;

  public EventuateRESTClient(Vertx vertx, EventuateCredentials credentials, URI url) {
    this.credentials = credentials;
    HttpClientOptions options = new HttpClientOptions();
    boolean https = url.getScheme().startsWith("https");
    options.setSsl(https);
    options.setDefaultHost(url.getHost());
    options.setDefaultPort(url.getPort() != -1 ? url.getPort() : (https ? 443 : 80));
    options.setKeepAlive(true);
    this.authorizationHeader = "Basic " + Base64.getEncoder().encodeToString((credentials.getApiKeyId() + ":" + credentials.getApiKeySecret()).getBytes());
    context = VertxUtil.getContext(vertx);

    context.runOnContext(ignore -> {
      EventuateRESTClient.this.httpClient = vertx.createHttpClient(options);
    });
    this.vertx = vertx;
  }

  private <T> CompletableFuture<T> withRetry(Supplier<CompletableFuture<T>> asyncRequest) {
    CompletableFuture<T> result = new CompletableFuture<>();
    attemptOperation(asyncRequest, result);
    return result;
  }

  private <T> void attemptOperation(Supplier<CompletableFuture<T>> asyncRequest, CompletableFuture<T> result) {
    CompletableFuture<T> f = asyncRequest.get();
    f.handleAsync((val, throwable) -> {
      if (throwable != null) {
        if (throwable instanceof EventuateServiceUnavailableException) {
          vertx.setTimer(1000, event -> {
            logger.trace("Retrying");
            attemptOperation(asyncRequest, result);
          });
        } else
          result.completeExceptionally(throwable);
      }
      else
        result.complete(val);
      return null;
    });
  }

  @Override
  public CompletableFuture<EntityIdVersionAndEventIds> save(String aggregateType, List<EventTypeAndData> events, Optional<AggregateCrudSaveOptions> options) {
    return withRetry(() -> {
      if (logger.isDebugEnabled())
        logger.debug("save: " + aggregateType + ", events" + events);

      CompletableFuture<EntityIdVersionAndEventIds> cf = new CompletableFuture<>();

      CreateEntityRequest request = new CreateEntityRequest(aggregateType, events);
      options.flatMap(AggregateCrudSaveOptions::getEntityId).ifPresent(request::setEntityId);

      String json = JSonMapper.toJson(request);
      context.runOnContext(ignore -> {
        httpClient.post(makePath())
                .handler(response -> {

                  if (logger.isDebugEnabled())
                    logger.debug("HTTP response=" + response.statusCode());

                  if (response.statusCode() == 200)
                    response.bodyHandler(body -> {
                      try {
                        CreateEntityResponse r = JSonMapper.fromJson(body.toString(), CreateEntityResponse.class);
                        cf.complete(new EntityIdVersionAndEventIds(r.getEntityId(), r.getEntityVersion(), r.getEventIds()));
                      } catch (Throwable e) {
                        cf.completeExceptionally(e);
                      }
                    });
                  else {
                    handleErrorResponse(cf, response, new EntityIdAndType(null, aggregateType), null);
                  }

                })
                .putHeader("authorization", authorizationHeader)
                .putHeader("content-type", "application/json")
                .exceptionHandler(cf::completeExceptionally)
                .end(Buffer.buffer(json));
      });
      return cf;
    });
  }

  private String makePath() {
    return StringUtils.isBlank(credentials.getSpace()) ? "/entity/default" : "/entity/" + credentials.getSpace();
  }

  private void handleErrorResponse(CompletableFuture<?> cf, HttpClientResponse response, EntityIdAndType entityIdAndType, Int128 entityVersion) {
    if (response.statusCode() == 500) {
      cf.completeExceptionally(new EventuateServerException());
//      response.bodyHandler(body -> {
//        System.out.println("500 response body: "+ body.toString());
//      });
    } else if (response.statusCode() == 401)
      cf.completeExceptionally(new EventuateAuthenticationFailedException());
    else if (response.statusCode() == 503)
      cf.completeExceptionally(new EventuateServiceUnavailableException());
    else if (response.statusCode() == 409) {
      response.bodyHandler(body -> {
        JsonObject eventStoreErrorResponse = new JsonObject(body.toString());
        String errorCode = eventStoreErrorResponse.getString("error");
        if ("entity_exists".equals(errorCode)) {
          cf.completeExceptionally(new EntityAlreadyExistsException());
        } else if ("optimistic_lock_error".equals(errorCode)) {
          cf.completeExceptionally(new OptimisticLockingException(entityIdAndType, entityVersion));
        } else if ("duplicate_event".equals(errorCode)) {
          cf.completeExceptionally(new DuplicateTriggeringEventException());
        } else if ("entity_temporarily_unavailable".equals(errorCode)) {
          cf.completeExceptionally(new EntityTemporarilyUnavailableException());
        } else {
          cf.completeExceptionally(new EventuateUnknownResponseException(409, errorCode));
        }
      });
    } else
      cf.completeExceptionally(new EventuateUnknownResponseException(response.statusCode(), null));
  }

  @Override
  public <T extends Aggregate<T>> CompletableFuture<LoadedEvents> find(String aggregateType, String entityId, Optional<AggregateCrudFindOptions> findOptions) {
    return withRetry(() -> {
      CompletableFuture<LoadedEvents> cf = new CompletableFuture<>();
      context.runOnContext(ignore -> {
        httpClient.get(makePath() + "/" + aggregateType + "/" + entityId + makeGetQueryString(findOptions.flatMap(AggregateCrudFindOptions::getTriggeringEvent)))
                .handler(response -> {
                  if (response.statusCode() == 200) {
                    response.bodyHandler(body -> {
                      try {
                        GetEntityResponse r = JSonMapper.fromJson(body.toString(), GetEntityResponse.class);
                        if (r.getEvents().isEmpty())
                          cf.completeExceptionally(new EntityNotFoundException());
                        else {
                          Optional<SerializedSnapshotWithVersion> snapshot = Optional.empty();  // TODO - retrieve snapshot
                          cf.complete(new LoadedEvents(snapshot, r.getEvents()));
                        }
                      } catch (Throwable e) {
                        cf.completeExceptionally(e);
                      }
                    });
                  } else {
                    handleErrorResponse(cf, response, new EntityIdAndType(entityId, aggregateType), null);
                  }
                })
                .putHeader("authorization", authorizationHeader)
                .putHeader("accept", "application/json")
                .exceptionHandler(cf::completeExceptionally)
                .end();
      });
      return cf;
    });
  }

  private String makeGetQueryString(Optional<EventContext> triggeringEvent) {
    return triggeringEvent.flatMap(te -> Optional.of(te.getEventToken())).map(eventToken -> "?triggeringEventToken=" + eventToken).orElse("");
  }


  @Override
  public CompletableFuture<EntityIdVersionAndEventIds> update(EntityIdAndType aggregateIdAndType, Int128 entityVersion, List<EventTypeAndData> events, Optional<AggregateCrudUpdateOptions> updateOptions) {
    return withRetry(() -> {
      if (logger.isDebugEnabled())
        logger.debug("update: " + aggregateIdAndType.getEntityType() + ", " + aggregateIdAndType.getEntityId() + ", " + ", events" + events + ", " + updateOptions);

      CompletableFuture<EntityIdVersionAndEventIds> cf = new CompletableFuture<>();

      // TODO post snapshot if there is one
      UpdateEntityRequest request = new UpdateEntityRequest(events, entityVersion,
              updateOptions.flatMap(AggregateCrudUpdateOptions::getTriggeringEvent).map(EventContext::getEventToken).orElse(null));

      String json = JSonMapper.toJson(request);
      context.runOnContext(ignore -> {
        httpClient.post(makePath() + "/" + aggregateIdAndType.getEntityType() + "/" + aggregateIdAndType.getEntityId())
                .handler(response -> {
                  if (logger.isDebugEnabled())
                    logger.debug("HTTP response=" + response.statusCode());
                  if (response.statusCode() == 200)
                    response.bodyHandler(body -> {
                      try {
                        UpdateEntityResponse r = JSonMapper.fromJson(body.toString(), UpdateEntityResponse.class);
                        cf.complete(new EntityIdVersionAndEventIds(r.getEntityId(), r.getEntityVersion(), r.getEventIds()));
                      } catch (Throwable e) {
                        cf.completeExceptionally(e);
                      }
                    });
                  else {
                    handleErrorResponse(cf, response, aggregateIdAndType, entityVersion);
                  }

                })
                .putHeader("authorization", authorizationHeader)
                .putHeader("content-type", "application/json")
                .exceptionHandler(cf::completeExceptionally)
                .end(Buffer.buffer(json));
      });
      return cf;
    });
  }
}
