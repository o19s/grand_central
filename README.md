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
1. Create Pod containing app version, database, and loader
1. Create Service for Pod (routes requests internally within the cluster)
1. Proxy the original request

*Note* that all requests will have some metrics stored to determine activity for a give pod. This is useful when reaping old pods.

**BUT WAIT!** *What happens when two requests come in for the same version?*

Easy, state is maintained within an Atomically accessed Map. As soon as a version is detected to not be running we instantiate it before releasing the lock. If a version exists, but isn't running we pause the request until the creation process is complete on another thread.

## Pod Creation
Should a container not be running in the Kubernetes cluster when the request is performed the following process starts.

1. Create a Pod with the following components
   * Rails Application
   * MySQL Instance
   * Data dump loader (golden review image stored block store)
1. Create a service referencing that pod

## Pod Clean-up
Every hour a janitorial task runs which cleans up pods / services that have not received a request in the past *x* seconds. This keeps cluster resources available. Since pods are trivial to create they can be re-instantiated easily.

## Resource Constraints
There is a hard limit to the number of simultaneous pods running on the cluster. To prevent Denial of Service by our own team the maximum number of review environments is capped at *y*. Should a new pod be requested when the currently running count is already maxed the pod with the oldest most recent request will be removed.

## Future Features
* Persistent hashes - the ability to mark a version as persistent. This prevents the janitor from reaping the pod.
* Admin interface - allow a RESTful interface to manage pods. List all pods, create a new one, delete an old one out of the janitorial process etc.
* Security?!

## Implementation

All logic for checking / creation of pods may be performed in a [`javax.servlet.Filter`](http://docs.oracle.com/javaee/7/api/javax/servlet/Filter.html?is-external=true). Requests may then be passed along to a [`org.eclipse.jetty.proxy.ProxyServlet`](http://download.eclipse.org/jetty/stable-9/apidocs/org/eclipse/jetty/proxy/ProxyServlet.html) after pod management is complete.

## Local K8S Route
Adds a route into the OS X routing table to forward all requests to the internal K8S IP space through the K8S virtual machine's IP.

```
sudo route add -net 10.2.47 172.17.4.99
```

## Local K8S Certificate
The K8S cluster has a self-signed SSL certificate. It must be added to a keystore as a trusted certificate before requests are permitted.

**OS X**

```
brew install openssl
echo -n | /usr/local/Cellar/openssl/1.0.2e/bin/openssl s_client -connect 172.17.4.99:443 | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > k8s/local.pem
echo -n | /usr/local/Cellar/openssl/1.0.2e/bin/openssl s_client -connect gcr.io:443 | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > k8s/gcr.io.pem
keytool -import -v -trustcacerts -alias local_k8s -file k8s/local.pem -keystore grandcentral.jks -keypass changeit -storepass changeit
keytool -import -v -trustcacerts -alias gcr_io -file k8s/gcr.io.pem -keystore grandcentral.jks -keypass changeit -storepass changeit
```

**Linux**

```
echo -n | openssl s_client -connect 172.17.4.99:443 | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > config/local.pem
echo -n | openssl s_client -connect gcr.io:443 | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > config/gcr.io.pem
keytool -import -v -trustcacerts -alias local_k8s -file k8s/local.pem -keystore config/grandcentral.jks -keypass changeit -storepass changeit
keytool -import -v -trustcacerts -alias gcr_io -file k8s/gcr.io.pem -keystore config/grandcentral.jks -keypass changeit -storepass changeit
```

### Logging in to GCR.io
Setup a GCP service account and download it's JSON key file

```
docker login -e 1234@5678.com -u _json_key -p "$(cat keyfile.json)" https://gcr.io
```
