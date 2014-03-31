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
}
