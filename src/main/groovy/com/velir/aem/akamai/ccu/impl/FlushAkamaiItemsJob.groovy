package com.velir.aem.akamai.ccu.impl

import com.velir.aem.akamai.ccu.CcuManager
import com.velir.aem.akamai.ccu.PurgeResponse
import org.apache.felix.scr.annotations.Component
import org.apache.felix.scr.annotations.Property
import org.apache.felix.scr.annotations.Service
import org.apache.sling.api.SlingConstants
import org.apache.sling.event.jobs.Job
import org.apache.sling.event.jobs.consumer.JobConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * FlushAkamaiItemsJob -
 *
 * @author Sebastien Bernard
 */
@Component
@Service(value = [FlushAkamaiItemsJob, JobConsumer.class])
@Property(name = JobConsumer.PROPERTY_TOPICS, value = FlushAkamaiItemsJob.JOB_TOPIC)
class FlushAkamaiItemsJob implements JobConsumer {
	public static final String JOB_TOPIC = "com/velir/aem/akamai/ccu/impl/"
	private static final Logger LOG = LoggerFactory.getLogger(FlushAkamaiItemsJob.class)

	@org.apache.felix.scr.annotations.Reference
	private CcuManager ccuManager

	@Override
	JobConsumer.JobResult process(Job job) {
		Set<String> pathsToPurge = getPathsToPurge(job)
		if (pathsToPurge.isEmpty()) {
			LOG.warn("No path to process for FlushAkamaiItemsJob, canceling...")
			return JobConsumer.JobResult.CANCEL
		}

		LOG.debug("Starting process to purge Akamai caching {}.", pathsToPurge)

		PurgeResponse response = ccuManager.purgeByUrls(pathsToPurge)

		return convertToJobResult(response);
	}

	private static Set getPathsToPurge(Job job) {
		String pathsToInvalidate = job.getProperty(SlingConstants.PROPERTY_PATH)
		if (pathsToInvalidate == null) {
			LOG.error("The property {} is mandatory to execute the job", SlingConstants.PROPERTY_PATH)
			return Collections.emptySet()
		}
		Set results = new HashSet()
		results.add(pathsToInvalidate)
		return results;
	}

	static JobConsumer.JobResult convertToJobResult(PurgeResponse response) {
		return response.isSuccess() ? JobConsumer.JobResult.OK : JobConsumer.JobResult.FAILED;
	}
}
