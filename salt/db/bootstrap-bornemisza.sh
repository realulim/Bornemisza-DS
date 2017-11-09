#!/bin/bash

cd /opt/scripts || exit -1
source ./config.sh db
sh bootstrap-common.sh db

# create db state tree
svn export --force $SvnTrunk/salt/db/srv/salt $SaltLocal
if [ ! -e $PillarLocal/top.sls ]; then
	svn export --force $SvnTrunk/salt/db/srv/pillar/top.sls $PillarLocal
	svn export --force $SvnTrunk/salt/db/srv/pillar/dbservers.sls $PillarLocal
	svn export --force $SvnTrunk/salt/db/srv/pillar/appserverips.sls $PillarLocal
fi

# dynamic pillar: haproxy
if [ ! -e $PillarLocal/haproxy.sls ]; then
        printf "stats-password: %s\n" "$(generatepw)" > "$PillarLocal"/haproxy.sls
fi

# dynamic pillar: couchdb
if [ ! -e $PillarLocal/couchdb.sls ]; then
	read -s -p -r 'CouchDB Admin Password [leave empty to generate random string]: ' COUCH_PW
	if [ -z "$COUCH_PW" ]; then
		COUCH_PW=$(generatepw)
	fi
	printf "couchdb-admin-password: %s\n" "$COUCH_PW" > $PillarLocal/couchdb.sls
	printf "\n"

	read -s -p -r 'Erlang Cookie [leave empty to generate random string]: ' COOKIE
	if [ -z "$COOKIE" ]; then
		COOKIE=$(generatepw)
	fi
	printf "cookie: %s\n" "$COOKIE" >> "$PillarLocal"/couchdb.sls
	printf "\n"

	read -p -r 'IP Address of Node already in Cluster [leave empty if this is the first node]: ' CLUSTERIP
	if [ -z "$CLUSTERIP" ]; then
		CLUSTERIP=$(getprivip db)
	fi
	printf "clusterip: %s\n" "$CLUSTERIP" >> "$PillarLocal"/couchdb.sls
	printf "\n"
fi

# letsencrypt needs to know the ssl endpoint for creating its certificate
if ! grep -q sslhost: $PillarLocal/basics.sls ; then
	SSLHOST=$(domainname -f)
	printf "sslhost: %s\n" "$SSLHOST" | tee -a "$PillarLocal"/basics.sls	
fi

# haproxy needs to know all external hostnames to select the correct load balancing strategy
sed -i '/  - .*/d' $PillarLocal/dbservers.sls
for HOSTNAME in $(host -t srv _db._tcp.$domain.|cut -d" " -f8|sort|rev|cut -c2-|rev|paste -s -d" ")
do
	if ! grep -q "$HOSTNAME" "$PillarLocal"/dbservers.sls ; then
		printf "  - %s\n" "$HOSTNAME" tee -a "$PillarLocal"/dbservers.sls
	fi
done

# haproxy needs to know the appserver source ips that are whitelisted for database access
sed -i '/  - .*/d' $PillarLocal/appserverips.sls
for HOSTNAME in $(host -t srv _app._tcp.$domain.|cut -d" " -f8|sort|rev|cut -c2-|rev|paste -s -d" ")
do
	IPADDRESS=$(getip "$HOSTNAME")
	if ! grep -q "$IPADDRESS" "$PillarLocal"/appserverips.sls ; then
		printf "  - %s\n" "$IPADDRESS" | tee -a "$PillarLocal"/appserverips.sls
	fi
done

chmod -R 400 $PillarLocal

# collect all pillar secrets in order to exclude them from Salt's output
SECRET_KEYS=(CFEMAIL CFKEY couchdb-admin-password cookie stats-password)
EXCL_STR=$(getsecrets "${SECRET_KEYS[@]}")

# create server
salt-call -l info state.highstate --force-color |& grep -E -v "$EXCL_STR"
