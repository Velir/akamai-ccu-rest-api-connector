package com.velir.aem.akamai.ccu

/**
 * PurgeAction -
 *
 * @author Sebastien Bernard
 */
enum PurgeAction {
	REMOVE('delete'),
	INVALIDATE('invalidate')

	private String apiVal

	PurgeAction(String apiVal) {
		this.apiVal = apiVal
	}

	String getApiVal() {
		return apiVal
	}
}