package org.workspace7.msa.calculator.client.util;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class providing a REST client implementation
 *
 * @author Jaisy_Cheriyan
 *
 */
public class RestHttpClientUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(RestHttpClientUtil.class);

	private final RequestConfig config;
	private final SocketConfig sconfig;
	// CloseableHttpClient httpclient = getHTTPClient();

	private static enum HTTPMethod {
		GET, POST, PUT, DELETE
	}

	public RestHttpClientUtil() {
		config = RequestConfig.custom().setConnectionRequestTimeout(30000)
				.setConnectTimeout(30000)
				.setSocketTimeout(30000).setExpectContinueEnabled(Boolean.TRUE)
				.setMaxRedirects(50).setStaleConnectionCheckEnabled(true).build();
		sconfig = SocketConfig.custom().setSoTimeout(30000)
				.setSoKeepAlive(Boolean.FALSE).build();

	}

	private CloseableHttpClient getHTTPClient() {
		LOGGER.info("Entering getHTTPClient at {}", System.currentTimeMillis());
		CloseableHttpClient httpclient = null;
		try {
			PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
			connManager.setMaxTotal(20);
			httpclient = HttpClients.custom().setDefaultRequestConfig(config).setConnectionManager(connManager)
					.setDefaultSocketConfig(sconfig).build();
		} catch (NumberFormatException e) {
			throw new BaseException("Failed :NumberFormatException", e);
		} catch (Exception e) {
			throw new BaseException("Failed :CloseableHttpClient", e);
		} finally {
			LOGGER.info("Exiting getHTTPClient at {}", System.currentTimeMillis());
		}
		return httpclient;
	}

	/**
	 * Rest client method executing GET request
	 *
	 * @param url
	 *            : rest server url
	 * @param header
	 *            : header parameters in map
	 * @return Response string
	 * @throws IOException
	 */
	public String executeGetRequest(String url, Map<String, String> header) throws IOException {
		LOGGER.info("Entering executeGetRequest at {}", System.currentTimeMillis());
		HttpGet request = null;
		try {
			request = getHTTPGetRequest(url);
			request = (HttpGet) setHttpHeader(header, request);
			return sendHttpResponse(request);
		} finally {
			closeHTTPRequest(request);
			LOGGER.info("Exiting executeGetRequest at {}", System.currentTimeMillis());
		}
	}

	/**
	 * Rest client method executing POST request
	 *
	 * @param url
	 *            : rest server url
	 * @param json
	 *            : request body
	 * @param Header
	 *            : header parameters in map
	 * @return Response string
	 * @throws IOException
	 */
	public String executePostRequest(String url, Object json, Map<String, String> Header) throws IOException {

		LOGGER.info("Entering executePostRequest at {}", System.currentTimeMillis());
		HttpPost request = null;
		try {
			StringEntity params = null;
			params = new StringEntity(json.toString());
			request = getHTTPPostRequest(url);
			request = (HttpPost) setHttpHeader(Header, request);
			request.setEntity(params);
			return sendHttpResponse(request);
		} finally {
			closeHTTPRequest(request);// closing the request
			LOGGER.info("Exiting executePostRequest at {}", System.currentTimeMillis());
		}

	}

	/**
	 * Rest client method executing PUT request
	 *
	 * @param url
	 *            : rest server url
	 * @param json
	 *            : request body
	 * @param Header
	 *            : header parameters in map
	 * @return Response string
	 * @throws IOException
	 */
	public String executePutRequest(String url, Object json, Map<String, String> Header) throws IOException {
		LOGGER.info("Entering executePutRequest at {}", System.currentTimeMillis());
		HttpPut request = null;
		try {
			StringEntity params = null;
			params = new StringEntity(json.toString());
			request = getHTTPPutRequest(url);
			request = (HttpPut) setHttpHeader(Header, request);
			request.setEntity(params);
			return sendHttpResponse(request);
		} finally {
			closeHTTPRequest(request);// closing the request
			LOGGER.info("Exiting executePutRequest at {}", System.currentTimeMillis());
		}
	}

	public String executeDeleteRequest(String url, Map<String, String> Header) throws IOException {

		LOGGER.info("Entering executeDeleteRequest at {}", System.currentTimeMillis());
		HttpDelete request = null;
		try {
			request = getHTTPDeleteRequest(url);
			request = (HttpDelete) setHttpHeader(Header, request);
			return sendHttpResponse(request);
		} finally {
			closeHTTPRequest(request);// closing the request
			LOGGER.info("Exiting executeDeleteRequest at {}", System.currentTimeMillis());
		}

	}

	private HttpRequestBase getHTTPRequest(String restUri, Enum<HTTPMethod> httpMethod) {
		HttpRequestBase request = null;
		if (restUri != null) {
			if (httpMethod.name().equals("POST")) {
				request = new HttpPost(restUri);
			} else if (httpMethod.name().equals("GET")) {
				request = new HttpGet(restUri);
			} else if (httpMethod.name().equals("PUT")) {
				request = new HttpPut(restUri);
			} else if (httpMethod.name().equals("DELETE")) {
				request = new HttpDelete(restUri);
			}
		}
		return request;
	}

	private HttpGet getHTTPGetRequest(String restUri) {
		return (HttpGet) getHTTPRequest(restUri, HTTPMethod.GET);
	}

	private HttpPut getHTTPPutRequest(String restUri) {
		return (HttpPut) getHTTPRequest(restUri, HTTPMethod.PUT);
	}

	private HttpPost getHTTPPostRequest(String restUri) {
		return (HttpPost) getHTTPRequest(restUri, HTTPMethod.POST);
	}

	private HttpDelete getHTTPDeleteRequest(String restUri) {
		return (HttpDelete) getHTTPRequest(restUri, HTTPMethod.DELETE);
	}

	private HttpRequestBase setHttpHeader(Map<String, String> headerMap, HttpRequestBase request) {
		headerMap.forEach((k, v) -> request.addHeader(k, v));
		// for (Map.Entry<String, String> entry : headerMap.entrySet()) {
		// request.addHeader(entry.getKey(), entry.getValue());
		// }
		return request;
	}

	private String sendHttpResponse(HttpRequestBase request) throws IOException {
		LOGGER.info("Entering sendHttpResponse at {}", System.currentTimeMillis());
		HttpResponse response = null;
		BufferedReader br = null;
		StringBuilder result = new StringBuilder();
		try {
			response = getHTTPClient().execute(request);

			if (response.getStatusLine() != null) {
				if (response.getStatusLine().getStatusCode() != 200) {
					throw handleHttpResponseStatus(response);
				}
				result.append(read(response.getEntity().getContent()));
			}
			closeHTTPRequest(request);// closing the request
		} catch (IOException e) {
			LOGGER.error("Failed :IOException", e);
			throw new BaseException("Failed :IOException", e);
		} finally {
			if (null != br) {
				br.close();
				br = null;
			}
			LOGGER.info("Exiting sendHttpResponse {}", System.currentTimeMillis());
		}
		return result.toString();
	}

	private BaseException handleHttpResponseStatus(HttpResponse response) {
		BaseException exp = null;
		switch (response.getStatusLine().getStatusCode()) {
		case 202:
			exp = new BaseException("RC_202", "Request accepted.");
			break;
		case 400:
			exp = new BaseException("RC_400", "Bad Request.");
			break;
		case 401:
			exp = new BaseException("RC_401", "Unauthorized.");
			break;
		case 403:
			exp = new BaseException("RC_403", "Forbidden.");
			break;
		case 404:
			exp = new BaseException("RC_404", "Not Found.");
			break;
		case 405:
			exp = new BaseException("RC_405", "Method not found.");
			break;
		case 408:
			exp = new BaseException("RC_408", "Request time out.");
			break;
		case 412:
			exp = new BaseException("RC_412", "Precondition failed.");
			break;
		case 415:
			exp = new BaseException("RC_415", "Unsupported media type.");
			break;
		case 500:
			exp = new BaseException("RC_500", "Server Exception.");
			break;
		case 503:
			exp = new BaseException("RC_503", "Service unavailable.");
			break;
		default:
			exp = new BaseException("RC_500", "Other Exceptation");
			break;
		}
		return exp;

	}

	private void closeHTTPRequest(HttpRequestBase httpRequest) throws ClientProtocolException, IOException {
		if (null != httpRequest) {
			httpRequest.releaseConnection();
		}
	}

	private String read(InputStream input) throws IOException {
		try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
			return buffer.lines().collect(Collectors.joining("\n"));
		}
	}

}
