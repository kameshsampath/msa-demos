package org.workspace7.msa.calculator.client;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author kameshs
 */
public class CalculatorClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CalculatorClient.class);

    private static final String SERVICE_URL = "http://simple-calculator-spring";

    public static String read(InputStream input) throws IOException {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
            return buffer.lines().collect(Collectors.joining("\n"));
        }
    }

    // this causes issue
    //private static CloseableHttpClient httpClient = HttpClients.createDefault()
    private static CloseableHttpClient httpClient() {
        return HttpClients.createDefault();
    }

    public static void main(String[] args) {
        CloseableHttpResponse response = null;
        LOGGER.info("Calculator Client Started ...");
        try {
            HttpGet httpGet = new HttpGet(SERVICE_URL + "/api/whoami");

            while (true) {

                response = httpClient().execute(httpGet);
                String respBody = read(response.getEntity().getContent());
                LOGGER.info("Response:" + respBody);
                SECONDS.sleep(1);
            }
        } catch (IOException e) {
            LOGGER.error("Error :", e);
        } catch (InterruptedException e) {
            LOGGER.error("Error :", e);
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {

                }
            }
        }
    }
}
