package com.onelogin.sdk.conn;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthClientResponse;
import org.apache.oltu.oauth2.client.response.OAuthClientResponseFactory;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.utils.OAuthUtils;


public class OneloginURLConnectionClient extends URLConnectionClient {
		@SuppressWarnings("unchecked")
		public <T extends OAuthClientResponse> T execute(OAuthClientRequest request, Map<String, String> headers,
			String requestMethod, Class<T> responseClass) throws OAuthSystemException, OAuthProblemException {
		InputStream responseBody = null;

		Map responseHeaders = new HashMap();
		URLConnection c;
		int responseCode;
		try {
			URL url = new URL(request.getLocationUri());

			c = url.openConnection();
			responseCode = -1;
			if (c instanceof HttpURLConnection) {
				HttpURLConnection httpURLConnection = (HttpURLConnection) c;

				if ((headers != null) && (!(headers.isEmpty()))) {
					for (Map.Entry header : headers.entrySet()) {
						httpURLConnection.addRequestProperty((String) header.getKey(), (String) header.getValue());
					}
				}

				if (request.getHeaders() != null) {
					for (Map.Entry header : request.getHeaders().entrySet()) {
						httpURLConnection.addRequestProperty((String) header.getKey(), (String) header.getValue());
					}
				}

				if (OAuthUtils.isEmpty(requestMethod)) {
					httpURLConnection.setRequestMethod("GET");
				} else {
					httpURLConnection.setRequestMethod(requestMethod);
					setRequestBody(request, requestMethod, httpURLConnection);
				}

				httpURLConnection.connect();

				responseCode = httpURLConnection.getResponseCode();
				InputStream inputStream;
				//Modified line to extend the captured errors
				if ((responseCode == 400) || (responseCode == 401) || (responseCode == 403) || (responseCode == 404))
					inputStream = httpURLConnection.getErrorStream();
				else {
					inputStream = httpURLConnection.getInputStream();
				}

				responseHeaders = httpURLConnection.getHeaderFields();
				responseBody = inputStream;
			}
		} catch (IOException e) {
			throw new OAuthSystemException(e);
		}

		return (T) OAuthClientResponseFactory.createCustomResponse(responseBody, c.getContentType(), responseCode,
				responseHeaders, responseClass);
	}
	
	private void setRequestBody(OAuthClientRequest request, String requestMethod, HttpURLConnection httpURLConnection)
			throws IOException {
		String requestBody = request.getBody();
		if (OAuthUtils.isEmpty(requestBody)) {
			return;
		}

		if (("POST".equals(requestMethod)) || ("PUT".equals(requestMethod))) {
			httpURLConnection.setDoOutput(true);
			OutputStream ost = httpURLConnection.getOutputStream();
			PrintWriter pw = new PrintWriter(ost);
			pw.print(requestBody);
			pw.flush();
			pw.close();
		}
	}
}