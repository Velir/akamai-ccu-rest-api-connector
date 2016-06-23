package com.velir.aem.akamai.ccu

import groovy.transform.ToString

import static org.apache.http.HttpStatus.SC_CREATED
/**
 * PurgeStatus -
 *
 * @author Sebastien Bernard
 */
@ToString
class PurgeResponse {
	int httpStatus
	String detail
	long estimatedSeconds
	String purgeId
	String progressUri
	long pingAfterSeconds
	String supportId

	static PurgeResponse noResponse() {
		new PurgeResponse(httpStatus: -1, detail: "Nothing has been sent because the query was not valid")
	}

	boolean isSuccess() {
		httpStatus == SC_CREATED
	}
}
