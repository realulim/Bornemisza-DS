#!/bin/bash

cd /opt/scripts
source ./config.sh db
sh bootstrap-common.sh db

# create state tree
mkdir -p $SaltLocal/files/couchdb
mkdir -p $SaltLocal/files/haproxy
for FILE in top.sls haproxy.sls files/haproxy/haproxy.cfg hosts.sls files/hosts couchdb.sls files/couchdb/couchdb.service
do
	curl -o $SaltLocal/$FILE -L $SaltRemote/$FILE
done

# static pillars
for FILE in top.sls
do
	curl -o $PillarLocal/$FILE -L $PillarRemote/$FILE
done

# dynamic pillar: haproxy
if [[ ! -e $PillarLocal/haproxy.sls ]]; then
	curl -o $PillarLocal/haproxy.sls -L $PillarRemote/haproxy.sls
	sed -ie s/stats-password:/"stats-password: `generatepw`"/ $PillarLocal/haproxy.sls
fi

# dynamic pillar: couchdb
if [[ ! -e $PillarLocal/couchdb.sls ]]; then
	curl -o $PillarLocal/couchdb.sls -L $PillarRemote/couchdb.sls
	sed -ie s/couchdb-admin-password:/"couchdb-admin-password: `generatepw`"/ $PillarLocal/couchdb.sls
fi

# determine cluster members hostnames and ips
if [ `grep hostname1 /srv/pillar/basics.sls | wc -l` -eq 0 ]; then
	COUNTER=1
	for LOCATION in ${db_HostLocation[@]}
	do
		HOSTNAME=$db_HostPrefix.$LOCATION.$db_Domain
		printf "hostname$COUNTER: $HOSTNAME\n" | tee -a $PillarLocal/basics.sls
		printf "ip$COUNTER: `getip $HOSTNAME`\n" | tee -a $PillarLocal/basics.sls
		let "COUNTER++"
	done
fi

# determine ssl hostname and domain
if [ `grep ssl /srv/pillar/basics.sls | wc -l` -eq 0 ]; then
	SSLHOST=`domainname -f`
	printf "sslhost: $SSLHOST\n" | tee -a $PillarLocal/basics.sls
	SSLDOMAIN=`printf $SSLHOST | rev | awk -F. '{ print $1"."$2 }' | rev`
	printf "ssldomain: $SSLDOMAIN\n" | tee -a $PillarLocal/basics.sls
fi

# create server
salt-call -l info state.highstate
