package org.workspace7.msa.calculator.client.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.kubernetes.KubernetesServiceImporter;
import io.vertx.servicediscovery.types.HttpEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * A REST client utility that uses Kubernetes service discovery to perform REST calls to the services.  The
 * application uses the http://vertx.io/docs/vertx-service-discovery-bridge-kubernetes to perform the Service Discovery
 * and store them in the Vert.x service registry.
 *
 * @author kameshs
 */
public class VertxRestClientUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(VertxRestClientUtil.class);

    private final long CONNECTION_TIMEOUT = 10000L;
    private final Vertx vertx;
    private ServiceDiscovery serviceDiscovery;
    private JsonObject httpClientOptions = new JsonObject();

    /**
     * The kubernetes configuration which can be passed to register and import services,
     * important ones will be
     * <ul>
     * <li>"master" - the master url</li>
     * <li> "token" - the service account token to use</li>
     * <li>"namespace" - the Kubernetes name space to use</li>
     * </ul>
     * see http://vertx.io/docs/vertx-service-discovery-bridge-kubernetes/java/
     */
    private final JsonObject k8sConfig = new JsonObject();

    private boolean isServiceable = false;

    private Cache<String, HttpClient> httpClientCache;

    public VertxRestClientUtil() {
        vertx = Vertx.vertx();
        discoverServices();

        httpClientOptions.put("keepAlive", false);
        httpClientOptions.put("minPoolSize", 5);
        httpClientOptions.put("maxPoolSize", 25);

        httpClientCache = CacheBuilder.newBuilder().build();
    }

    /**
     * This will discover all Kubernetes services in the cluster, and enables the flag isServiceable to allow
     * the clients to start using the REST client
     */
    public void discoverServices() {
        serviceDiscovery = ServiceDiscovery.create(vertx);
        KubernetesServiceImporter kubernetesServiceImporter = new KubernetesServiceImporter();
        serviceDiscovery.registerServiceImporter(kubernetesServiceImporter, k8sConfig, result -> {

            if (result.succeeded()) {
                isServiceable = true;
            } else {
                LOGGER.error("Error while discovering service", result.cause());
            }
        });

    }

    /**
     * Checks the local cache for the client implementation, if one found it will use it to handle the request
     * else will create one client, put it to the cache and will use that new client to handle
     *
     * @param serviceName   - the service name which will be used to look up the client in cache
     * @param clientHandler - the handler which will be responsible for handling the {@link HttpClient}
     * @throws ExecutionException - any error that might occur while getting element from cache
     */
    private void getClient(String serviceName, Handler<Optional<HttpClient>> clientHandler) throws ExecutionException {

        //Check the cache for the service
        HttpClient httpClient = null;
        try {
            httpClient = httpClientCache.get(serviceName, () -> null);
            if (httpClient != null) {
                clientHandler.handle(Optional.ofNullable(httpClient));
                return;
            }
        } catch (CacheLoader.InvalidCacheLoadException e) {
            LOGGER.info("Http Client for key {} does not exist creating and adding ",serviceName);
            //Since we don't have the client we need to create one via Service Discovery
            HttpEndpoint.getClient(serviceDiscovery, buildServiceFilter(serviceName), httpClientOptions, result -> {

                if (result.succeeded()) {
                    HttpClient newHttpClient = result.result();
                    httpClientCache.put(serviceName, newHttpClient);
                    clientHandler.handle(Optional.of(newHttpClient));
                } else {
                    LOGGER.error("Error building HTTP Client: ", result.cause());
                    clientHandler.handle(null);
                }
            });
        }
    }

    /**
     * This performs the {@link io.vertx.core.http.HttpMethod#DELETE} on the service using its endpoint
     *
     * @param serviceName     - name of the service which will serve the REST request
     * @param path            - the REST path
     * @param headers         - optional headers to add to the request
     * @param responseHandler - the handler that will hold the response of the service call
     */

    public void executeDELETE(String serviceName, String path, Map<String, String> headers,
                              Handler<AsyncResult<String>> responseHandler) {

        if (isServiceable()) {
            try {
                getClient(serviceName, httpClient -> {
                    if (httpClient.isPresent()) {
                        HttpClientRequest request = httpClient.get().delete(path,
                            (res) -> handleResponse(res, responseHandler));
                        request.setTimeout(CONNECTION_TIMEOUT);
                        if (headers != null && !headers.isEmpty()) {
                            headers.forEach((k, v) -> request.putHeader(k, v));
                        }
                        request.end();
                    } else {
                        responseHandler.handle(Future.failedFuture(responseMessage(
                            999, "Unable to load client from Cache").encodePrettily()));
                    }
                });
            } catch (ExecutionException e) {
                LOGGER.error("Error getting Service : " + serviceName + "from cache", e);
                responseHandler.handle(Future.failedFuture(responseMessage(
                    999, "Unable to load client from Cache").encodePrettily()));
            }
        } else {
            serviceClientUnavailable(responseHandler);
        }
    }

    /**
     * This performs the {@link io.vertx.core.http.HttpMethod#GET} on the service using its endpoint
     *
     * @param serviceName     - name of the service which will serve the REST request
     * @param path            - the REST path
     * @param headers         - optional headers to add to the request
     * @param responseHandler - the handler that will hold the response of the service call
     */
    public void executeGET(String serviceName, String path, Map<String, String> headers,
                           Handler<AsyncResult<String>> responseHandler) {
        if (isServiceable()) {

            try {
                getClient(serviceName, httpClient -> {

                    if (httpClient.isPresent()) {
                        HttpClientRequest request = httpClient.get()
                            .get(path, (res) -> handleResponse(res, responseHandler));
                        request.setTimeout(CONNECTION_TIMEOUT);
                        if (headers != null && !headers.isEmpty()) {
                            headers.forEach((k, v) -> request.putHeader(k, v));
                        }
                        request.end();
                    } else {
                        responseHandler.handle(Future.failedFuture(responseMessage(
                            999, "Unable to load client from Cache").encodePrettily()));
                    }
                });
            } catch (ExecutionException e) {
                LOGGER.error("Error getting Service : " + serviceName + "from cache", e);
                responseHandler.handle(Future.failedFuture(responseMessage(
                    999, "Unable to load client from Cache").encodePrettily()));
            }
        } else {
            serviceClientUnavailable(responseHandler);
        }

    }

    /**
     * This performs the {@link io.vertx.core.http.HttpMethod#POST} on the service using its endpoint
     *
     * @param serviceName     - name of the service which will serve the REST request
     * @param path            - the REST path
     * @param body            - the JSON or XML body that will be sent as Request Body to the REST call
     * @param headers         - optional headers to add to the request
     * @param responseHandler - the handler that will hold the response of the service call
     */
    public void executePOST(String serviceName, String path, String body, Map<String, String> headers,
                            Handler<AsyncResult<String>> responseHandler) {
        if (isServiceable()) {
            try {

                getClient(serviceName, httpClient -> {

                    if (httpClient.isPresent()) {
                        HttpClientRequest request = httpClient.get()
                            .post(path, (res) -> handleResponse(res, responseHandler));
                        request.setTimeout(CONNECTION_TIMEOUT);
                        headers.forEach((k, v) -> request.putHeader(k, v));
                        request
                            .write(body)
                            .end();
                    } else {
                        responseHandler.handle(Future.failedFuture(responseMessage(
                            999, "Unable to load client from Cache").encodePrettily()));
                    }

                });
            } catch (ExecutionException e) {
                LOGGER.error("Error getting Service : " + serviceName + "from cache", e);
                responseHandler.handle(Future.failedFuture(responseMessage(
                    999, "Unable to load client from Cache").encodePrettily()));
            }
        } else {
            serviceClientUnavailable(responseHandler);
        }
    }

    /**
     * This performs the {@link io.vertx.core.http.HttpMethod#PUT} on the service using its endpoint
     *
     * @param serviceName     - name of the service which will serve the REST request
     * @param path            - the REST path
     * @param body            - the JSON or XML body that will be sent as Request Body to the REST call
     * @param headers         - optional headers to add to the request
     * @param responseHandler - the handler that will hold the response of the service call
     */

    public void executePUT(String serviceName, String path, String body, Map<String, String> headers,
                           Handler<AsyncResult<String>> responseHandler) {
        if (isServiceable()) {
            try {

                getClient(serviceName, httpClient -> {

                    if (httpClient.isPresent()) {
                        HttpClientRequest request = httpClient.get()
                            .put(path, (res) -> handleResponse(res, responseHandler));
                        request.setTimeout(CONNECTION_TIMEOUT);
                        headers.forEach((k, v) -> request.putHeader(k, v));
                        request
                            .write(body)
                            .end();
                    } else {
                        responseHandler.handle(Future.failedFuture(responseMessage(
                            999, "Unable to load client from Cache").encodePrettily()));
                    }
                });
            } catch (ExecutionException e) {
                LOGGER.error("Error getting Service : " + serviceName + "from cache", e);
                responseHandler.handle(Future.failedFuture(responseMessage(
                    999, "Unable to load client from Cache").encodePrettily()));
            }

        } else {
            serviceClientUnavailable(responseHandler);
        }
    }

    /**
     * the method will add the client service not available to the response as JSON when isServiceable flag is false
     * with STATUS_CODE of 1000
     *
     * @param responseHandler - the response handler where the JSON response string will be added
     */

    protected void serviceClientUnavailable(Handler<AsyncResult<String>> responseHandler) {
        responseHandler.handle(Future.succeededFuture(responseMessage(1000,
            "Service Discovery is not completed, please try after sometime")
            .encodePrettily()));
    }

    /**
     * This flag can be used by clients to see if application is client available for service
     *
     * @return - true if the client is ready to service
     */
    public boolean isServiceable() {
        return isServiceable;
    }

    /**
     * Method to handle the response of the REST request, extract the body and add it to the handler responseHandler
     *
     * @param res             - the http response returned by the client
     * @param responseHandler - the {@link Handler} object to which the response body will be added
     */
    protected void handleResponse(HttpClientResponse res, Handler<AsyncResult<String>> responseHandler) {
        LOGGER.debug("Status Code: " + res.statusCode());
        LOGGER.debug("Status Message: " + res.statusMessage());
        //handling only 200
        if (200 == res.statusCode()) {
            res.bodyHandler(buffer -> responseHandler.handle(Future.succeededFuture(buffer.toString())));
        } else {
            res.bodyHandler(buffer -> responseHandler.handle(Future.succeededFuture(
                responseMessage(res.statusCode(), res.statusMessage()).encodePrettily())));
        }
    }


    /**
     * Builds the Kubernetes service filter based on &quotname&quot;
     *
     * @param serviceName - the name of the service that will be added to the filter
     * @return - {@link JsonObject}
     */
    protected JsonObject buildServiceFilter(String serviceName) {
        JsonObject serviceFilter = new JsonObject();
        serviceFilter.put("name", serviceName);
        return serviceFilter;
    }

    /**
     * A generic response message builder, for cases where there is issue extract response body or want to send
     * custom response body
     *
     * @param statusCode    - the status code that will be sent
     * @param statusMessage - the status message corresponding to the statuscode
     * @return - {@link JsonObject}
     */
    protected JsonObject responseMessage(int statusCode, String statusMessage) {
        JsonObject respError = new JsonObject();
        respError.put("statusCode", statusCode);
        respError.put("statusMessage", statusMessage);
        return respError;
    }
}
