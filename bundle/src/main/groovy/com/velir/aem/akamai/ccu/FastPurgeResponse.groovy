package com.velir.aem.akamai.ccu

import static org.apache.http.HttpStatus.SC_CREATED

/**
 * FastPurgeResponse -
 *
 * @author Kai Rasmussen
 */
class FastPurgeResponse {
	String detail, purgeId, supportId
	int estimatedSeconds,  httpStatus

	static FastPurgeResponse noResponse() {
		new FastPurgeResponse(httpStatus: -1, detail: "Nothing has been sent because the query was not valid")
	}

	boolean isSuccess() {
		httpStatus == SC_CREATED
	}
}
