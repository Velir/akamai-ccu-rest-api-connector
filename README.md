# Velir Akamai CCU REST connector for AEM (CQ)

## Description

This bundle provides a "ready to use" OSGI connector for the Akamai Open CCU V3 REST API. This connector has been written in groovy using the http-builder framework.
It is designed to enable cache invalidation for AEM/CQ CMS when assets get invalidated. It can be easily configured with your own credentials
and settings. The connector provides all services that you can request via the REST API as well as convinence 

The bundle is made to be as light as possible and can be installed just by itself. You will need groovy-all version 2.4.7 to be installed along with some others bundles like
httpclient, commons-collections, commons-lang ... but they usually are already there.

As of version 2.5, this bundle requires AEM 6.3+

## Implementation

There are three major classes that manage the invalidation :

- CcuManager is the interface of the connector itself that manages a pool of connections to request the Akamai CCU REST services. It offers a simple method's signature with predefined default values
to invalidate cached objects. You can invalidate by CP code or ARL depending on your strategy. This class doesn't do any processing on the urls that you pass to it. It just makes
sure that the list contains only unique values and adds them to the invalidate caching request.

```groovy
def response = ccuManager.fastPurgeByUrls(["http://www.mysite.com/test", "http://www.mysite.com/test2"])
response = ccuManager.fastPurgeByCpCode("CPCODE1")
response = ccuManager.fastPurge(["http://www.mysite.com/test", "http://www.mysite.com/test2"], PurgeType.ARL, PurgeAction.REMOVE, PurgeDomain.PRODUCTION)
response = ccuManager.fastPurgeByTags(['tag1', 'tag2'])
response = ccuManager.fastPurge(["http://www.mysite.com/test", "http://www.mysite.com/test2"], FastPurgeType.URL, PurgeAction.INVALIDATE, PurgeDomain.STAGING)
response = ccuManager.fastPurge(["http://www.mysite.com/test", "http://www.mysite.com/test2"], FastPurgeType.URL)
response = ccuManager.fastPurge(["CPCODE1"], FastPurgeType.CPCODE)
response = ccuManager.fastPurge(["tag1"], FastPurgeType.TAG)
```

The minimum configuration needed for that service are your Akamai API tokens and secret.

- AkamaiEventHandler is an event handler that listens to com/day/cq/replication by default and just adds a job to a dedicated queue ("com/velir/aem/akamai/ccu/impl/FlushAkamaiItemsJob")
if the path to invalidate begins with one of the values specified in the list "pathsHandled" (By default it is /content/dam).

- FlushAkamaiItemsJob is the job that listens to the queue "com/velir/aem/akamai/ccu/impl/FlushAkamaiItemsJob" and calls the CCuManager.fastPurgeByUrls(...) with the list of paths to
invalidate. These paths are prepended by the rootUrl that represent the scheme + the domain without / at the end.
ex: rootSiteUrl = "http://www.velir.com" and url = "/test" => The path to invalidate will be "http://www.velir.com/test"

You don't have to use the whole thing -- you could easily just use the CCuManager to invalidate your cache without using the listener.

## Configuration

Each of these classes can be configured to fit you need and your Akamai credentials. In your JCR repository under /apps/your_app/config.author/ you can add the following configuration files:

- CcuManagerImpl: *com.velir.aem.akamai.ccu.impl.CcuManagerImpl.xml*

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.0"
    jcr:primaryType="sling:OsgiConfig"
    rootCcuUrl="https://api.ccu.akamai.com"
    clientToken="your_clientSecret"
    clientSecret="your_clientSecret"
    accessToken="your_accessToken"
    defaultPurgeAction="remove"
    defaultPurgeDomain="production"
    threadPoolSize="5"/>
```
For more info on credentials see https://developer.akamai.com/introduction/Prov_Creds.html

defaultPurgeAction : The default purge if not specified.
    - remove: (default) Remove the asset from the edge server and force the next request to the asset to reach the origin.
    - invalidate: Just invalidate the asset so the next query will still reach origin but can also still serve the stale value if origin is down.

defaultPurgeDomain : The default domain if not specified.
    - production: (default)
    - staging:
    
threadPoolSize : Number of threads to dedicate to the API HTTP Client. Defaults to 5.

- AkamaiEventHandler: *com.velir.aem.akamai.ccu.impl.AkamaiEventHandler.xml*

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.0"
    jcr:primaryType="sling:OsgiConfig"
    pathsHandled="[/content/dam]"/>
```

pathsHandled: Comma separated list of paths that can be invalidated.

- FlushAkamaiItemsJob: com.velir.aem.akamai.ccu.impl.FlushAkamaiItemsJob.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.0"
    jcr:primaryType="sling:OsgiConfig"
    rootSiteUrl="http://www.mysite.com"/>
```

rootSiteUrl: The root site url that is prepended to the path being invalidated.
## Administration Page

We've included a small administration page that can be found in the Velir section on AEM Tools. Example localhost direct url: [http://localhost:4502/etc/velir/akamaiflush.html](http://localhost:4502/etc/velir/akamaiflush.html)

This page is simple and allows administrators to manually flush urls without a replication event. Use cases for this would be after a deployment or in an emergency where needed production objects are stale in the CDN. 
The benefit of this page, is that AEM administrators can access  this functionality without having to be provided access the Luna control center.

The admin page can be filled in manually with URLS/ARLs, tags or CP codes and is currently limited to one object type per request.

Also, you can load the form via json file on the 'Choose json flush file' button. This takes in a json file in the following format, specifying objects and object type.
```json
{
	"objects":[
		"https://www.velir.com/object1",
		"https://www.velir.com/object2"
	],
	"type" : "URL"
}
```
Type is optional, but can be values URL, CPCODE or TAG. 

This option can be used if there is a repeatable series of objects that might need regular flushing, such as in after deployments.
#### Administration Security

It is recommended to limit the usage of the akamai flush administration page and endpoint to approved users. This is to limit usage to users who know what they are doing and are authorized to do so.  First, you can deny access to the admin page itself, /etc/velir/akamaiflush, which should be blocked for most user groups by default in newer versions of AEM.
Secondly, it is highly recommended to configure the API endpoint servlet to do the same.

- AkamaiFlushServlet: com.velir.aem.akamai.ccu.servlet.AkamaiFlushServlet.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.0"
    jcr:primaryType="sling:OsgiConfig"
    allowedGroups="[administrators,akamai-flush-admin]"/>
```

## Who are we

Velir is a Web Agency that provides a large scale of expertise in user experience design, content management, and marketing platform integrations. Our clients partner with us
to develop and implement websites and applications powered by Sitecore and AEM/Adobe CQ. More on www.velir.com

## License

This project is open source under MIT License.

## More information

If you want to learn more about the CCU REST API: https://developer.akamai.com/introduction/
