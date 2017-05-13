#!/bin/bash

cd /opt
source ./config.sh db

# create state tree
for FILE in top.sls hosts.sls files/hosts
do
	curl -o $SaltLocal/$FILE -L $SaltRemote/$FILE
done

# static pillars
for FILE in top.sls
do
        curl -o $PillarLocal/$FILE -L $PillarRemote/$FILE
done

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
