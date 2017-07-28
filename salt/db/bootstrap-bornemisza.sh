#!/bin/bash

cd /opt/scripts
source ./config.sh db
sh bootstrap-common.sh db

# create db state tree
svn export --force $SaltTrunk/db/srv/salt /srv/salt

# static pillars
for FILE in top.sls
do
	curl -o $PillarLocal/$FILE -L $PillarRemote/$FILE
done

# dynamic pillar: haproxy
if [ ! -e $PillarLocal/haproxy.sls ]; then
	curl -o $PillarLocal/haproxy.sls -L $PillarRemote/haproxy.sls
	sed -ie s/stats-password:/"stats-password: `generatepw`"/ $PillarLocal/haproxy.sls
fi

# dynamic pillar: couchdb
if [ ! -e $PillarLocal/couchdb.sls ]; then
	curl -o $PillarLocal/couchdb.sls -L $PillarRemote/couchdb.sls

	read -p 'CouchDB Admin Password [leave empty to generate random string]: ' COUCH_PW
	if [ -z $COUCH_PW ]; then
		COUCH_PW=`generatepw`
	fi
	sed -ie s/couchdb-admin-password:/"couchdb-admin-password: $COUCH_PW"/ $PillarLocal/couchdb.sls

	read -p 'Erlang Cookie [leave empty to generate random string]: ' COOKIE
	if [ -z $COOKIE ]; then
		COOKIE=`generatepw`
	fi
	sed -ie s/cookie:/"cookie: $COOKIE"/ $PillarLocal/couchdb.sls

	read -p 'IP Address of Node already in Cluster [leave empty if this is the first node]: ' CLUSTERIP
	if [ -z $CLUSTERIP ]; then
		CLUSTERIP=`getprivip db`
	fi
	sed -ie s/clusterip:/"clusterip: $CLUSTERIP"/ $PillarLocal/couchdb.sls

	CLUSTERSIZE=${#db_HostLocation[@]}
	sed -ie s/clustersize:/"clustersize: $CLUSTERSIZE"/ $PillarLocal/couchdb.sls
fi

# letsencrypt needs to know the ssl endpoint for creating its certificate
if ! grep -q sslhost: /srv/pillar/basics.sls ; then
	SSLHOST=`domainname -f`
	printf "sslhost: $SSLHOST\n" | tee -a $PillarLocal/basics.sls	
fi

# haproxy needs to know all external hostnames to select the correct load balancing strategy
COUNTER=1
for LOCATION in ${db_HostLocation[@]}
do
	if ! grep -q hostname$COUNTER /srv/pillar/haproxy.sls ; then
		HOSTNAME=$db_HostPrefix.$LOCATION.$db_Domain
		printf "hostname$COUNTER: $HOSTNAME\n" | tee -a $PillarLocal/haproxy.sls
	fi
	let "COUNTER++"
done

# haproxy needs to know the appserver source ips that are whitelisted for database access
for COUNTER in `seq -s' ' 1 $app_HostCount`
do
	if ! grep -q ipapp$COUNTER /srv/pillar/haproxy.sls ; then
		HOSTNAME=$app_HostPrefix$COUNTER.$app_Domain
		printf "ipapp$COUNTER: `getip $HOSTNAME`\n" | tee -a $PillarLocal/haproxy.sls
	fi
done

chmod -R 400 $PillarLocal

# create server
salt-call -l info state.highstate
