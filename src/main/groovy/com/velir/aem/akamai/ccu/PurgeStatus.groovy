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
	int httpStatus
	long completionTime
	String submittedBy
	String purgeStatus
	String submissionTime
	long pingAfterSeconds

	public static PurgeStatus noStatus(){
		return new PurgeStatus(httpStatus: -1, purgeStatus: "The request was not sent")
	}
}
