package com.velir.aem.akamai.ccu.impl

import com.velir.aem.akamai.ccu.CcuManager
import com.velir.aem.akamai.ccu.FastPurgeResponse
import com.velir.aem.akamai.ccu.FastPurgeType
import com.velir.aem.akamai.ccu.PurgeResponse
import org.apache.felix.scr.annotations.Component
import org.apache.felix.scr.annotations.Property
import org.apache.felix.scr.annotations.Service
import org.apache.sling.commons.osgi.PropertiesUtil
import org.apache.sling.event.jobs.Job
import org.apache.sling.event.jobs.consumer.JobConsumer
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * FlushAkamaiItemsJob -
 *
 * @author Sebastien Bernard
 */
@Component(label = "Akamai Flush Job", description = "Job that execute Akamai flush using Akamai CCU manager", metatype = true, immediate = true)
@Service(value = [FlushAkamaiItemsJob, JobConsumer])
@org.apache.felix.scr.annotations.Properties(value = [
	@Property(name = JobConsumer.PROPERTY_TOPICS, value = FlushAkamaiItemsJob.JOB_TOPIC),
	@Property(name = "rootSiteUrl", value = "", label = "Root site url", description = "Scheme and domain to append at the beginning of the paths like http://www.velir.com"),
	@Property(name = "useFastPurge", boolValue = false, label = "Use fast purge?", description = "Check to use the fast purge option for automatic flushes")
])
class FlushAkamaiItemsJob implements JobConsumer {
	private static final Logger LOG = LoggerFactory.getLogger(FlushAkamaiItemsJob)
	public static final String JOB_TOPIC = "com/velir/aem/akamai/ccu/impl/FlushAkamaiItemsJob"
	public static final String PATHS = "paths"

	@org.apache.felix.scr.annotations.Reference
	private CcuManager ccuManager

	private String rootSiteUrl

	private boolean useFastPurge = false

	@Override
	JobConsumer.JobResult process(Job job) {
		Set<String> pathsToPurge = getPathsToPurge(job)
		LOG.debug("Start processing job to purge Akamai cache")
		if (pathsToPurge.isEmpty()) {
			LOG.warn("No path to process, canceling...")
			return JobConsumer.JobResult.CANCEL
		}

		Set<String> absoluteUrls = prependPathWithRootUrl(pathsToPurge)
		logUrls(absoluteUrls)
		JobConsumer.JobResult result = purge(absoluteUrls)
		result
	}

	private JobConsumer.JobResult purge(Set<String> absoluteUrls) {
		JobConsumer.JobResult result
		if (!useFastPurge) {
			PurgeResponse response = ccuManager.purgeByUrls(absoluteUrls)
			result = convertToJobResult(response)
		} else {
			FastPurgeResponse purge = ccuManager.fastPurge(absoluteUrls, FastPurgeType.URL)
			result = purge.httpStatus == 201 ? JobConsumer.JobResult.OK : JobConsumer.JobResult.FAILED
		}
		result
	}

	private static logUrls(Set<String> pathsToPurge) {
		if (LOG.isInfoEnabled()) {
			LOG.info("Path(s) to purge:")
			for (path in pathsToPurge) {
				LOG.info(path)
			}
		}
	}

	private Set<String> prependPathWithRootUrl(Collection<String> paths) {
		if (!rootSiteUrl) {
			return paths
		}

		Set<String> urls = new HashSet<String>(paths.size())
		for (path in paths) {
			if (!path.startsWith(rootSiteUrl)) {
				urls.add(rootSiteUrl.concat(path))
			}
		}

		urls
	}

	private static Set getPathsToPurge(Job job) {
		String[] pathsToInvalidate = PropertiesUtil.toStringArray(job.getProperty(PATHS))
		if (pathsToInvalidate == null) {
			LOG.error("The property {} is mandatory to execute the job", PATHS)
			return Collections.emptySet()
		}
		Set results = new HashSet()
		results.addAll(pathsToInvalidate)
		results
	}

	static JobConsumer.JobResult convertToJobResult(PurgeResponse response) {
		response.isSuccess() ? JobConsumer.JobResult.OK : JobConsumer.JobResult.FAILED
	}

	public void activate(ComponentContext context) {
		Dictionary props = context.properties
		rootSiteUrl = PropertiesUtil.toString(props.get("rootSiteUrl"), "")
		useFastPurge = PropertiesUtil.toBoolean(props.get("useFastPurge"), false)
	}
}
