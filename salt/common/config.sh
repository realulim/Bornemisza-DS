#!/bin/bash

SaltLocal=/srv/salt
PillarLocal=/srv/pillar
SvnTrunk=https://github.com/realulim/Bornemisza/trunk
CFAPI=https://api.cloudflare.com/client/v4/zones

# you have to change the following settings
entrypoint=www.bornemisza.de

app_HostCount=3
app_HostPrefix=app
app_Domain=fra.bornemisza.de
app_publicIpInterface=eth0
app_privateIpInterface=eth1

db_HostLocation=(fra ams lon)
db_HostPrefix=db
db_Domain=bornemisza.de
db_publicIpInterface=eth0
db_privateIpInterface=eth1
# end of user configurable settings

function getip {
	host $1 | cut -d' ' -f4
}

function generatepw {
	openssl rand -hex 12
}

function getprivip {
	VARNAME=$1_privateIpInterface
	ip addr show ${!VARNAME}|grep "inet "|cut -d"/" -f1|cut -d" " -f6
}

function getsecrets {
	arr=("$@")

	EXCL_STR="'"

	for PILLAR in "${arr[@]}";
	do
		VALUE=`/usr/bin/salt-call pillar.get $PILLAR | grep -v local | awk '{$1=$1;print}'`
		EXCL_STR+=$VALUE'|'
	done

	EXCL_STR=${EXCL_STR%?}
	EXCL_STR+="'"
	echo $EXCL_STR
}
