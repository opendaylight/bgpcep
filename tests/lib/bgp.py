#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import datetime
import signal
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

def set_bgp_neighbours_user(ip_prefix, count, passive_mode=True):
    passive_mode_mapping = "true" if passive_mode else "false"
    for i in range(1, count + 1):
        mapping = {"IP": ip_prefix + "." + str(i), "HOLDTIME": 180, "PEER_PORT": 17900 , "PASSIVE_MODE": passive_mode_mapping, "BGP_RIB_OPENCONFIG" :"example-bgp-rib"}
        templated_requests.put_templated_request("templates/bgp_peer_user", mapping, json=False)

def set_bgp_application_peer(ip):
    mapping = {"IP": ip, "BGP_RIB_OPENCONFIG" :"example-bgp-rib"}
    resposne = templated_requests.put_templated_request("templates/bgp_application_peer", mapping, json=False)

    return resposne

def delete_bgp_application_peer(ip):
    mapping = {"IP": ip, "BGP_RIB_OPENCONFIG" :"example-bgp-rib"}
    resposne = templated_requests.delete_templated_request("templates/bgp_application_peer", mapping, json=False)

    return resposne

def delete_bgp_neighbour(ip):
    mapping = {"IP": ip, "BGP_RIB_OPENCONFIG" :"example-bgp-rib"}
    resposne = templated_requests.delete_templated_request("templates/bgp_peer", mapping)

    return resposne

def delete_bgp_neighbour_user(ip):
    mapping = {"IP": ip, "BGP_RIB_OPENCONFIG" :"example-bgp-rib"}
    resposne = templated_requests.delete_templated_request("templates/bgp_peer_user", mapping)

    return resposne

def delete_bgp_neighbours_user(ip_prefix, count):
    for i in range(1, count + 1):
        mapping = {"IP": ip_prefix + "." + str(i), "BGP_RIB_OPENCONFIG" :"example-bgp-rib"}
        templated_requests.put_templated_request("templates/bgp_peer_user", mapping, json=False)

def start_bgp_speaker(ammount, insert=None, withdraw=None, prefill=None, update=None, multiplicity=None, listen=None):
    command_parts = [f"python tools/play.py --amount {ammount}\
            --myip=127.0.0.1 \
            --myport=17900 \
            --peerip=127.0.0.1 \
            --peerport=1790"]
    
    if insert is not None:
        command_parts.append(f"--insert={insert}")
    if withdraw is not None:
        command_parts.append(f"--withdraw={withdraw}")
    if prefill is not None:
        command_parts.append(f"--prefill={prefill}")
    if update is not None:
        command_parts.append(f"--update={update}")
    if multiplicity is not None:
        command_parts.append(f"--multiplicity={multiplicity}")
    if listen:
        command_parts.append("--listen")

    command_parts.append("--info >tmp/play.py.out 2>&1")
    
    process = infra.shell(" ".join(command_parts),
            run_in_background=True)
    
    return process

def stop_bgp_speaker(process):
    process.send_signal(signal.SIGINT)
    # dump BGP speaker logs
    infra.shell("cat play.py.out")

def start_bgp_app_peer(count=None, command="post", prefix="8.0.0.0", prefix_len=28, uri="data/bgp-rib:application-rib=10.0.0.10/tables=bgp-types%3Aipv4-address-family,bgp-types%3Aunicast-subsequent-address-family", log_level="info", timeout=1200):
    
    process = infra.shell(f"python tools/bgp_app_peer.py \
            {'--count ' +  str(count) if count is not None else ''}\
            --host 127.0.0.1 \
            --port 8181 \
            --command {command} \
            --prefix {prefix} \
            --prefixlen {prefix_len} \
            {'--' + log_level + ' ' if log_level is not None else ''} \
            --uri {uri} \
            --xml tools/ipv4-routes-template.xml \
            >bgp_app_peer.log 2>&1",
            timetout=timeout,
            run_in_background=False)
    
    return process
