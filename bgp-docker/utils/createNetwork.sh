#!/usr/bin/env bash
docker network create --subnet 172.18.0.0/24 --ip-range 172.18.0.255/25 --gateway 172.18.0.254 bgp-network

