package com.velir.aem.akamai.ccu.auth

import groovy.json.JsonOutput
import groovy.transform.builder.Builder
import groovyx.net.http.Method

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest

import static groovyx.net.http.Method.POST
import static javax.crypto.Mac.getInstance
import static org.apache.commons.codec.binary.Base64.encodeBase64String
import static org.apache.commons.lang.StringUtils.EMPTY
/**
 * AuthorizationBuilder - Responsible for translating a request into a signature
 *
 * @author Kai Rasmussen
 */

@Builder
class Signature {
	private static final String HMAC_ALG = "HmacSHA256"
	private static final String CHARSET = "UTF-8"
	public static final String MD_ALG = "SHA-256"

	String secret, auth, scheme, host, path, timestamp
	HashMap requestHeaders, postBody
	Method method

	String getSignature() {
		String signingKey = sign(timestamp, secret.getBytes(CHARSET))
		String toSign = "${canonicalRequest}${auth}"
		sign(toSign, signingKey.getBytes(CHARSET))
	}

	private String getCanonicalRequest(){
		"${method.toString()}\t${scheme}\t${host}\t${path}\t${canonicalizeHeaders}\t${contentHash}\t"
	}

	private String getCanonicalizeHeaders(){
		requestHeaders?requestHeaders.inject(''){ str, key, value ->
			value = (value.trim() =~ /s+/).replaceAll(' ')
			if(value){
				str += "${key.toLowerCase()}:${value}\t"
			}
		} : EMPTY
	}

	private String getContentHash(){
		String hash = EMPTY
		if(method == POST && postBody){
			String body = JsonOutput.toJson(postBody)
			MessageDigest md = MessageDigest.getInstance(MD_ALG)
			byte[] bytes = body.bytes
			md.update(bytes, 0, bytes.length)
			byte[] digest = md.digest()
			hash = encodeBase64String(digest)
		}
		hash
	}

	private static String sign(String s, byte[] key) {
		SecretKeySpec signingKey = new SecretKeySpec(key, HMAC_ALG)
		Mac mac = getInstance(HMAC_ALG)
		mac.init(signingKey)
		byte[] valueBytes = s.getBytes(CHARSET)
		byte[] bytes = mac.doFinal(valueBytes)
		encodeBase64String(bytes)
	}
}
