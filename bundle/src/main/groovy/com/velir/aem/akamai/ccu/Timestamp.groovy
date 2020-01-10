package com.velir.aem.akamai.ccu

import groovy.transform.CompileStatic

/**
 * Timestamp - Date Formatter
 *
 * @author Kai Rasmussen
 */
@CompileStatic
class Timestamp {
	private static final String CCU_FORMAT = "yyyyMMdd'T'HH:mm:ssZ"
	public static final String UTC = "UTC"
	public static final TimeZone UTCTZ = TimeZone.getTimeZone(UTC)

	static String getTimestamp(Date date){
		date.format(CCU_FORMAT, UTCTZ)
	}
}