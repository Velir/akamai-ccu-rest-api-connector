package com.velir.aem.akamai.ccu.impl

import org.apache.felix.scr.annotations.Component
import org.apache.felix.scr.annotations.Property
import org.apache.felix.scr.annotations.Service
import org.apache.sling.api.SlingConstants
import org.apache.sling.event.EventUtil
import org.apache.sling.event.jobs.JobManager
import org.osgi.service.event.Event
import org.osgi.service.event.EventConstants
import org.osgi.service.event.EventHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * AkamaiEventHandler - Listen to repository replication notification to invalidate Akamai cache when it is needed
 *
 * @author Sebastien Bernard
 */
@Service(value = [AkamaiEventHandler.class, EventHandler.class])
@Component(label = "Akamai event handler", description = "Listen to repository replication notification to invalidate Akamai cache when it is needed", metatype = true)
@Property(name = EventConstants.EVENT_TOPIC, value = [SlingConstants.TOPIC_RESOURCE_ADDED, SlingConstants.TOPIC_RESOURCE_CHANGED, SlingConstants.TOPIC_RESOURCE_REMOVED])
class AkamaiEventHandler implements EventHandler {
	private final static Logger LOG = LoggerFactory.getLogger(AkamaiEventHandler.class)
	public static final String JOB_NAME = null; // No default job name set

	@org.apache.felix.scr.annotations.Reference
	private JobManager jobManager;

	@org.apache.felix.scr.annotations.Property(name = "validPaths", value = ["/content/dam"])
	private Set<String> validPaths;

	@SuppressWarnings("GroovyUnusedDeclaration")
	void setValidPaths(String[] validPaths) {
		this.validPaths = validPaths
	}

	@Override
	public void handleEvent(Event event) {
		if (EventUtil.isLocal(event)) {
			LOG.debug("Start adding Akamai job")
			def path = event.getProperty(SlingConstants.PROPERTY_PATH)

			if (valid(path)) {
				jobManager.addJob(FlushAkamaiItemsJob.JOB_TOPIC, JOB_NAME, buildJobProperties(path));
			} else {
				LOG.debug("{} is not a valid path to purge", path)
			}
			LOG.debug("Akamai job Added")
		}
	}

	boolean valid(String path) {
		if (!path) {
			return false
		}

		if (!validPaths) {
			return true
		}

		for (String validPath : validPaths) {
			if (path.startsWith(validPath)) {
				return true
			}
		}
		return false
	}

	private static Map<String, Object> buildJobProperties(String path) {
		Map<String, Object> jobProperties = new HashMap();
		jobProperties.put(FlushAkamaiItemsJob.PATH_PROPERTY_NAME, path);
		return jobProperties;
	}

}
