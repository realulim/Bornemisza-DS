#!/bin/bash

function cmd() {
	# $1 http method (GET, POST, PUT, DELETE)
	# $2 url
	# $3 cfemail
	# $4 cfkey
	# $5 data to send (only with POST or PUT)

	if [ -n "$5" ]; then
		/usr/bin/curl -s -X $1 "$2" -H "X-Auth-Email: $3" -H "X-Auth-Key: $4" -H "Content-Type: application/json" --data "$5"
	else
		/usr/bin/curl -s -X $1 "$2" -H "X-Auth-Email: $3" -H "X-Auth-Key: $4" -H "Content-Type: application/json"
	fi
}

function get-zoneid() {
	local CFAPI=$1; local CFEMAIL=$2; local CFKEY=$3
	cmd GET "$CFAPI" $CFEMAIL $CFKEY | jq '.result|.[]|.id' | tr -d "\""
}

function get-SRV-targets-for-service() {
	local CFAPI=$1; local CFEMAIL=$2; local CFKEY=$3; local CFZONEID=$4; local SERVICENAME=$5
	cmd GET "$CFAPI/$CFZONEID/dns_records" $CFEMAIL $CFKEY | \
	jq -re '.result|.[]|select(.type=="SRV" and .name=="'$SERVICENAME'")|.data.target'
}

function exists-SRV-target-for-service() {
	local CFAPI=$1; local CFEMAIL=$2; local CFKEY=$3; local CFZONEID=$4; local SERVICENAME=$5; local TARGET=$6
	cmd GET "$CFAPI/$CFZONEID/dns_records" $CFEMAIL $CFKEY | \
	jq -re '.result|.[]|select(.type=="SRV" and .name=="'$SERVICENAME'" and .data.target=="'$TARGET'")|""'
}

function get-recordid-for() {
	local CFAPI=$1; local CFEMAIL=$2; local CFKEY=$3; local CFZONEID=$4; local TYPE=$5; local NAME=$6
	cmd GET "$CFAPI/$CFZONEID/dns_records" $CFEMAIL $CFKEY | \
	jq -re '.result|.[]|select(.type=="'$TYPE'" and .name=="'$NAME'")|.id'
}

function host-has-this-A-record() {
	local CFAPI=$1; local CFEMAIL=$2; local CFKEY=$3; local CFZONEID=$4; local NAME=$5; local CONTENT=$6
	cmd GET "$CFAPI/$CFZONEID/dns_records" $CFEMAIL $CFKEY | \
	jq -re '.result|.[]|select(.type=="A" and .name=="'$NAME'" and .content=="'$CONTENT'")'
}

function host-has-other-A-record() {
	local CFAPI=$1; local CFEMAIL=$2; local CFKEY=$3; local CFZONEID=$4; local NAME=$5; local CONTENT=$6
	cmd GET "$CFAPI/$CFZONEID/dns_records" $CFEMAIL $CFKEY | \
	jq -re '.result|.[]|select(.type=="A" and .name=="'$NAME'" and .content!="'$CONTENT'")'
}

function update-A-record() {
	local CFAPI=$1; local CFEMAIL=$2; local CFKEY=$3; local CFZONEID=$4; local HOST=$5; local DATA=$6
	RECORD_ID=`get-recordid-for $CFAPI $CFEMAIL $CFKEY $CFZONEID A $HOST`
	cmd PUT "$CFAPI/$CFZONEID/dns_records/$RECORD_ID" $CFEMAIL $CFKEY '{{ DATA }}'
}

# call arguments verbatim:
"$@"
