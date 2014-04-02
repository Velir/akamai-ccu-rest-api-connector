package com.velir.aem.akamai.ccu

/**
 * CcuManager - Define CCU methods
 *
 * @author Sebastien Bernard
 */
public interface CcuManager {
	/**
	 * Purge using url and default params. If url is null return PurgeReponse with httpStatus = -1
	 * @param url the url to purge
	 * @return a {@link PurgeResponse}
	 */
	PurgeResponse purgeByUrl(String url)

	/**
	 * Purge using urls collection and default params. If urls is null or empty return PurgeReponse with httpStatus = -1
	 * @param urls the collection of urls to purge
	 * @return a {@link PurgeResponse}
	 */
	PurgeResponse purgeByUrls(Collection<String> urls)

	/**
	 * Purge using cpCode and default params. If cpCode is null return PurgeReponse with httpStatus = -1
	 * @param cpCode the cpCode to purge
	 * @return a {@link PurgeResponse}
	 */
	PurgeResponse purgeByCpCode(String cpCode)

	/**
	 * Purge using CPCodes collection and default params. If cpCodes is null or empty return PurgeReponse with httpStatus = -1
	 * @param cpCodes the collection of urls to purge
	 * @return a {@link PurgeResponse}
	 */
	PurgeResponse purgeByCpCodes(Collection<String> cpCodes)

	/**
	 * Purge method where you can specify all params for your akamai request. If objets is null or empty return PurgeReponse with httpStatus = -1
	 * @param objets the collection of object to invalidate
	 * @param purgeType the purgeType to use
	 * @param purgeAction the purgeAction to use
	 * @param purgeDomain the purgeDomain to use
	 * @return a {@link PurgeResponse}
	 */
	PurgeResponse purge(Collection<String> objets, PurgeType purgeType, PurgeAction purgeAction, PurgeDomain purgeDomain)

	/**
	 * Return the status of a purge
	 * @param progressUri the purge uri
	 * @return a {@link PurgeStatus}
	 */
	PurgeStatus getPurgeStatus(String progressUri)

	/**
	 * Return the status of the queue with the number of objects waiting to be purge.
	 * @return a {@link QueueStatus}
	 */
	QueueStatus getQueueStatus()
}