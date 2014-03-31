package com.velir.aem.akamai.ccu

import groovyx.net.http.AsyncHTTPBuilder
import org.apache.felix.scr.annotations.*
import org.osgi.service.component.ComponentContext

import java.security.InvalidParameterException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * CcuManagerImpl -
 *
 * @author Sebastien Bernard
 */
@Component(immediate = false, policy = ConfigurationPolicy.REQUIRE)
@Service(value = [CcuManager.class])
class CcuManagerImpl implements CcuManager {
	private static final String DEFAULT_CCU_URL = "https://api.ccu.akamai.com"
	public static final PurgeAction DEFAULT_PURGE_ACTION = PurgeAction.REMOVE
	public static final PurgeDomain DEFAULT_PURGE_DOMAIN = PurgeDomain.PRODUCTION
	public static final String CONTENT_TYPE = "application/json"

	@Property(name="rootCcuUrl", label = "Akamai CCU API URL", value = "https://api.ccu.akamai.com")
	private String rootCcuUrl;
	@Property(name = "userName", label = "Username")
	private String userName;
	@Property(name="password", label = "Password")
	private String password;
	@Property(name="defaultPurgeAction", label = "Default purge action", description = "Can be invalidate, remove (default)", value = "remove")
	private PurgeAction defaultPurgeAction;
	@Property(name="defaultPurgeDomain", label = "Default purge domain", description = "Can be staging, production (default)", value = "production")
	private PurgeDomain defaultPurgeDomain;

	private AsyncHTTPBuilder httpBuilder;

	@Override
	public PurgeResponse purgeByUrl(String url) {
		purgeByUrls([url]);
	}

	@Override
	public PurgeResponse purgeByUrls(Collection<String> urls) {
		return purge(urls, PurgeType.ARL, defaultPurgeAction, defaultPurgeDomain);
	}

	@Override
	public PurgeResponse purgeByCpCode(String cpCode) {
		return purgeByCpCodes([cpCode]);
	}

	@Override
	public PurgeResponse purgeByCpCodes(Collection<String> cpCode) {
		return purge(cpCode, PurgeType.CPCODE, defaultPurgeAction, defaultPurgeDomain);
	}

	@Override
	public PurgeResponse purge(Collection<String> objets, PurgeType purgeType, PurgeAction purgeAction, PurgeDomain purgeDomain) {
		LinkedHashSet<String> uniqueObjects = removeDuplicate(objets)
		if (uniqueObjects.isEmpty()) {
			return null; //TO IMPROVE
		}

		Future result = httpBuilder.post(
			path: "/ccu/v2/queues/default",
			requestContentType: CONTENT_TYPE,
			body: [
				type: purgeType.name().toLowerCase(),
				action: purgeAction.name().toLowerCase(),
				domain: purgeDomain.name().toLowerCase(),
				objects: uniqueObjects,
			]) { resp, json -> return new PurgeResponse(json) }

		return result.get();
	}

	private LinkedHashSet<String> removeDuplicate(Collection<String> urls) {
		return new LinkedHashSet(urls)
	}

	@Override
	public PurgeStatus getPurgeStatus(String progressUri) {
		Future result = httpBuilder.get(
			path: progressUri,
			requestContentType: CONTENT_TYPE
		)
			{ resp, json -> return new PurgeStatus(json) }

		return result.get();
	}

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
