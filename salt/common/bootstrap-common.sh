#!/bin/bash

cd /opt/scripts
source ./config.sh $1

# determine my hostname and public ip
if [ `grep hostname: $PillarLocal/basics.sls | wc -l` -eq 0 ]; then
	HOSTNAME=`domainname -f`
	VARNAME=$1_publicIpInterface
	IP=`ip addr show ${!VARNAME}|grep "inet "|cut -d"/" -f1|cut -d" " -f6`
	printf "hostname: $HOSTNAME\nip: $IP\n" | tee $PillarLocal/basics.sls
fi

# determine my private IP
if [ `grep privip: $PillarLocal/basics.sls | wc -l` -eq 0 ]; then
	printf "privip: `getprivip $1`\n" | tee -a $PillarLocal/basics.sls
fi

# ask for Cloudflare API key
if [ `grep CFKEY: $PillarLocal/basics.sls | wc -l` -eq 0 ]; then
	read -p 'Cloudflare API Key: ' CFKEY
	printf "CFKEY: $CFKEY\n" >> $PillarLocal/basics.sls
fi

# ask for Cloudflare email (username of Cloudflare account)
if [ `grep CFEMAIL: /srv/pillar/basics.sls | wc -l` -eq 0 ]; then
	read -p 'Cloudflare Email: ' CFEMAIL
	printf "CFEMAIL: $CFEMAIL\n" >> $PillarLocal/basics.sls
fi

if [[ -e /opt/bootstrap.sh ]]; then
	mv /opt/bootstrap.sh /opt/scripts
fi
