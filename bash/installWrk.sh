#!/bin/bash

# Helper script to install wrk on CentOS (for generating load)

yum groupinstall -y "Development Tools" && yum install -y openssl-devel git && cd /opt && git clone https://github.com/wg/wrk.git && cd wrk && make
