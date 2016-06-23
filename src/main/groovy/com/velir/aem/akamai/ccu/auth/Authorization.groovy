package com.velir.aem.akamai.ccu.auth

import com.velir.aem.akamai.ccu.Credentials
import com.velir.aem.akamai.ccu.Timestamp
import groovy.transform.builder.Builder
import groovyx.net.http.Method

import static java.util.UUID.randomUUID

/**
 * Authorization -
 *
 * @author Kai Rasmussen
 */
@Builder
class Authorization {
	Credentials credentials
	String path, rootCcuUrl
	HashMap body, headers
	Method method

	String getAuthorization(){
		String timeStamp = use(Timestamp){ new Date().timestamp }
		String nonce = randomUUID().toString()
		String unsignedAuth = "EG1-HMAC-SHA256 client_token=${credentials.clientToken};access_token=${credentials.accessToken};timestamp=${timeStamp};nonce=${nonce};"
		String signedAuth = signAuth(path, unsignedAuth, timeStamp, body, method, headers)
		"${unsignedAuth}signature=${signedAuth}"
	}

	private String signAuth(String path, String auth, String timestamp, HashMap body, Method method, HashMap headers) {
		Signature sigBuilder = Signature.builder()
										.secret(credentials.clientSecret).scheme("https").path(path)
										.timestamp(timestamp).host(rootCcuUrl.replaceFirst("https://", ""))
										.requestHeaders(headers).postBody(body).method(method).auth(auth)
										.build()
		sigBuilder.signature
	}
}
