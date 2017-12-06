#!/bin/bash

cd /opt/scripts || exit -1
source ./config.sh $1

# create common state tree
mkdir -p $PillarLocal

svn export --force $SvnTrunk/salt/common/srv/salt $SaltLocal
chmod u+x /srv/salt/files/basics/cloudflare.sh

# put svn trunk url in pillar for subsequent checkouts via salt
if [ ! -e $PillarLocal/basics.sls ]; then
	printf "svn: %s\n" "$SvnTrunk" | tee $PillarLocal/basics.sls
fi

# determine my hostname, domain and public ip
if ! grep -q hostname: $PillarLocal/basics.sls ; then
	VARNAME=$1_publicIpInterface
	HOSTNAME=$(domainname -f)
	printf "hostname: %s\n" "$HOSTNAME" | tee -a $PillarLocal/basics.sls
	IP=$(ip addr show "${!VARNAME}"|grep "inet "|cut -d"/" -f1|cut -d" " -f6)
	printf "ip: %s\n" "$IP" | tee -a $PillarLocal/basics.sls
	SSLDOMAIN=$(printf "%s" "$domain")
	printf "ssldomain: %s\n" "$SSLDOMAIN" | tee -a $PillarLocal/basics.sls
	printf "cfns: %s\n" "$cfns" | tee -a $PillarLocal/basics.sls
	printf "cfns2: %s\n" "$cfns2" | tee -a $PillarLocal/basics.sls
fi

# determine my private IP
if ! grep -q privip: $PillarLocal/basics.sls ; then
	printf "privip: %s\n" "$(getprivip "$1")" | tee -a $PillarLocal/basics.sls
fi

# specify the service I am providing
if ! grep -q service: $PillarLocal/basics.sls ; then
	printf "service: %s\n" "$1" | tee -a $PillarLocal/basics.sls
fi

# cluster sizes need to be rewritten on every run
APPCLUSTERSIZE=$(host -t srv _app._tcp.$domain. $cfns|grep SRV|wc -l)
DBCLUSTERSIZE=$(host -t srv _db._tcp.$domain. $cfns|grep SRV|wc -l)
printf "appclustersize: %s\ndbclustersize: %s\n" "$APPCLUSTERSIZE" "$DBCLUSTERSIZE" | tee $PillarLocal/cluster.sls

# ask for Cloudflare API key
if ! grep -q CFKEY: $PillarLocal/basics.sls ; then
	read -rs -p 'Cloudflare API Key: ' CFKEY
	printf "CFKEY: %s\n" "$CFKEY" >> $PillarLocal/basics.sls
	printf "\n"
fi

# ask for Cloudflare email (username of Cloudflare account)
if ! grep -q CFEMAIL: $PillarLocal/basics.sls ; then
	read -rs -p 'Cloudflare Email: ' CFEMAIL
	printf "CFEMAIL: %s\n" "$CFEMAIL" >> $PillarLocal/basics.sls
	printf "\n"
fi

# determine zone id of domain
if ! grep -q CFZONEID: $PillarLocal/basics.sls ; then
	CFZONEID=$(/srv/salt/files/basics/cloudflare.sh get-zoneid "$CFAPI" "$CFEMAIL" "$CFKEY")
	printf "CFZONEID: %s\n" "$CFZONEID" | tee -a $PillarLocal/basics.sls
	printf "CFAPI: %s\n" "$CFAPI" | tee -a $PillarLocal/basics.sls
fi

if [ -e /opt/bootstrap.sh ]; then
	mv /opt/bootstrap.sh /opt/scripts
fi
