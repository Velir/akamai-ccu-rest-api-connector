package com.velir.aem.akamai.ccu

/**
 * CcuManager - Define CCU methods
 *
 * @author Sebastien Bernard
 */
interface CcuManager {

	/**
	 * Purge using url and default params. If url is null return FastPurgeResponse with httpStatus = -1
	 * @param url the url to purge
	 * @return a {@link FastPurgeResponse}
	 */
	FastPurgeResponse fastPurgeByUrl(String url)

	/**
	 * Purge using urls collection and default params. If urls is null or empty return FastPurgeResponse with httpStatus = -1
	 * @param urls the collection of urls to purge
	 * @return a {@link FastPurgeResponse}
	 */
	FastPurgeResponse fastPurgeByUrls(Collection<String> urls)

	/**
	 * Purge using cpCode and default params. If cpCode is null return FastPurgeResponse with httpStatus = -1
	 * @param cpCode the cpCode to purge
	 * @return a {@link FastPurgeResponse}
	 */
	FastPurgeResponse fastPurgeByCpCode(String cpCode)

	/**
	 * Purge using CPCodes collection and default params. If cpCodes is null or empty return FastPurgeResponse with httpStatus = -1
	 * @param cpCodes the collection of urls to purge
	 * @return a {@link FastPurgeResponse}
	 */
	FastPurgeResponse fastPurgeByCpCodes(Collection<String> cpCodes)

	/**
	 * Purge using tag and default params. If url is null return FastPurgeResponse with httpStatus = -1
	 * @param tag the tag to purge
	 * @return a {@link FastPurgeResponse}
	 */
	FastPurgeResponse fastPurgeByTag(String tag)

	/**
	 * Purge using tag collection and default params. If urls is null or empty return FastPurgeResponse with httpStatus = -1
	 * @param tags the collection of tags to purge
	 * @return a {@link FastPurgeResponse}
	 */
	FastPurgeResponse fastPurgeByTags(Collection<String> tags)

	/**
	 * Performs a fast purge for multiple object types on default params
	 * @param objects list of objects to purge
	 * @param type type of objects, urls, cp codes or tags
	 * @return a PurgeResponse
	 */
	FastPurgeResponse fastPurge(Collection<String> objects, FastPurgeType type)

	/**
	 * Performs a fast purge for multiple object types
	 * @param objects objects list of objects to purge
	 * @param type type type of objects, urls, cp codes or tags
	 * @param purgeAction invalidate or remove
	 * @param purgeDomain staging or production
	 * @return
	 */
	FastPurgeResponse fastPurge(Collection<String> objects, FastPurgeType type, PurgeAction purgeAction, PurgeDomain purgeDomain)
}