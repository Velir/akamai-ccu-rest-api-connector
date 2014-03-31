package com.velir.aem.akamai.ccu

/**
 * PurgeStatus -
 *
 * @author Sebastien Bernard
 */
class PurgeStatus {
	long originalEstimatedSeconds
	String progressUri
	int originalQueueLength
	String purgeId
	String supportId
	String httpStatus
	long completionTime
	String submittedBy
	String purgeStatus
	Date submissionTime
	long pingAfterSeconds
}
