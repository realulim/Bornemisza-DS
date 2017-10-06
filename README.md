# Bornemisza
This is a cloud-based distributed system that self-installs onto standard CentOS VMs of the $5-$10 variety.
It provides a template for starting a web-based business on the cheap and seamlessly progressing to web scale later on.

## Architectural Goals

#### 1. True horizontal scalability
- new nodes join the (application server or database) cluster automatically
- load distribution is adjusted when cluster membership changes
- no single point of failure
- all nodes are self-sufficient, no central service needed at runtime
- the management layer must scale horizontally as well

#### 2. Composability
- create the system from small and well-understood tools
- avoid being owned by a God Platform
- use standards where available

#### 3. Affordability
- the minimal production system consists of three application servers and three databases, thus keeping the monthly cost well below $50
- for learning or development purposes it is, of course, also possible to work with one node each
- don't be afraid to skimp here

#### 4. Security
- be **very** afraid to skimp here

## Technology Stack

#### Application Server: Payara
- runs Java-based microservices
- is clustered via Hazelcast over a private network
- client-side load balancing of Database cluster

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
- load balancing of Frontend and Payara cluster

#### High Availability: Bird, Monit
- nodes are monitored and taken in and out of service on the routing layer

## Provisioning
- Infrastructure Setup via masterless Salt
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
