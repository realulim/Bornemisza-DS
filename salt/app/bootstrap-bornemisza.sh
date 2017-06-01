#!/bin/bash

IP=`curl http://ipecho.net/plain`
PRIVIP=`ip route get $app_PrivateNetwork|head -n1|cut -d" " -f7`

cd /opt
source ./config.sh app

# create state tree
mkdir -p $SaltLocal/files/haproxy
mkdir -p $SaltLocal/files/network
mkdir -p $SaltLocal/files/payara
for FILE in top.sls haproxy.sls files/haproxy/haproxy.cfg network.sls files/network/ifcfg-eth1 files/network/bird.conf payara.sls files/payara/payara.service files/payara/hazelcast.xml ufw.sls
do
	curl -o $SaltLocal/$FILE -L $SaltRemote/$FILE
done

# static pillars
for FILE in top.sls
do
	curl -o $PillarLocal/$FILE -L $PillarRemote/$FILE
done

function generatepw {
	openssl rand -hex 12
}

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

# determine cluster members hostnames and ips
if [ `grep hostname1 /srv/pillar/basics.sls | wc -l` -eq 0 ]; then
	for COUNTER in `seq -s' ' 1 $app_HostCount`
	do
		HOSTNAME=$app_HostPrefix$COUNTER.$app_Domain
		printf "hostname$COUNTER: $HOSTNAME\n" | tee -a $PillarLocal/basics.sls
		printf "ip$COUNTER: $IP $HOSTNAME\n" | tee -a $PillarLocal/basics.sls
	done
fi

# determine my position in the cluster
OFFSET=`printf "$PRIVIP" | cut -d'.' -f 4`
PREFIX=`printf "10.99.0.10" | sed -r 's/(.*)\..*/\1/'`
if [ `grep privip: /srv/pillar/basics.sls | wc -l` -eq 0 ]; then
        POS=`grep "$IP" $PillarLocal/basics.sls | grep -v "ip:" | cut -d':' -f1 | sed "s/ip//"`
        printf "privip: $PREFIX.$[$POS + $OFFSET - 1]\n" | tee -a $PillarLocal/basics.sls
fi

# ask for floating IP and determine its hostname and domain
read -p 'floating IP: ' FLOATIP
if [ `grep floatip: /srv/pillar/basics.sls | wc -l` -eq 0 ]; then
	printf "floatip: $FLOATIP\n" >> $PillarLocal/basics.sls
	FLOATHOST=`host $FLOATIP | cut -d' ' -f5 | sed -r 's/(.*)\..*/\1/'`
	printf "floathost: $FLOATHOST\n" | tee -a $PillarLocal/basics.sls
	FLOATDOMAIN=`printf $FLOATHOST | rev | awk -F. '{ print $1"."$2 }' | rev`
	printf "floatdomain: $FLOATDOMAIN\n" | tee -a $PillarLocal/basics.sls
fi

# ask for autonomous system number
read -p 'ASN: ' ASN
if [ `grep ASN: /srv/pillar/basics.sls | wc -l` -eq 0 ]; then
	printf "ASN: $ASN\n" >> $PillarLocal/basics.sls
fi

# ask for BGP password
read -p 'BGP: ' BGP
if [ `grep BGP: /srv/pillar/basics.sls | wc -l` -eq 0 ]; then
	printf "BGP: $BGP\n" >> $PillarLocal/basics.sls
fi

# ask for Cloudflare API key
read -p 'Cloudflare API Key: ' CFKEY
if [ `grep CFKEY: /srv/pillar/basics.sls | wc -l` -eq 0 ]; then
	printf "CFKEY: $CFKEY\n" >> $PillarLocal/basics.sls
fi

# ask for Cloudflare email (username of Cloudflare account)
read -p 'Cloudflare Email: ' CFEMAIL
if [ `grep CFEMAIL: /srv/pillar/basics.sls | wc -l` -eq 0 ]; then
	printf "CFEMAIL: $CFEMAIL\n" >> $PillarLocal/basics.sls
fi

chmod -R 400 $PillarLocal

# create server
salt-call -l info state.highstate
