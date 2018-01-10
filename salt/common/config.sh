#!/bin/bash

export SaltLocal=/srv/salt
export PillarLocal=/srv/pillar
export CFAPI=https://api.cloudflare.com/client/v4/zones

# you have to change the following settings
export SvnTrunk=https://github.com/realulim/Bornemisza/trunk

export domain=bornemisza.de
export entrypoint=www.bornemisza.de
export cfns=oswald.ns.cloudflare.com
export cfns2=june.ns.cloudflare.com

export app_publicIpInterface=eth0
export app_privateIpInterface=eth1

export db_publicIpInterface=eth0
export db_privateIpInterface=eth1
# end of user configurable settings

function getip {
	host "$1" | cut -d' ' -f4
}

function generatepw {
	openssl rand -hex 12
}

function getprivip {
	VARNAME=$1_privateIpInterface
	ip addr show "${!VARNAME}"|grep "inet "|cut -d"/" -f1|cut -d" " -f6
}

function getsecrets {
	arr=("$@")

	for PILLAR in "${arr[@]}";
	do
		VALUE=$(/usr/bin/salt-call pillar.get "$PILLAR" | grep -v local | awk '{$1=$1;print}')
		EXCL_STR+=$VALUE'|'
	done

	EXCL_STR=${EXCL_STR%?}
	echo "$EXCL_STR"
}
