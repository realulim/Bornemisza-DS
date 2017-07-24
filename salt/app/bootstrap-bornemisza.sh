#!/bin/bash

cd /opt/scripts
source ./config.sh app
sh bootstrap-common.sh app

# create state tree
mkdir -p $SaltLocal/files/haproxy
mkdir -p $SaltLocal/files/maven
mkdir -p $SaltLocal/files/network
mkdir -p $SaltLocal/files/payara
for FILE in top.sls files/haproxy/haproxy.cfg network.sls files/network/ifcfg-eth1 files/network/bird.conf payara.sls files/payara/payara.service files/payara/domain-config.sh files/payara/hazelcast.xml files/payara/jks_import_pem.sh maven.sls files/maven/pom.xml ufw.sls
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

# dynamic pillar: payara
if [[ ! -e $PillarLocal/payara.sls ]]; then
	curl -o $PillarLocal/payara.sls -L $PillarRemote/payara.sls
	sed -ie s/asadmin-password:/"asadmin-password: `generatepw`"/ $PillarLocal/payara.sls
	sed -ie s/asadmin-master-password:/"asadmin-master-password: `generatepw`"/ $PillarLocal/payara.sls
fi

# haproxy needs to know all appserver hostnames for load balancing between them
for COUNTER in `seq -s' ' 1 $app_HostCount`
do
	if [ `grep hostname$COUNTER /srv/pillar/haproxy.sls | wc -l` -eq 0 ]; then
		HOSTNAME=$app_HostPrefix$COUNTER.$app_Domain
		printf "hostname$COUNTER: $HOSTNAME\n" | tee -a $PillarLocal/haproxy.sls
	fi
done

# letsencrypt needs to know the ssl endpoint for creating its certificate
if [ `grep sslhost: /srv/pillar/basics.sls | wc -l` -eq 0 ]; then
	FLOATIP=`getip $entrypoint`
	SSLHOST=`host $FLOATIP | cut -d' ' -f5 | sed -r 's/(.*)\..*/\1/'`
	printf "sslhost: $SSLHOST\n" | tee -a $PillarLocal/basics.sls
fi

# birdc needs to know the floating ip in order to manage failover routing
if [ `grep floatip: /srv/pillar/basics.sls | wc -l` -eq 0 ]; then
	FLOATIP=`getip $entrypoint`
	printf "floatip: $FLOATIP\n" | tee -a $PillarLocal/basics.sls
fi

# ask for autonomous system number
if [ `grep ASN: /srv/pillar/basics.sls | wc -l` -eq 0 ]; then
	read -p 'ASN: ' ASN
	printf "ASN: $ASN\n" >> $PillarLocal/basics.sls
fi

# ask for BGP password
if [ `grep BGP: /srv/pillar/basics.sls | wc -l` -eq 0 ]; then
	read -p 'BGP: ' BGP
	printf "BGP: $BGP\n" >> $PillarLocal/basics.sls
fi

chmod -R 400 $PillarLocal

# create server
salt-call -l info state.highstate
