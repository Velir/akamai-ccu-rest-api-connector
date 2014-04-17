package com.velir.aem.akamai.ccu

/**
 * PurgeStatus -
 *
 * @author Sebastien Bernard
 */
class PurgeResponse {
	int httpStatus
	String detail
	long estimatedSeconds
	String purgeId
	String progressUri
	long pingAfterSeconds
	String supportId

	public static PurgeResponse noResponse(){
		return new PurgeResponse(httpStatus: -1, detail: "Nothing has been sent because the query was not valid")
	}

	boolean isSuccess(){
		return httpStatus == 201
	}
}
