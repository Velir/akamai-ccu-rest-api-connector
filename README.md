# Velir Akamai CCU REST connector for AEM (CQ)

## Description

This bundle provide a "ready to use" OSGI connector for the new Akamai CCU REST API. This connector has been written in groovy using the http-builder.
It is design to enable cache invalidation for AEM/CQ CMS when assets get invalidated. It can be easily configured with your own credentials
and settings. The connector provide all services that you can request via the REST API like getPurgeStatus(), getQueueStatus() and the most important purge().

The bundle is made to be as light as possible and can be installed just by itself. You will need groovy-all to be installed along with some others bundle like
httpclient, commons-collections, commons-lang ... but they usually are already there.

## Implementation

They are three major classes that manage the invalidation :

- CcuManager is the interface of the connector itself that manage a pool of connections to request the Akamai CCU REST services. It offers simple method's signature with predefined default values
to invalidate cached objects. You can invalidate by CP code or ARL depending of your strategy. This class doesn't do any processing on the urls that you pass to it. It just make
sure that the list contains only unique values and add them to the invalidate caching request.
ex:
`
def response = ccuManager.purgeByUrls(["http://www.mysite.com/test", "http://www.mysite.com/test2"])
def response = ccuManager.purgeByCpCode("CPCODE1")
def response = ccuManager.purge(["http://www.mysite.com/test", "http://www.mysite.com/test2"], PurgeType.ARL, PurgeAction.REMOVE, PurgeDomain.PRODUCTION)
`

The minimum configuration needed for that service are your Akamai credentials : "userName" and "password".

- AkamaiEventHandler is an event handler that listen to com/day/cq/replication by default and just add a job to a dedicated queue ("com/velir/aem/akamai/ccu/impl/FlushAkamaiItemsJob")
if the path to invalidate start by one of the values specified in the list "pathsHandled" (By default it is /content/dam).

- FlushAkamaiItemsJob is the job that listen to the queue "com/velir/aem/akamai/ccu/impl/FlushAkamaiItemsJob" and call the CCuManager.purgeByUrls(...) with the list of path to
invalidate. These paths are prepended by the rootUrl that represent the scheme + the domaine without / at the end.
ex: rootSiteUrl = http://www.velir.com and url = /test => The path to invalidate will be http://www.velir.com/test

You don't have to use the all thing you could easily just use the CCuManager to invalidate your cache without using the listener.

## Configuration

Each of these classes can be configured to fit you need and your akamai credential. In your JCR repository under /apps/your_app/config.author/ you can add the following configuration files:

- CcuManagerImpl: *com.velir.aem.akamai.ccu.impl.CcuManagerImpl.xml*
`<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.0"
		  jcr:primaryType="sling:OsgiConfig"
		  rootCcuUrl="https://api.ccu.akamai.com"
	      userName="your_username"
		  password="your_password"
		  defaultPurgeAction="remove"
		  defaultPurgeDomain="prod"/>
</code>`

defaultPurgeAction : The default purge if not specified.
    - remove: (default) Remove the asset from the edge server and force the next request to the asset to reach the origin.
    - invalidate: Just invalidate the asset so the next query will still reach origin but can also still serve the stale value if origin is down.

defaultPurgeDomain : The default domain if not specified.
    - production: (default)
    - staging:

- AkamaiEventHandler: *com.velir.aem.akamai.ccu.impl.AkamaiEventHandler.xml*
`
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.0"
		  jcr:primaryType="sling:OsgiConfig"
		  pathsHandled="[/content/dam]"/>
</code>
`

pathsHandled: Comma separated list of paths that can be invalidate.

- FlushAkamaiItemsJob: com.velir.aem.akamai.ccu.impl.FlushAkamaiItemsJob.xml
`
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.0"
		  jcr:primaryType="sling:OsgiConfig"
		  rootSiteUrl="http://www.mysite.com"/>
`

rootSiteUrl: The root site url that is prepended to the path being invalidated.

## Who are we

Velir is Web Agency that provide a large scale of expertise in user experience design, content management, and marketing platform integrations. Our clients partner with us
to develop and implement websites and applications powered by Sitecore and AEM/Adobe CQ. More on www.velir.com

## License

This project is open source under MIT License.

## More information

If you want to learn more about the CCU REST API: https://api.ccu.akamai.com/ccu/v2/docs/index.html
