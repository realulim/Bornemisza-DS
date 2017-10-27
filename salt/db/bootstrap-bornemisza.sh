#!/bin/bash

cd /opt/scripts
source ./config.sh db
sh bootstrap-common.sh db

# create db state tree
svn export --force $SvnTrunk/salt/db/srv/salt /srv/salt
if [ ! -e $PillarLocal/top.sls ]; then
	svn export --force $SvnTrunk/salt/db/srv/pillar/top.sls /srv/pillar
fi

# dynamic pillar: haproxy
if [ ! -e $PillarLocal/haproxy.sls ]; then
        printf "stats-password: `generatepw`\n" > $PillarLocal/haproxy.sls
fi

# dynamic pillar: couchdb
if [ ! -e $PillarLocal/couchdb.sls ]; then
	read -s -p 'CouchDB Admin Password [leave empty to generate random string]: ' COUCH_PW
	if [ -z $COUCH_PW ]; then
		COUCH_PW=`generatepw`
	fi
	printf "couchdb-admin-password: $COUCH_PW\n" > $PillarLocal/couchdb.sls
	printf "\n"

	read -s -p 'Erlang Cookie [leave empty to generate random string]: ' COOKIE
	if [ -z $COOKIE ]; then
		COOKIE=`generatepw`
	fi
	printf "cookie: $COOKIE\n" >> $PillarLocal/couchdb.sls
	printf "\n"

	read -p 'IP Address of Node already in Cluster [leave empty if this is the first node]: ' CLUSTERIP
	if [ -z $CLUSTERIP ]; then
		CLUSTERIP=`getprivip db`
	fi
	printf "clusterip: $CLUSTERIP\n" >> $PillarLocal/couchdb.sls
	printf "\n"

	CLUSTERSIZE=`host -t srv _db._tcp.$domain.|wc -l`
	printf "clustersize: $CLUSTERSIZE\n" >> $PillarLocal/couchdb.sls
fi

# letsencrypt needs to know the ssl endpoint for creating its certificate
if ! grep -q sslhost: /srv/pillar/basics.sls ; then
	SSLHOST=`domainname -f`
	printf "sslhost: $SSLHOST\n" | tee -a $PillarLocal/basics.sls	
fi

# haproxy needs to know all external hostnames to select the correct load balancing strategy
COUNTER=1
for HOSTNAME in `host -t srv _db._tcp.$domain.|cut -d" " -f8|sort|rev|cut -c2-|rev|paste -s -d" "`
do
	if ! grep -q hostname$COUNTER /srv/pillar/haproxy.sls ; then
		printf "hostname$COUNTER: $HOSTNAME\n" | tee -a $PillarLocal/haproxy.sls
	fi
	let "COUNTER++"
done

# haproxy needs to know the appserver source ips that are whitelisted for database access
COUNTER=1
for HOSTNAME in `host -t srv _app._tcp.$domain.|cut -d" " -f8|sort|rev|cut -c2-|rev|paste -s -d" "`
do
	sed -i /ipapp$COUNTER/d /srv/pillar/haproxy.sls
	IPADDRESS=`getip $HOSTNAME`
	if [ ! -z $IPADDRESS ]; then
		printf "ipapp$COUNTER: $IPADDRESS\n" | tee -a $PillarLocal/haproxy.sls
		let "COUNTER++"
	fi
done

chmod -R 400 $PillarLocal

# collect all pillar secrets in order to exclude them from Salt's output
SECRET_KEYS=(CFEMAIL CFKEY couchdb-admin-password cookie stats-password)
EXCL_STR=`getsecrets ${SECRET_KEYS[@]}`

# create server
salt-call -l info state.highstate --force-color |& egrep -v $EXCL_STR
