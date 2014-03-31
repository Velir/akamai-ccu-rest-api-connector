package com.velir.aem.akamai.ccu

/**
 * CcuManager - Define CCU methods
 *
 * @author Sebastien Bernard
 */
public interface CcuManager {
	PurgeResponse purgeByUrl(String url)

	PurgeResponse purgeByUrls(Collection<String> urls)

	PurgeResponse purgeByCpCode(String cpCode)

	PurgeResponse purgeByCpCodes(Collection<String> cpCode)

	PurgeResponse purge(Collection<String> objets, PurgeType purgeType, PurgeAction purgeAction, PurgeDomain purgeDomain)

	PurgeStatus getPurgeStatus(String progressUri)

	QueueStatus getQueueStatus()
}