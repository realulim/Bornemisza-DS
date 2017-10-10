# Bornemisza
This is a cloud-based distributed system that self-installs onto standard CentOS VMs of the $5-$10 variety.
It provides a generic template for starting a web-based business on the cheap and seamlessly progressing to web scale later on.

The main difference to most other distributed system architectures is that infrastructure is not a first-class citizen. Deployable units are simply application servers and database servers (shorthand notation: app nodes and db nodes).

## Architectural Overview
The backend is comprised of the two clusters (app and db), whereas the frontend is a statically served HTML5 single page application, so that the UI runs entirely on the client. For any backend functionality, such as requests for data or business logic processing, the client uses a static interface name (e. g. www&#8203;.myservice.de) to connect to a REST API running on the app cluster. The interface name is highly available through a mechanism on the network layer that makes sure requests are routed to one of the app nodes. Whenever an app node wants to access persistent data, it uses a client-side load balancing scheme to connect to one of the db nodes.

## Design Goals

#### 1. True horizontal scalability
- new nodes join the (app or db) cluster automatically
- load distribution is adjusted when cluster membership changes
- no single point of failure
- all nodes are self-sufficient, no central service needed at runtime
- the management layer must scale horizontally as well

#### 2. Composability
- create the system from small and well-understood tools
- avoid being owned by a God Platform
- use standards where available

#### 3. Affordability
- the minimal production system consists of three app nodes and three db nodes, thus keeping the monthly cost well below $50
- for learning or development purposes it is, of course, also possible to work with one node each
- don't be afraid to skimp here

#### 4. Security
- be **very** afraid to skimp here

## Technology Stack

#### Application Server: Payara
- runs Java-based microservices
- is clustered via Hazelcast over a private network
- client-side load balancing of db cluster

#### Database: CouchDB
- runs one database per user
- only talks to Payara
- is clustered via Erlang over a private network

#### Frontend: single page application
- lean approach with Riot.js framework and micro libraries for pin-pointed functionality
- is fully responsible for the UI, only talks to the backend for data and business logic
- self-service user registration with email confirmation
    
#### Edge Server: HAProxy
- terminates SSL in front of the private networks
- load balancing of frontend and Payara cluster

#### High Availability: Bird, Monit
- nodes are monitored and taken in and out of service on the routing layer

## Provisioning
- Infrastructure setup via masterless Salt
- download bootstrap script from Github
- run bootstrap script locally, manually type in secrets during installation
- SaltStack does the rest
- self-healing and self-updating system by running Salt minion periodically

## Requirements
- Cloud Provider must support CentOS and private networks (e. g. Vultr, UpCloud)
- DNS Provider (e. g. free Cloudflare account)
- a domain as an anchor for the SSL certificates

```
more detailed documentation to follow
```
