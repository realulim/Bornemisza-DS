#!/bin/bash

cd /opt/scripts
source ./config.sh app
sh bootstrap-common.sh app

# create app state tree
svn export --force $SvnTrunk/salt/app/srv/salt /srv/salt
svn export --force $SvnTrunk/html5 /opt/frontend
if [ ! -e $PillarLocal/top.sls ]; then
	svn export --force $SvnTrunk/salt/app/srv/pillar/top.sls /srv/pillar
fi

# dynamic pillar: haproxy
if [ ! -e $PillarLocal/haproxy.sls ]; then
	printf "stats-password: `generatepw`\n" > $PillarLocal/haproxy.sls
fi

# dynamic pillar: payara
if [ ! -e $PillarLocal/payara.sls ]; then
	printf "asadmin-password: `generatepw`\n" > $PillarLocal/payara.sls
	printf "asadmin-master-password: `generatepw`\n" >> $PillarLocal/payara.sls
fi

# ask for CouchDB admin password
if [ ! -e $PillarLocal/couchdb.sls ]; then
	read -s -p 'CouchDB Admin Password: ' COUCH_PW
	printf "couchdb-admin-password: $COUCH_PW\n" > $PillarLocal/couchdb.sls
	printf "\n"
fi

# letsencrypt needs to know the ssl endpoint for creating its certificate
if ! grep -q sslhost: /srv/pillar/basics.sls ; then
	FLOATIP=`getip $entrypoint`
	SSLHOST=`host $FLOATIP | cut -d' ' -f5 | sed -r 's/(.*)\..*/\1/'`
	printf "sslhost: $SSLHOST\n" | tee -a $PillarLocal/basics.sls
fi

# haproxy needs to know all appserver hostnames for load balancing between them
for HOSTNAME in `host -t srv _app._tcp.$domain.|cut -d" " -f8|sort|rev|cut -c2-|rev|paste -s -d" "`
do
	if ! grep -q $HOSTNAME /srv/pillar/appservers.sls ; then
		printf "  $HOSTNAME\n" | tee -a $PillarLocal/appservers.sls
	fi
done

# birdc needs to know the floating ip in order to manage failover routing
if ! grep -q floatip: /srv/pillar/basics.sls ; then
	FLOATIP=`getip $entrypoint`
	printf "floatip: $FLOATIP\n" | tee -a $PillarLocal/basics.sls
fi

# ask for autonomous system number
if ! grep -q ASN: /srv/pillar/basics.sls ; then
	read -s -p 'ASN: ' ASN
	printf "ASN: $ASN\n" >> $PillarLocal/basics.sls
	printf "\n"
fi

# ask for BGP password
if ! grep -q BGP: /srv/pillar/basics.sls ; then
	read -s -p 'BGP: ' BGP
	printf "BGP: $BGP\n" >> $PillarLocal/basics.sls
	printf "\n"
fi

chmod -R 400 $PillarLocal

# collect all pillar secrets in order to exclude them from Salt's output
SECRET_KEYS=(CFEMAIL CFKEY BGP ASN stats-password asadmin-password asadmin-master-password couchdb-admin-password)
EXCL_STR=`getsecrets ${SECRET_KEYS[@]}`

# create server
salt-call -l info state.highstate --force-color |& egrep -v $EXCL_STR
