package com.velir.aem.akamai.ccu.impl

import java.security.InvalidParameterException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

import com.velir.aem.akamai.ccu.CcuManager
import com.velir.aem.akamai.ccu.PurgeAction
import com.velir.aem.akamai.ccu.PurgeDomain
import com.velir.aem.akamai.ccu.PurgeResponse
import com.velir.aem.akamai.ccu.PurgeStatus
import com.velir.aem.akamai.ccu.PurgeType
import com.velir.aem.akamai.ccu.QueueStatus
import groovyx.net.http.AsyncHTTPBuilder
import org.apache.felix.scr.annotations.Activate
import org.apache.felix.scr.annotations.Component
import org.apache.felix.scr.annotations.ConfigurationPolicy
import org.apache.felix.scr.annotations.Deactivate
import org.apache.felix.scr.annotations.Property
import org.apache.felix.scr.annotations.Service
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * CcuManagerImpl -
 *
 * @author Sebastien Bernard
 */
@Component(label = "Akamai CCU REST API Manager", description = "Manage calls to the Akamai CCU REST API", metatype = true, immediate = true, policy = ConfigurationPolicy.REQUIRE)
@Service(value = [CcuManager.class])
class CcuManagerImpl implements CcuManager {
	private static final Logger LOG = LoggerFactory.getLogger(CcuManagerImpl.class)
	private static final String DEFAULT_CCU_URL = "https://api.ccu.akamai.com"
	public static final PurgeAction DEFAULT_PURGE_ACTION = PurgeAction.REMOVE
	public static final PurgeDomain DEFAULT_PURGE_DOMAIN = PurgeDomain.PRODUCTION
	public static final String CONTENT_TYPE = "application/json"

	@Property(name = "rootCcuUrl", label = "Akamai CCU API URL", value = "https://api.ccu.akamai.com")
	private String rootCcuUrl;
	@Property(name = "userName", label = "Username")
	private String userName;
	@Property(name = "password", label = "Password")
	private String password;
	@Property(name = "defaultPurgeAction", label = "Default purge action", description = "Can be invalidate, remove (default)", value = "remove")
	private PurgeAction defaultPurgeAction;
	@Property(name = "defaultPurgeDomain", label = "Default purge domain", description = "Can be staging, production (default)", value = "production")
	private PurgeDomain defaultPurgeDomain;

	private AsyncHTTPBuilder httpBuilder;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PurgeResponse purgeByUrl(String url) {
		purgeByUrls([url]);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PurgeResponse purgeByUrls(Collection<String> urls) {
		return purge(urls, PurgeType.ARL, defaultPurgeAction, defaultPurgeDomain);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PurgeResponse purgeByCpCode(String cpCode) {
		return purgeByCpCodes([cpCode]);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PurgeResponse purgeByCpCodes(Collection<String> cpCodes) {
		return purge(cpCodes, PurgeType.CPCODE, defaultPurgeAction, defaultPurgeDomain);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PurgeResponse purge(Collection<String> objets, PurgeType purgeType, PurgeAction purgeAction, PurgeDomain purgeDomain) {
		LinkedHashSet<String> uniqueObjects = removeDuplicate(objets)
		if (!uniqueObjects) {
			LOG.warn("No objects to invalidate")
			return PurgeResponse.noResponse();
		}

		logDebug(purgeType, purgeAction, purgeDomain, uniqueObjects)

		Future result = httpBuilder.post(
			path: "/ccu/v2/queues/default",
			requestContentType: CONTENT_TYPE,
			body: [
				type   : purgeType.name().toLowerCase(),
				action : purgeAction.name().toLowerCase(),
				domain : purgeDomain.name().toLowerCase(),
				objects: uniqueObjects,
			]) { resp, json -> return new PurgeResponse(json) }

		return result.get();
	}

	private void logDebug(PurgeType purgeType, PurgeAction purgeAction, PurgeDomain purgeDomain, LinkedHashSet<String> uniqueObjects) {
		if(LOG.isDebugEnabled()){
			LOG.debug("Type: {}", purgeType)
			LOG.debug("Action: {}", purgeAction)
			LOG.debug("Domain: {}", purgeDomain)
			LOG.debug("objects: {}", uniqueObjects)
		}
	}

	/**
	 * Remove null and duplicates but keep given ordering
	 * @param objects the list of objects
	 * @return a ordered set of objects
	 */
	private LinkedHashSet<String> removeDuplicate(Collection<String> objects) {
		objects.removeAll([null])
		return new LinkedHashSet(objects)
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PurgeStatus getPurgeStatus(String progressUri) {
		if (!progressUri) {
			return PurgeStatus.noStatus();
		}

		Future result = httpBuilder.get(
			path: progressUri,
			requestContentType: CONTENT_TYPE
		) { resp, json -> return new PurgeStatus(json) }

		return result.get();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public QueueStatus getQueueStatus() {
		Future result = httpBuilder.get(
			path: "/ccu/v2/queues/default",
			requestContentType: CONTENT_TYPE
		) { resp, json -> return new QueueStatus(json) }

		return result.get();
	}

	@Activate
	protected void activate(ComponentContext context) {
		setRootCcuUrl(context.getProperties().get("rootCcuUrl"))
		setUserName(context.getProperties().get("userName"))
		setPassword(context.getProperties().get("password"))
		setDefaultPurgeAction(context.getProperties().get("defaultPurgeAction"))
		setDefaultPurgeDomain(context.getProperties().get("defaultPurgeDomain"))

		httpBuilder = new AsyncHTTPBuilder(
			timeout: TimeUnit.SECONDS.toMillis(5).toInteger(),
			poolSize: 5,
			uri: rootCcuUrl,
		)
		httpBuilder.setContentEncoding("utf-8")
		httpBuilder.auth.basic userName, password
	}

	@Deactivate
	protected void deactivate() {
		httpBuilder = null
		userName = null
		password = null
		defaultPurgeAction = null
		defaultPurgeDomain = null
	}

	private void setRootCcuUrl(String rootCcuUrl) {
		if (rootCcuUrl) {
			this.rootCcuUrl = rootCcuUrl
		} else {
			this.rootCcuUrl = DEFAULT_CCU_URL;
		}
	}

	private void setUserName(String userName) {
		if (!userName) {
			throw InvalidParameterException("The username is mandatory");
		}
		this.userName = userName
	}

	private void setPassword(String password) {
		if (!password) {
			throw InvalidParameterException("The username is mandatory");
		}
		this.password = password
	}

	private void setDefaultPurgeAction(String purgeAction) {
		if (purgeAction) {
			this.defaultPurgeAction = PurgeAction.valueOf(purgeAction.toUpperCase())
		} else {
			this.defaultPurgeAction = DEFAULT_PURGE_ACTION
		}

	}

	private void setDefaultPurgeDomain(String purgeDomain) {
		if (purgeDomain) {
			this.defaultPurgeDomain = PurgeDomain.valueOf(purgeDomain.toUpperCase())
		} else {
			this.defaultPurgeDomain = DEFAULT_PURGE_DOMAIN;
		}
	}
}
