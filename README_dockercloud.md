# Grand Central
*Quepid's automated review deployment tool. Gut-check in the cloud*

Grand Central is a tool for automated deployment and cleanup of developer review environments / containers. Requests are parsed and routed based on their URL structure. If the target container exists the request is proxied along. If not Grand Central will spin up a container and forward the request once it comes online.

## URL Structure
The appropriate container is determined by parsing the first part of the domain name. `*.review.quepid.com` in DNS is directed at the Grand Central service. The application parses the domain name to retrieve the appropriate Git version to deploy. `http://db139cf.review.quepid.com/secure` would route to a container running version `db139cf` if it exists.

## Request Flow
When a request is received by the system the following processing takes place.

1. Validate the git version supplied in the `Host` header. *Does it match a valid short hash signature?*
1. Verify if the version is currently running
   * If so, proxy the request
   * If not, continue
1. Verify version exists in Container Registry. *We can't deploy a version which doesn't exist.
   * If so, continue
   * If not, 404
1. Create DockerCloud Stack (?) containing app version, database, and loader
1. Create Service for Stack (routes requests internally within the cluster)
1. Proxy the original request

*Note* that all requests will have some metrics stored to determine activity for a give stack. This is useful when reaping old stacks.

**BUT WAIT!** *What happens when two requests come in for the same version?*

Easy, state is maintained within an Atomically accessed Map. As soon as a version is detected to not be running we instantiate it before releasing the lock. If a version exists, but isn't running we pause the request until the creation process is complete on another thread.

## Stack Creation
Should a container not be running in the DockerCloud cluster when the request is performed the following process starts.

1. Create a Stack with the following components (ERIC: IS THIS AT ALL RIGHT?)
   * Rails Application
   * MySQL Instance
   * Data dump loader (golden review image stored block store)
1. Create a service referencing that stack

## Stack Clean-up
Every hour a janitorial task runs which cleans up stacks / services that have not received a request in the past *x* seconds. This keeps cluster resources available. Since stacks are trivial to create they can be re-instantiated easily.

## Resource Constraints
There is a hard limit to the number of simultaneous stacks running on the cluster. To prevent Denial of Service by our own team the maximum number of review environments is capped at *y*. Should a new stack be requested when the currently running count is already maxed the stack with the oldest most recent request will be removed.

## Future Features
* Persistent hashes - the ability to mark a version as persistent. This prevents the janitor from reaping the pod.
* Admin interface - allow a RESTful interface to manage stacks. List all stacks, create a new one, delete an old one out of the janitorial process etc.
* Security?!


## curling

curl --user username:apikey "https://cloud.docker.com/api/app/v1/stack/"

## /etc/hosts

Add to make testing your local set up easier this to your `/etc/hosts` file:

```
127.0.0.1 v1.datastart.grandcentral.com
127.0.0.1 v2.datastart.grandcentral.com
```




## Implementation

All logic for checking / creation of pods may be performed in a [`javax.servlet.Filter`](http://docs.oracle.com/javaee/7/api/javax/servlet/Filter.html?is-external=true). Requests may then be passed along to a [`org.eclipse.jetty.proxy.ProxyServlet`](http://download.eclipse.org/jetty/stable-9/apidocs/org/eclipse/jetty/proxy/ProxyServlet.html) after stack management is complete.
