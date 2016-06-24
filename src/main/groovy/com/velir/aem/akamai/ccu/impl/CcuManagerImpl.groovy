package com.velir.aem.akamai.ccu.impl

import com.velir.aem.akamai.ccu.CcuManager
import com.velir.aem.akamai.ccu.Credentials
import com.velir.aem.akamai.ccu.PurgeAction
import com.velir.aem.akamai.ccu.PurgeDomain
import com.velir.aem.akamai.ccu.PurgeResponse
import com.velir.aem.akamai.ccu.PurgeStatus
import com.velir.aem.akamai.ccu.PurgeType
import com.velir.aem.akamai.ccu.QueueStatus
import com.velir.aem.akamai.ccu.auth.Authorization
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.apache.felix.scr.annotations.*
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.security.InvalidParameterException

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
	@Property(name= "accessToken", description = "", label="Access Token")
])
class CcuManagerImpl implements CcuManager {
	private static final Logger LOG = LoggerFactory.getLogger(CcuManagerImpl)
	private static final String DEFAULT_CCU_URL = "https://api.ccu.akamai.com"
	static final PurgeAction DEFAULT_PURGE_ACTION = PurgeAction.REMOVE
	static final PurgeDomain DEFAULT_PURGE_DOMAIN = PurgeDomain.PRODUCTION
	static final String AUTHORIZATION = 'Authorization'
	static final String QUEUES_PATH = "/ccu/v2/queues/default"
	static final String UTF_8 = "UTF-8"
	private static final Closure VAL_NOT_NULL = { key, value -> value }

	@Property(name = "rootCcuUrl", label = "Akamai CCU API URL", value = "https://api.ccu.akamai.com")
	private String rootCcuUrl
	@Property(name = "defaultPurgeAction", label = "Default purge action", description = "Can be invalidate, remove (default)", value = "remove")
	private PurgeAction defaultPurgeAction
	@Property(name = "defaultPurgeDomain", label = "Default purge domain", description = "Can be staging, production (default)", value = "production")
	private PurgeDomain defaultPurgeDomain

	private Credentials credentials

	private HTTPBuilder httpBuilder

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
		PurgeResponse purgeResponse = null
		HashMap postBody = [
			type   : purgeType.name().toLowerCase(),
			action : purgeAction.name().toLowerCase(),
			domain : purgeDomain.name().toLowerCase(),
			objects: uniqueObjects,
		]
		httpBuilder.request(POST, JSON){
			uri.path = QUEUES_PATH
			headers[AUTHORIZATION] = getAuth(QUEUES_PATH, postBody, POST, headers as HashMap)
			body = postBody
			response.success = { resp, json ->
				purgeResponse = new PurgeResponse(json.findAll(VAL_NOT_NULL))
			}
			response.failure = { resp, json ->
				throw new RuntimeException("Error purging ${resp.status} ${json.detail}")
			}
		}
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
		PurgeStatus purgeStatus = null
		httpBuilder.request(GET, JSON){
			uri.path = progressUri
			headers[AUTHORIZATION] = getAuth(progressUri, [:] as HashMap, GET, headers as HashMap)
			response.success = { resp, json ->
				purgeStatus = new PurgeStatus(json.findAll(VAL_NOT_NULL))
			}
			response.failure = { resp, json ->
				LOG.error("Error getting status", json)
			}
		}

		purgeStatus
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	QueueStatus getQueueStatus() {
		QueueStatus queueStatus = null
		httpBuilder.request(GET, JSON){
			uri.path = QUEUES_PATH
			headers[AUTHORIZATION] = getAuth(QUEUES_PATH, [:] as HashMap, GET, headers as HashMap)
			response.success = { resp, json ->
				queueStatus = new QueueStatus(json.findAll(VAL_NOT_NULL))
			}
			response.failure = { resp, json ->
				LOG.error("Error getting status", json)
			}
		}

		queueStatus
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
		httpBuilder = new HTTPBuilder(rootCcuUrl)
		httpBuilder.contentEncoding =  UTF_8
	}

	@Deactivate
	protected void deactivate() {
		httpBuilder = null
		credentials = null
		defaultPurgeAction = null
		defaultPurgeDomain = null
	}

	private void setRootCcuUrl(String rootCcuUrl) {
		if (rootCcuUrl) {
			this.rootCcuUrl = rootCcuUrl
		} else {
			this.rootCcuUrl = DEFAULT_CCU_URL
		}
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
			this.defaultPurgeDomain = DEFAULT_PURGE_DOMAIN
		}
	}
}
