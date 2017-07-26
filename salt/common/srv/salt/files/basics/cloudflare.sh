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
	CFAPI=$1; CFEMAIL=$2; CFKEY=$3
	cmd GET "$CFAPI" $CFEMAIL $CFKEY | jq '.result|.[]|.id' | tr -d "\""
}

function get-srv-targets-for-service() {
	CFAPI=$1; CFEMAIL=$2; CFKEY=$3; CFZONEID=$4; SERVICENAME=$5
	cmd GET "$CFAPI/$CFZONEID/dns_records" $CFEMAIL $CFKEY | \
	jq -re '.result|.[]|select(.type=="SRV" and .name=="'$SERVICENAME'")|.data.target'
}

function exists-srv-target-for-service() {
	CFAPI=$1; CFEMAIL=$2; CFKEY=$3; CFZONEID=$4; SERVICENAME=$5; TARGET=$6
	cmd GET "$CFAPI/$CFZONEID/dns_records" $CFEMAIL $CFKEY | \
	jq -re '.result|.[]|select(.type=="SRV" and .name=="'$SERVICENAME'" and .data.target=="'$TARGET'")|""'
}

function get-recordid-for() {
	CFAPI=$1; CFEMAIL=$2; CFKEY=$3; CFZONEID=$4; TYPE=$5; NAME=$6
	cmd GET "$CFAPI/$CFZONEID/dns_records" $CFEMAIL $CFKEY | \
	jq -re '.result|.[]|select(.type=="'$TYPE'" and .name=="'$NAME'")|.id'
}

# DATA='{"type":"A","name":"'$HOST'","content":"127.0.0.1","ttl":1,"proxied":false}'

#RECORDID=`get-recordid-for A $HOST`
#if [ -n "$RECORDID" ]; then
#	cmd DELETE "$CFAPI/$CFZONEID/dns_records/$RECORDID" $CFEMAIL $CFKEY | jq -re '.success'
#	echo "Record deleted"
#fi

#RECORDID=`get-recordid-for A $HOST`
#if [ -z "$RECORDID" ]; then
#	cmd POST "$CFAPI/$CFZONEID/dns_records" $CFEMAIL $CFKEY $DATA | jq -re '.success'
#	echo "Record created"
#fi

#RECORDID=`get-recordid-for A $HOST`
#if [ -n "$RECORDID" ]; then
#	cmd PUT "$CFAPI/$CFZONEID/dns_records/$RECORDID" $CFEMAIL $CFKEY $DATA | jq -re '.success'
#	echo "Record updated"
#fi

# call arguments verbatim:
"$@"
