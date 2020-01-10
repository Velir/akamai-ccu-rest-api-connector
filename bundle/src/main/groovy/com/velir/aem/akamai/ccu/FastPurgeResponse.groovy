package com.velir.aem.akamai.ccu

/**
 * FastPurgeResponse -
 *
 * @author Kai Rasmussen
 */
class FastPurgeResponse implements CcuResponse {
	String detail, purgeId, supportId
	int estimatedSeconds,  httpStatus
}
