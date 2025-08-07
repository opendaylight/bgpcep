import datetime
import time
import logging

from lib import infra
from lib import templated_requests

log = logging.getLogger(__name__)

def set_bgp_neighbour(ip, passive_mode=True):
    mapping = {"IP": ip, "HOLDTIME": 180, "PEER_PORT": 17900 , "PASSIVE_MODE": passive_mode, "BGP_RIB_OPENCONFIG" :"example-bgp-rib"}
    resposne = templated_requests.put_templated_request("templates/bgp_peer", mapping)

    return resposne

def delete_bgp_neighbour(ip):
    mapping = {"IP": ip, "BGP_RIB_OPENCONFIG" :"example-bgp-rib"}
    resposne = templated_requests.delete_templated_request("templates/bgp_peer", mapping)

    return resposne

def start_bgp_speaker(ammount, insert, withdraw, prefill, update):
    process = infra.shell(f"python play.py --amount {ammount}\
            --myip=127.0.0.1 \
            --myport=17900 \
            --peerip=127.0.0.1 \
            --peerport=1790 \
            --insert={insert} \
            --withdraw={withdraw} \
            --prefill {prefill} \
            --update {update} \
            --info >tmp/play.py.out 2>&1",
            run_in_background=True)
    
    return process
