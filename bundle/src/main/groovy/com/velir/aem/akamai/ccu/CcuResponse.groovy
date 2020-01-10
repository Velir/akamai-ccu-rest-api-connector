package com.velir.aem.akamai.ccu

/**
 * Response - Shared info between purge and fast purge.
 * Used for JSON serialization for admin screen
 *
 * @author Kai Rasmussen
 */
interface CcuResponse {
	String getDetail()
	String getPurgeId()
	String getSupportId()
	int getEstimatedSeconds()
	int getHttpStatus()
}
