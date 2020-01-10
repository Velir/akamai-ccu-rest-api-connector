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
	String completionTime
	String submittedBy
	String purgeStatus
	String submissionTime
	long pingAfterSeconds

	static PurgeStatus noStatus(){
		new PurgeStatus(httpStatus: -1, purgeStatus: "The request was not sent")
	}
}
