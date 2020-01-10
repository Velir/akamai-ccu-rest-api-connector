package com.velir.aem.akamai.ccu

import org.osgi.framework.BundleActivator
import org.osgi.framework.BundleContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Activator -
 *
 * @author Sebastien Bernard
 */
class Activator implements BundleActivator {
	private static final Logger LOG = LoggerFactory.getLogger(Activator)

	void start(final BundleContext context) throws Exception {
		LOG.info(context.getBundle().getSymbolicName() + " started")
	}

	void stop(final BundleContext context) throws Exception {
		LOG.info(context.getBundle().getSymbolicName() + " stopped")
	}
}
