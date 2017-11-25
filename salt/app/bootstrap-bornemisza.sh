#!/bin/bash

cd /opt/scripts || exit -1
source ./config.sh app
sh bootstrap-common.sh app

# create app state tree
svn export --force $SvnTrunk/salt/app/srv/salt $SaltLocal
svn export --force $SvnTrunk/html5 /opt/frontend

if [ ! -e $PillarLocal/top.sls ]; then
	svn export --force $SvnTrunk/salt/app/srv/pillar/top.sls $PillarLocal
	svn export --force $SvnTrunk/salt/app/srv/pillar/appservers.sls $PillarLocal
fi

# dynamic pillar: haproxy
if [ ! -e $PillarLocal/haproxy.sls ]; then
	printf "stats-password: %s\n" "$(generatepw)" > $PillarLocal/haproxy.sls
fi

# dynamic pillar: payara
if [ ! -e $PillarLocal/payara.sls ]; then
	printf "asadmin-password: %s\n" "$(generatepw)" > $PillarLocal/payara.sls
	printf "asadmin-master-password: %s\n" "$(generatepw)" >> $PillarLocal/payara.sls
fi

# ask for CouchDB admin password
if [ ! -e $PillarLocal/couchdb.sls ]; then
	read -rs -p 'CouchDB Admin Password: ' COUCH_PW
	printf "couchdb-admin-password: %s\n" "$COUCH_PW" > $PillarLocal/couchdb.sls
	printf "\n"
fi

# letsencrypt needs to know the ssl endpoint for creating its certificate
if ! grep -q sslhost: $PillarLocal/basics.sls ; then
	FLOATIP=$(getip $entrypoint)
	SSLHOST=$(host "$FLOATIP" | cut -d' ' -f5 | sed -r 's/(.*)\..*/\1/')
	printf "sslhost: %s\n" "$SSLHOST" | tee -a $PillarLocal/basics.sls
fi

# haproxy needs to know all appserver hostnames for load balancing between them
sed -i '/  - .*/d' $PillarLocal/appservers.sls
for HOSTNAME in $(host -t srv _app._tcp.$domain. $cfns|cut -d" " -f8|sort|rev|cut -c2-|rev|paste -s -d" ")
do
	if ! grep -q "$HOSTNAME" $PillarLocal/appservers.sls ; then
		printf "  - %s\n" "$HOSTNAME" | tee -a $PillarLocal/appservers.sls
	fi
done

# birdc needs to know the floating ip in order to manage failover routing
if ! grep -q floatip: $PillarLocal/basics.sls ; then
	FLOATIP=$(getip $entrypoint)
	printf "floatip: %s\n" "$FLOATIP" | tee -a $PillarLocal/basics.sls
fi

# ask for autonomous system number
if ! grep -q ASN: $PillarLocal/basics.sls ; then
	read -rs -p 'ASN: ' ASN
	printf "ASN: %s\n" "$ASN" >> $PillarLocal/basics.sls
	printf "\n"
fi

# ask for BGP password
if ! grep -q BGP: $PillarLocal/basics.sls ; then
	read -rs -p 'BGP: ' BGP
	printf "BGP: %s\n" "$BGP" >> $PillarLocal/basics.sls
	printf "\n"
fi

chmod -R 400 $PillarLocal

# collect all pillar secrets in order to exclude them from Salt's output
SECRET_KEYS=(CFEMAIL CFKEY BGP ASN stats-password asadmin-password asadmin-master-password couchdb-admin-password)
EXCL_STR=$(getsecrets "${SECRET_KEYS[@]}")

# create server
salt-call -l info state.highstate --force-color |& grep -E -v "$EXCL_STR"
