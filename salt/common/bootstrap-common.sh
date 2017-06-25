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
	VARNAME=$1_privateIpInterface
	PRIVIP=`ip addr show ${!VARNAME}|grep "inet "|cut -d"/" -f1|cut -d" " -f6`
	printf "privip: $PRIVIP\n" | tee -a $PillarLocal/basics.sls
fi

if [[ -e /opt/bootstrap.sh ]]; then
	mv /opt/bootstrap.sh /opt/scripts
fi
