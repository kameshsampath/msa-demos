package org.workspace7.msa.calculator.client;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.workspace7.msa.calculator.client.util.RestHttpClientWithoutKeepAlive;
import org.workspace7.msa.calculator.client.util.VertxRestClientUtil;

import java.io.IOException;
import java.util.Collections;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

/**
 * @author kameshs
 */
public class SyncCalculatorClient extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(VertxRestClientUtil.class);
    private static final String SERVICE_URL = "http://simple-calculator-spring";
    RestHttpClientWithoutKeepAlive restHttpClientUtil = new RestHttpClientWithoutKeepAlive();

    @Override
    public void start(Future future) throws Exception {

        // Create a router object.
        Router router = Router.router(vertx);

        router.route("/api/*").handler(this::handleProxyRequest);
        router.route("/api/*").handler(BodyHandler.create());

        vertx.createHttpServer()
            .requestHandler(router::accept)
            .listen(8080, future.completer());


    }

    private void handleProxyRequest(RoutingContext routingContext) {


        HttpServerRequest request = routingContext.request();
        HttpServerResponse response = routingContext.response();
        HttpMethod httpMethod = routingContext.request().method();

        LOGGER.info("Handling Request with method {} for path {} ", request.method(), request.path());

        switch (httpMethod) {
            case GET:
            default: {
                LOGGER.info("Sync Handling GET Request...");
                try {
                    String respBody = restHttpClientUtil.executeGetRequest(SERVICE_URL + request.path(),
                        Collections.emptyMap());
                    response
                        .putHeader(CONTENT_TYPE, "application/json; charset=utf-8")
                        .end(Json.encode(respBody));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
            case DELETE: {
                //TODO
                break;
            }
            case POST: {
                //TODO
                break;
            }
            case PUT: {
                //TODO
                break;
            }
        }
    }
}
