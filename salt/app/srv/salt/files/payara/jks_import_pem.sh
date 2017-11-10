#!/bin/bash
PEM_FILE=$1
PASSWORD=$2
KEYSTORE=$3

# number of certs in the PEM file
CERTS=$(grep -c 'END CERTIFICATE' "$PEM_FILE")

# For every cert in the PEM file, extract it and import into the JKS keystore
# awk command: step 1, if line is in the desired cert, print the line
#              step 2, increment counter when last line of cert is found
for N in $(seq 0 $((CERTS - 1))); do
	ALIAS="${PEM_FILE%.*}-$N"
	if keytool -list -keystore "$KEYSTORE" -storepass "$PASSWORD" | grep "$ALIAS" ; then
		keytool -delete -alias "$ALIAS" -keystore "$KEYSTORE" -storepass "$PASSWORD"
	fi
	< "$PEM_FILE" awk "n==$N { print }; /END CERTIFICATE/ { n++ }" |
		keytool -noprompt -import -trustcacerts -alias "$ALIAS" -keystore "$KEYSTORE" -storepass "$PASSWORD"
done
