package com.velir.aem.akamai.ccu.servlet

import com.velir.aem.akamai.ccu.*
import groovyx.net.http.ContentType
import org.apache.felix.scr.annotations.Component
import org.apache.felix.scr.annotations.Property
import org.apache.felix.scr.annotations.Properties
import org.apache.felix.scr.annotations.Reference
import org.apache.felix.scr.annotations.Service
import org.apache.felix.scr.annotations.sling.SlingServlet
import org.apache.http.HttpStatus
import org.apache.jackrabbit.api.security.user.User
import org.apache.jackrabbit.api.security.user.UserManager
import org.apache.sling.api.SlingHttpServletRequest
import org.apache.sling.api.SlingHttpServletResponse
import org.apache.sling.api.servlets.SlingAllMethodsServlet
import org.apache.sling.commons.osgi.PropertiesUtil
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.Servlet
import javax.servlet.ServletException

import static groovy.json.JsonOutput.toJson
import static groovyx.net.http.ContentType.JSON
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR
import static org.apache.http.HttpStatus.SC_OK

/**
 * AkamaiFlushServlet -
 *
 * @author Kai Rasmussen
 */
@Component(label = "Akamai Admin Flush Servlet", metatype = true, immediate = true)
@Service(value = Servlet)
@Properties(value = [
    @Property(name = "sling.servlet.paths", value = "/bin/velir/flushakamai", propertyPrivate = true),
	@Property(name = "sling.servlet.methods ", value = "POST", propertyPrivate = true),
	@Property(name = "allowedGroups", value = [], cardinality = Integer.MAX_VALUE)
])
class AkamaiFlushServlet extends SlingAllMethodsServlet {

	private static final Logger LOG = LoggerFactory.getLogger(AkamaiFlushServlet)
	private static final FastPurgeResponse NOT_ALLOWED = new FastPurgeResponse(httpStatus: HttpStatus.SC_FORBIDDEN, detail: "Forbidden")
	private static final String URL = "URL"
	private static final String OBJ = "obj"
	private static final String OBJ_TYPE = "objType"
	private static final String JOIN = ", "

	private Set<String> allowedGroups

	@Reference
	private CcuManager ccuManager

	@Override
	protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
		response.status = SC_OK
		response.contentType = JSON
		if(!allowed(request)){
			response.writer.write(toJson(NOT_ALLOWED))
			return
		}
		List<String> objects = request.getParameterValues(OBJ)?:[] as List<String>
		String objType = request.getParameter(OBJ_TYPE) ?: URL
		FastPurgeResponse ccuResponse = getApiResponse(objects, objType)
		response.writer.write(toJson(ccuResponse))
	}

	private FastPurgeResponse getApiResponse(List<String> objects, String objType) {
		FastPurgeResponse ccuResponse = new FastPurgeResponse()
		if (objects && objType) {
			LOG.debug("Flushing objects {} type {}", objects.join(JOIN), objType)
			try {
				ccuResponse = fastPurge(objects, objType)
			} catch (e) {
				LOG.error("Error flushing Akamai", e)
				ccuResponse = new FastPurgeResponse(httpStatus: SC_INTERNAL_SERVER_ERROR, detail: e.message)
			}
		}
		ccuResponse
	}

	private FastPurgeResponse fastPurge(List<String> objects, String objType) {
		FastPurgeType type = objType as FastPurgeType
		ccuManager.fastPurge(objects, type)
	}

	void activate(ComponentContext context) {
		Dictionary props = context.properties
		allowedGroups = PropertiesUtil.toStringArray(props.get("allowedGroups"), [] as String[]) as Set
	}

	boolean allowed(SlingHttpServletRequest request) {
		boolean allowed = false
		try{
			User user = request.resourceResolver.adaptTo(UserManager).getAuthorizable(request.userPrincipal) as User
			def groups = user.memberOf()*.ID
			allowed = !allowedGroups || groups.intersect(allowedGroups)
		} catch(e){
			LOG.error("Error determining allowed", e)
		}
		allowed
	}
}
