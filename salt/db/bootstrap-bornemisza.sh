#!/bin/bash

cd /opt/scripts
source ./config.sh db
sh bootstrap-common.sh db

# create state tree
for FILE in top.sls hosts.sls files/hosts cockroachdb.sls
do
	curl -o $SaltLocal/$FILE -L $SaltRemote/$FILE
done

# static pillars
for FILE in top.sls
do
	curl -o $PillarLocal/$FILE -L $PillarRemote/$FILE
done

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
SSLHOST=`domainname -f`
printf "sslhost: $SSLHOST\n" | tee -a $PillarLocal/basics.sls
SSLDOMAIN=`printf $SSLHOST | rev | awk -F. '{ print $1"."$2 }' | rev`
printf "ssldomain: $SSLDOMAIN\n" | tee -a $PillarLocal/basics.sls

# create server
salt-call -l info state.highstate
