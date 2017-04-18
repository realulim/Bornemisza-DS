#!/bin/bash

cd /opt
source ./config.sh db

# create server
salt-call state.highstate
