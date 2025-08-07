#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import datetime
import time
import logging

from lib import infra
from lib import templated_requests

log = logging.getLogger(__name__)

def set_bgp_neighbour(ip, passive_mode=True):
    passive_mode_mapping = "true" if passive_mode else "false"
    mapping = {"IP": ip, "HOLDTIME": 180, "PEER_PORT": 17900 , "PASSIVE_MODE": passive_mode_mapping, "BGP_RIB_OPENCONFIG" :"example-bgp-rib"}
    resposne = templated_requests.put_templated_request("templates/bgp_peer", mapping, json=True)

    return resposne

def set_bgp_neighbour_user(ip, passive_mode=True):
    passive_mode_mapping = "true" if passive_mode else "false"
    mapping = {"IP": ip, "HOLDTIME": 180, "PEER_PORT": 17900 , "PASSIVE_MODE": passive_mode_mapping, "BGP_RIB_OPENCONFIG" :"example-bgp-rib"}
    resposne = templated_requests.put_templated_request("templates/bgp_peer_user", mapping, json=False)

    return resposne

def delete_bgp_neighbour(ip):
    mapping = {"IP": ip, "BGP_RIB_OPENCONFIG" :"example-bgp-rib"}
    resposne = templated_requests.delete_templated_request("templates/bgp_peer", mapping)

    return resposne

def delete_bgp_neighbour_user(ip):
    mapping = {"IP": ip, "BGP_RIB_OPENCONFIG" :"example-bgp-rib"}
    resposne = templated_requests.delete_templated_request("templates/bgp_peer_user", mapping)

    return resposne

def start_bgp_speaker(ammount, insert, withdraw, prefill, update, listen):
    process = infra.shell(f"python play.py --amount {ammount}\
            --myip=127.0.0.1 \
            --myport=17900 \
            --peerip=127.0.0.1 \
            --peerport=1790 \
            --insert={insert} \
            --withdraw={withdraw} \
            --prefill {prefill} \
            --update {update} \
            {"--listen" if listen else ""} \
            --info >tmp/play.py.out 2>&1",
            run_in_background=True)
    
    return process
