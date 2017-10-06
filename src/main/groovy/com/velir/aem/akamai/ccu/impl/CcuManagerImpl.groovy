package com.velir.aem.akamai.ccu.impl

import com.velir.aem.akamai.ccu.*
import com.velir.aem.akamai.ccu.auth.Authorization
import groovyx.net.http.AsyncHTTPBuilder
import groovyx.net.http.Method
import org.apache.felix.scr.annotations.*
import org.apache.sling.commons.osgi.PropertiesUtil
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.security.InvalidParameterException
import java.util.concurrent.Future

import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST
import static org.apache.commons.lang.StringUtils.EMPTY
import static org.apache.sling.commons.osgi.PropertiesUtil.toString
/**
 * CcuManagerImpl -
 *
 * @author Sebastien Bernard
 */
@Component(label = "Akamai CCU REST API Manager", description = "Manage calls to the Akamai CCU REST API", metatype = true, immediate = true, policy = ConfigurationPolicy.REQUIRE)
@Service(value = [CcuManager])
@Properties([
	@Property(name = "clientToken", description = "", label = "Client Token"),
	@Property(name = "clientSecret", description = "", label = "Client Secret", passwordValue = ""),
	@Property(name= "accessToken", description = "", label="Access Token"),
	@Property(name = "threadPoolSize", description= "Number of threads for concurrent HTTP connections", longValue = 5l)
])
class CcuManagerImpl implements CcuManager {
	private static final Logger LOG = LoggerFactory.getLogger(CcuManagerImpl)
	private static final String DEFAULT_CCU_URL = "https://api.ccu.akamai.com"
	static final PurgeAction DEFAULT_PURGE_ACTION = PurgeAction.REMOVE
	static final PurgeDomain DEFAULT_PURGE_DOMAIN = PurgeDomain.PRODUCTION
	static final String AUTHORIZATION = 'Authorization'
	static final String QUEUES_PATH = "/ccu/v2/queues/default"
	private static final String FAST_PURGE_URI_BASE =  '/ccu/v3/%1$s/%2$s/%3$s'
	static final String UTF_8 = "UTF-8"
	private static final Closure VAL_NOT_NULL = { key, value -> value }
	private static final int DEFAULT_POOL_SIZE = 5

	@Property(name = "rootCcuUrl", label = "Akamai CCU API URL", value = "https://api.ccu.akamai.com")
	private String rootCcuUrl
	@Property(name = "defaultPurgeAction", label = "Default purge action", description = "Can be invalidate, remove (default)", value = "remove")
	private PurgeAction defaultPurgeAction
	@Property(name = "defaultPurgeDomain", label = "Default purge domain", description = "Can be staging, production (default)", value = "production")
	private PurgeDomain defaultPurgeDomain

	private Credentials credentials

	private AsyncHTTPBuilder asyncHTTPBuilder

	/**
	 * {@inheritDoc}
	 */
	@Override
	PurgeResponse purgeByUrl(String url) {
		purgeByUrls([url])
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	PurgeResponse purgeByUrls(Collection<String> urls) {
		purge(urls, PurgeType.ARL, defaultPurgeAction, defaultPurgeDomain)
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	PurgeResponse purgeByCpCode(String cpCode) {
		purgeByCpCodes([cpCode])
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	PurgeResponse purgeByCpCodes(Collection<String> cpCodes) {
		purge(cpCodes, PurgeType.CPCODE, defaultPurgeAction, defaultPurgeDomain)
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	PurgeResponse purge(Collection<String> objects, PurgeType purgeType, PurgeAction purgeAction, PurgeDomain purgeDomain) {
		Collection<String> uniqueObjects = removeDuplicate(objects)
		if (!uniqueObjects) {
			LOG.warn("No objects to invalidate")
			return PurgeResponse.noResponse()
		}

		logDebug(purgeType, purgeAction, purgeDomain, uniqueObjects)
		HashMap postBody = [
			type   : purgeType.name().toLowerCase(),
			action : purgeAction.name().toLowerCase(),
			domain : purgeDomain.name().toLowerCase(),
			objects: uniqueObjects,
		]
		Future response = asyncHTTPBuilder.request(POST, JSON) {
			uri.path = QUEUES_PATH
			headers[AUTHORIZATION] = getAuth(QUEUES_PATH, postBody, POST, headers as HashMap)
			body = postBody
		}
		def json = response.get()
		PurgeResponse purgeResponse = new PurgeResponse(json.findAll(VAL_NOT_NULL))
		LOG.debug("Response {}", purgeResponse)
		purgeResponse
	}

	private String getAuth(String path, HashMap body, Method method, HashMap headers) {
		Authorization.builder()
					 .credentials(credentials).path(path).body(body)
					 .method(method).headers(headers).rootCcuUrl(rootCcuUrl)
					 .build().authorization
	}

	private static void logDebug(PurgeType purgeType, PurgeAction purgeAction, PurgeDomain purgeDomain, Collection<String> uniqueObjects) {
		if(LOG.isDebugEnabled()){
			LOG.debug("Request:")
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
	private static Collection<String> removeDuplicate(Collection<String> objects) {
		objects.removeAll([null])
		objects.unique()
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	PurgeStatus getPurgeStatus(String progressUri) {
		if (!progressUri) {
			return PurgeStatus.noStatus()
		}

		Future response = asyncHTTPBuilder.request(GET, JSON){
			uri.path = progressUri
			headers[AUTHORIZATION] = getAuth(progressUri, [:] as HashMap, GET, headers as HashMap)
		}
		def json = response.get()
		LOG.debug("purge response {}", json)
		new PurgeStatus(json.findAll(VAL_NOT_NULL))
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	QueueStatus getQueueStatus() {
		Future response = asyncHTTPBuilder.request(GET, JSON){
			uri.path = QUEUES_PATH
			headers[AUTHORIZATION] = getAuth(QUEUES_PATH, [:] as HashMap, GET, headers as HashMap)
		}
		def json = response.get()
		LOG.debug("Queue status response {}", json)
		new QueueStatus(json.findAll(VAL_NOT_NULL))
	}

	@Override
	FastPurgeResponse fastPurge(Collection<String> objects, FastPurgeType type) {
		fastPurge(objects, type, defaultPurgeAction, defaultPurgeDomain)
	}

	@Override
	FastPurgeResponse fastPurge(Collection<String> objects, FastPurgeType type, PurgeAction purgeAction, PurgeDomain purgeDomain) {
		String purgeUri = sprintf(FAST_PURGE_URI_BASE, [purgeAction.apiVal, type.name().toLowerCase(), purgeDomain.name().toLowerCase()])
		Map postBody = [objects : objects]
		Future future = asyncHTTPBuilder.request(POST, JSON){
			uri.path = purgeUri
			headers[AUTHORIZATION] = getAuth(purgeUri, postBody as HashMap, POST, headers as HashMap)
			body = postBody
		}
		new FastPurgeResponse(future.get())
	}

	@Activate
	protected void activate(ComponentContext context) {
		Dictionary props = context.properties
		this.credentials = new Credentials()
		setRootCcuUrl(toString(props.get("rootCcuUrl"), EMPTY))
		setClientToken(toString(props.get("clientToken"), EMPTY))
		setClientSecret(toString(props.get("clientSecret"), EMPTY))
		setDefaultPurgeAction(toString(props.get("defaultPurgeAction"), EMPTY))
		setDefaultPurgeDomain(toString(props.get("defaultPurgeDomain"), EMPTY))
		setAccessToken(toString(props.get("accessToken"), EMPTY))
		long poolSize = PropertiesUtil.toLong(props.get("threadPoolSize"), DEFAULT_POOL_SIZE)
		asyncHTTPBuilder = new AsyncHTTPBuilder(poolSize: poolSize, uri: rootCcuUrl, contentType: JSON)
		asyncHTTPBuilder.contentEncoding =  UTF_8
	}

	@Deactivate
	protected void deactivate() {
		asyncHTTPBuilder = null
		credentials = null
		defaultPurgeAction = null
		defaultPurgeDomain = null
	}

	private void setRootCcuUrl(String rootCcuUrl) {
		this.rootCcuUrl = rootCcuUrl?:DEFAULT_CCU_URL
	}

	private void setClientToken(String clientToken) {
		if (!clientToken) {
			throw new InvalidParameterException("The Client Token is mandatory")
		}
		this.credentials.clientToken = clientToken
	}

	private void setClientSecret(String clientSecret) {
		if (!clientSecret) {
			throw new InvalidParameterException("The Client Secret is mandatory")
		}
		this.credentials.clientSecret = clientSecret
	}

	private void setAccessToken(String accessToken) {
		if (!accessToken) {
			throw new InvalidParameterException("The Access token is mandatory")
		}
		this.credentials.accessToken = accessToken
	}


	private void setDefaultPurgeAction(String purgeAction) {
		this.defaultPurgeAction = purgeAction? PurgeAction.valueOf(purgeAction.toUpperCase()) : DEFAULT_PURGE_ACTION
	}

	private void setDefaultPurgeDomain(String purgeDomain) {
		this.defaultPurgeDomain = purgeDomain? PurgeDomain.valueOf(purgeDomain.toUpperCase()) : DEFAULT_PURGE_DOMAIN
	}
}
