package org.workspace7.msa.calculator.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.workspace7.msa.calculator.client.util.RestHttpClientWithoutKeepAlive;

import java.io.IOException;
import java.util.Collections;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author kameshs
 */
public class ApacheCalculatorClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApacheCalculatorClient.class);

    private static final String SERVICE_URL = "http://simple-calculator-spring";

    public static void main(String[] args) {
        //RestHttpClientUtil restHttpClientUtil = new RestHttpClientUtil();

        RestHttpClientWithoutKeepAlive restHttpClientUtil = new RestHttpClientWithoutKeepAlive();

        LOGGER.info("Calculator Client Started ...");

        while (true) {
            try {

                String respBody = restHttpClientUtil.executeGetRequest(SERVICE_URL + "/api/whoami", Collections.emptyMap());
                LOGGER.info("Response:" + respBody);
                SECONDS.sleep(3);
            } catch (IOException e) {
                LOGGER.error("Error :", e);
            } catch (InterruptedException e) {
                LOGGER.error("Error :", e);
            }
        }
    }

}
