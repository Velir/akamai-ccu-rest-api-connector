package com.velir.aem.akamai.ccu.auth

import com.velir.aem.akamai.ccu.Credentials
import spock.lang.Specification

import static groovyx.net.http.Method.POST

/**
 * AuthorizationTest -
 *
 * @author Kai Rasmussen
 */
class AuthorizationTest extends Specification {

	def "Test Authorization"(){
		when: "Create Auth"
		Credentials credentials = new Credentials(clientSecret: "secret", accessToken: "accessToken", clientToken: "clientToken")
		Authorization authorization =
				Authorization.builder()
				.credentials(credentials)
				.path("/path").rootCcuUrl("https://www.akamai.com")
				.body([foo: 'bar']).headers([bar : 'foo'])
				.method(POST).build()

		then:
		authorization.authorization.startsWith("EG1-HMAC-SHA256 client_token=clientToken;access_token=accessToken;timestamp=")
	}

	def "Test Signature"(){
		when:
		Signature signature = Signature.builder().method(POST).path("/path")
		.secret("secret").host("host").auth("auth").scheme("https").timestamp("timestamp")
		.requestHeaders([foo: 'bar']).postBody([bar : 'foo']).build()
		then:
		signature.signature == "TepJ/qCnZtaHHPr5eRcp88udOHyZZmY/XwcS+VCnd3I="
	}

}