package com.velir.aem.akamai.ccu.impl

import com.day.cq.replication.ReplicationAction
import org.apache.felix.scr.annotations.Component
import org.apache.felix.scr.annotations.Property
import org.apache.felix.scr.annotations.Service
import org.apache.sling.commons.osgi.PropertiesUtil
import org.apache.sling.event.EventUtil
import org.apache.sling.event.jobs.JobManager
import org.osgi.service.component.ComponentContext
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
@Component(label = "Akamai Event Handler", description = "Listen to repository replication notification to invalidate Akamai cache when it is needed", metatype = true, immediate = true)
@org.apache.felix.scr.annotations.Properties(value = [
	@Property(name = EventConstants.EVENT_TOPIC, value = ReplicationAction.EVENT_TOPIC, label = "Event topic"),
	@Property(name = "pathsHandled", value = ["/content/dam"], label = "Handled paths")
])
class AkamaiEventHandler implements EventHandler {
	private final static Logger LOG = LoggerFactory.getLogger(AkamaiEventHandler.class)
	public static final String JOB_NAME = null // No default job name set


	@org.apache.felix.scr.annotations.Reference
	private JobManager jobManager;

	private Set<String> pathsHandled;

	@Override
	public void handleEvent(Event event) {
		if (EventUtil.isLocal(event)) {
			LOG.debug("Start handling event to add Akamai job")
			String[] paths = PropertiesUtil.toStringArray(event.getProperty(FlushAkamaiItemsJob.PATHS))
			Set<String> validPaths = filterValidPath(paths);

			if (!validPaths.isEmpty()) {
				jobManager.addJob(FlushAkamaiItemsJob.JOB_TOPIC, JOB_NAME, buildJobProperties(validPaths));
				LOG.debug("Akamai job Added")
			} else {
				LOG.debug("{} has no valid path(s) to purge. No Job added", paths)
			}
		}
	}

	private Set<String> filterValidPath(String[] paths) {
		Set<String> validPaths = new HashSet<>();
		for (String path : paths) {
			if (path) {
				for (String validPath : pathsHandled) {
					if (path.startsWith(validPath)) {
						validPaths.add(path)
					}
				}
			}
		}
		return validPaths
	}

	private static Map<String, Object> buildJobProperties(Set<String> paths) {
		Map<String, Object> jobProperties = new HashMap();
		jobProperties.put(FlushAkamaiItemsJob.PATHS, paths);
		return jobProperties;
	}

	@SuppressWarnings("GroovyUnusedDeclaration")
	protected void activate(ComponentContext context) {
		pathsHandled = new HashSet<String>()
		pathsHandled.addAll(PropertiesUtil.toStringArray(context.getProperties().get("pathsHandled")));
	}

	@SuppressWarnings("GroovyUnusedDeclaration")
	protected void deactivate() {
		pathsHandled = Collections.emptySet();
	}
}
