# Bornemisza
This is a cloud-based distributed system that self-installs onto standard CentOS VMs of the $5 variety.
It provides a generic template for starting a web-based business on the cheap and seamlessly progressing to web scale later on. Theoretically it should run on any IAAS offering CentOS images, but in practice there are limitations due to the way the cloud providers set up their data centers (see below for detailed requirements).

The main difference to most other distributed system architectures is that infrastructure is not a first-class citizen. It gets deployed as part of a larger entity and has no life of its own. Currently a system consists of two types of entities: application nodes and database nodes. Those are the smallest deployment units and contain all the infrastructure necessary to discover and connect to each other.

## Architectural Overview
The backend is comprised of two clusters: an app cluster that bundles the available application nodes and a db cluster consisting of all the database nodes. The frontend (currently included in the app node) is a statically served HTML5 single page application, so that the UI runs entirely on the client. For any backend functionality, such as requests for data or business logic processing, the client uses a static interface name like `www.myservice.de` to connect to a REST API running on the app cluster. The interface name is made highly available by a routing daemon that talks to upstream via BGP, so that packets are always routed to a working app node. Whenever an app node wants to access persistent data, it uses a client-side load balancing scheme to connect to one of the db nodes. The database is per-user and schema-free in order to mitigate the otherwise common requirement of one database per microservice.

## Design Goals

#### 1. Horizontal scalability
- new nodes join the (app or db) cluster automatically
- load distribution is adjusted when cluster membership changes
- no single point of failure
- all nodes are self-sufficient, no central service needed at runtime
- management decentralised as well

#### 2. Composability
- create the system from small and well-understood components
- avoid being owned by a God Platform
- use standards where available

#### 3. Affordability
- a small production system could have three app nodes and three db nodes, thus keeping the monthly cost well below $50
- for learning or development purposes it is, of course, also possible to work with one node each
- let's not be afraid to skimp here

#### 4. Security
- let's be **very** afraid to skimp here

## Technology Stack

#### Business Logic: Payara
- runs Java-based microservices
- is clustered via Hazelcast over a private network
- does client-side load balancing in front of db cluster

#### Persistence: CouchDB
- provides one database per user
- knows all app nodes and talks to no one else
- is clustered via Erlang over a private network

#### Frontend: single page application
- lean approach for pin-pointed functionality: Riot.js framework, Grapnel router and qwest XHR2 engine (38 KB total)
- is fully responsible for the UI, only talks to the backend for data and business logic
- self-service user registration with email confirmation

#### Edge Server: HAProxy
- terminates SSL in front of the private networks
- load balancing of frontend and Payara cluster

#### High Availability: Bird, Monit
- nodes are monitored and taken in and out of service on the routing or application layer

#### Security: acme.sh, ufw
- nodes automatically create and manage SSL certificates from Letsencrypt
- Firewall blocks everything except white-listed traffic

## Provisioning
- infrastructure setup via masterless Salt
- download bootstrap script from Github
- run bootstrap script locally, manually type in secrets during installation
- SaltStack does the rest
- self-healing and self-updating system

## Requirements
- Cloud Provider must support CentOS (most do)
- public IP and private IP on seperate network devices (e. g. Vultr, UpCloud, DigitalOcean, but not Linode, Scaleway, VirMach)
- one floating IP (e. g. Vultr, DigitalOcean) as a fixed entrypoint
- a Cloudflare account (free plan works fine)
- a domain as an anchor for the SSL certificates
- nameservers for domain must delegate to the Cloudflare nameservers associated with your Cloudflare account

## Installation
- clone repository and edit the user-definable settings in salt/common/config.sh
- create cloud server according to the above requirements
- log in to cloud server and `wget https://raw.githubusercontent.com/yourgithubname/Bornemisza/master/salt/bootstrap.sh`
- `./bootstrap.sh app` for installing an app node and `./bootstrap.sh db` for installing a db node
- follow on-screen instructions
- once you installed at least one app node and one db node: `https://www.yourdomain.com`

## Troubleshooting
##### This is a self-healing system, so many problems will disappear after 15 minutes, when Salt runs again (it is started every 15 minutes via crontab) or whenever Salt is run manually via `salt-call -l info state.highstate`. This applies especially to timing issues, which are unavoidable in any distributed system, but can often be solved with a retry strategy:
- SSL certificate isn't acquired from Letsencrypt due to a timeout: self-healing
- The very first installation of the very first app or db node fails, because there are no SRV records in the DNS yet and thus an invalid haproxy.cfg file is generated: self-healing
