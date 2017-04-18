#!/bin/bash

source ../config.sh db

# create server
salt-call state.highstate
