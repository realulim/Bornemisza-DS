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

# download and run common script
curl -o ./bootstrap-common.sh $SaltRemoteRoot/common/bootstrap-common.sh
source ./bootstrap-common.sh

# create server
salt-call -l info state.highstate
