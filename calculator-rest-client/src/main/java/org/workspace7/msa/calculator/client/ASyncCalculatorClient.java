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
import org.workspace7.msa.calculator.client.util.VertxRestClientUtil;

import java.util.Collections;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

/**
 * @author kameshs
 */
public class ASyncCalculatorClient extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(VertxRestClientUtil.class);
    final VertxRestClientUtil vertxRestClientUtil = new VertxRestClientUtil();

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
                LOGGER.info("Handling GET Request...");
                vertxRestClientUtil.executeGET("simple-calculator-spring", request.path(),
                    Collections.emptyMap(), res -> {
                        response
                            .putHeader(CONTENT_TYPE, "application/json; charset=utf-8")
                            .end(Json.encode(res.result()));
                    });
                break;
            }
            case DELETE: {
                vertxRestClientUtil.executeDELETE("simple-calculator-spring", request.path(),
                    Collections.emptyMap(), res -> {
                        response
                            .putHeader(CONTENT_TYPE, "application/json; charset=utf-8")
                            .end(Json.encode(res.result()));
                    });
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

    //    public static void main(String[] args) {
//
// VertxRestClientUtil vertxRestClientUtil = new VertxRestClientUtil();
//
//        while (true) {
//
//            //SAMPLE GET
//            vertxRestClientUtil.executeGET("simple-calculator-spring",
//                "/api/whoami", Collections.emptyMap(), response -> {
//                    if (response.succeeded()) {
//                        LOGGER.info(response.result());
//                    } else {
//                        LOGGER.error("Error:", response.cause());
//                    }
//                });
//
//            //SAMPLE POST
//
//            JsonObject numbersJson = new JsonObject()
//                .put("numbers", new JsonArray().add(5).add(5).add(10));
//
//            String body = numbersJson.encodePrettily();
//            Map<String, String> headers = new HashMap<>();
//            //These are some mandatory headers that is required while sending POST/PUT requests
//            headers.put("Content-Length", String.valueOf(body.length()));
//            headers.put("Content-Type", "application/json; charset=utf8");
//
//            vertxRestClientUtil.executePOST("simple-calculator-spring",
//                "/api/mul", body, headers, response -> {
//                    if (response.succeeded()) {
//                        LOGGER.info(response.result());
//                    } else {
//                        LOGGER.error("Error:", response.cause());
//                    }
//                });
//
//            //give me breather ;)
//            try {
//                SECONDS.sleep(5);
//            } catch (InterruptedException e) {
//
//            }
//        }
//   }
}
