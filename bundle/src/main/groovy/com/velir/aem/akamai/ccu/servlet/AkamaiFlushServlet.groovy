package com.velir.aem.akamai.ccu.servlet

import com.velir.aem.akamai.ccu.*
import groovyx.net.http.ContentType
import org.apache.felix.scr.annotations.Reference
import org.apache.felix.scr.annotations.sling.SlingServlet
import org.apache.http.HttpStatus
import org.apache.sling.api.SlingHttpServletRequest
import org.apache.sling.api.SlingHttpServletResponse
import org.apache.sling.api.servlets.SlingAllMethodsServlet
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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
@SlingServlet(paths ="/bin/velir/flushakamai", methods = "POST")
class AkamaiFlushServlet extends SlingAllMethodsServlet {

	private static final Logger LOG = LoggerFactory.getLogger(AkamaiFlushServlet)
	private static final String URL = "URL"
	private static final String OBJ = "obj"
	private static final String OBJ_TYPE = "objType"
	private static final String JOIN = ", "

	@Reference
	private CcuManager ccuManager

	@Override
	protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
		response.status = SC_OK
		response.contentType = JSON
		List<String> objects = request.getParameterValues(OBJ)?:[] as List<String>
		String objType = request.getParameter(OBJ_TYPE) ?: URL
		CcuResponse ccuResponse = getApiResponse(objects, objType)
		response.writer.write(toJson(ccuResponse))
	}

	private CcuResponse getApiResponse(List<String> objects, String objType) {
		CcuResponse ccuResponse = new PurgeResponse()
		if (objects && objType) {
			LOG.debug("Flushing objects {} type {}", objects.join(JOIN), objType)
			try {
				ccuResponse = fastPurge(objects, objType)
			} catch (e) {
				LOG.error("Error flushing Akamai", e)
				ccuResponse = new PurgeResponse(httpStatus: SC_INTERNAL_SERVER_ERROR, detail: e.message)
			}
		}
		ccuResponse
	}

	private FastPurgeResponse fastPurge(List<String> objects, String objType) {
		FastPurgeType type = objType as FastPurgeType
		ccuManager.fastPurge(objects, type)
	}
}
