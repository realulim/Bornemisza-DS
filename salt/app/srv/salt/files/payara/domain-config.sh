#!/bin/bash

# $1 absolute path to asadmin binary
# $2 absolute path to password file
# $3 couchdb first hostname available via https
# $4 couchdb second hostname available via https
# $5 couchdb third hostname available via https
# Note: the three hostnames can be identical, if client-side load balancing is not necessary

{% set ASADMIN_CMD='$1 --interactive=false --user admin --passwordfile=$2' %}

# create alias for couchdb admin password
{{ ASADMIN_CMD }} delete-password-alias couchdb-admin-password
{{ ASADMIN_CMD }} create-password-alias couchdb-admin-password

# create custom JNDI resource for CouchDB _users database
{{ ASADMIN_CMD }} delete-custom-resource couchdb/Users
{{ ASADMIN_CMD }} create-custom-resource --property service=_db._tcp.bornemisza.de.:db=_users:username=admin:password='\$\{ALIAS\=couchdb-admin-password\}' --restype org.ektorp.CouchDbConnector --factoryclass de.bornemisza.users.da.UsersDbConnectorFactory couchdb/Users
