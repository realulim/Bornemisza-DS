#!/bin/bash

cd /opt/scripts
source ./config.sh $1

# create common state tree
mkdir -p $SaltLocal/files/basics
mkdir -p $PillarLocal
#for FILE in	$SaltLocal/basics.sls $SaltLocal/files/basics/bash.sh $SaltLocal/files/basics/cloudflare.sh \
#		$SaltLocal/dns.sls $SaltLocal/letsencrypt.sls $SaltLocal/haproxy.sls
#do
#	curl -L $SaltRemoteRoot/common/$FILE -o $FILE
#done

svn export --force https://github.com/realulim/Bornemisza/trunk/salt/common/srv/salt /srv/salt

# determine my hostname, domain and public ip
if ! grep -qs hostname: $PillarLocal/basics.sls ; then
	VARNAME=$1_publicIpInterface
	HOSTNAME=`domainname -f`
	IP=`ip addr show ${!VARNAME}|grep "inet "|cut -d"/" -f1|cut -d" " -f6`
	printf "hostname: $HOSTNAME\nip: $IP\n" | tee $PillarLocal/basics.sls
	SSLDOMAIN=`printf $entrypoint | rev | awk -F. '{ print $1"."$2 }' | rev`
        printf "ssldomain: $SSLDOMAIN\n" | tee -a $PillarLocal/basics.sls
fi

# determine my private IP
if ! grep -q privip: $PillarLocal/basics.sls ; then
	printf "privip: `getprivip $1`\n" | tee -a $PillarLocal/basics.sls
fi

# specify the service I am providing
if ! grep -q service: $PillarLocal/basics.sls ; then
	printf "service: $1\n" | tee -a $PillarLocal/basics.sls
fi

# ask for Cloudflare API key
if ! grep -q CFKEY: $PillarLocal/basics.sls ; then
	read -p 'Cloudflare API Key: ' CFKEY
	printf "CFKEY: $CFKEY\n" >> $PillarLocal/basics.sls
fi

# ask for Cloudflare email (username of Cloudflare account)
if ! grep -q CFEMAIL: /srv/pillar/basics.sls ; then
	read -p 'Cloudflare Email: ' CFEMAIL
	printf "CFEMAIL: $CFEMAIL\n" >> $PillarLocal/basics.sls
fi

# determine zone id of domain
if ! grep -q CFZONEID: /srv/pillar/basics.sls ; then
	CFZONEID=`/srv/salt/files/basics/cloudflare.sh get-zoneid "$CFAPI" $CFEMAIL $CFKEY`
	printf "CFZONEID: $CFZONEID\n" | tee -a $PillarLocal/basics.sls
	printf "CFAPI: $CFAPI\n" | tee -a $PillarLocal/basics.sls
fi

if [ -e /opt/bootstrap.sh ]; then
	mv /opt/bootstrap.sh /opt/scripts
fi
