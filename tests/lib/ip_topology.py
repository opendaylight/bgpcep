#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import datetime
import re
import time
import logging

from lib import templated_requests

ODL_IP = "127.0.0.1"
RESTCONF_PORT = "8181"

log = logging.getLogger(__name__)

def get_ipv4_topology():
    response = templated_requests.get_request(url=f"http://{ODL_IP}:{RESTCONF_PORT}/rests/data/network-topology:network-topology/topology=example-ipv4-topology?content=nonconfig")

    if response.status_code == 401:
        raise Exception("Anuthorized to get the topology")
    if response.status_code == 404:
        raise Exception("Topology not found")

    topology = response.json()

    return topology

def get_ipv4_topology_count():
    topology = get_ipv4_topology()
    topology_count = len(re.findall("'prefix': '", str(topology)))

    return topology_count

def wait_for_ipv4_topology_prefixes_to_become_stable(excluded_value, timeout=120, wait_period=5, consecutive_times_stable_value=4):
    start_time = datetime.datetime.now()
    last_count = None
    stable_times = 1
    while True:
        current_count = get_ipv4_topology_count()
        log.info(f"Waiting for ipv4 topology prefixes to become stable: {current_count=}, {last_count=}, {excluded_value=}, {stable_times=}")
        if current_count != excluded_value and current_count == last_count:
            stable_times += 1
            if stable_times >= consecutive_times_stable_value:
                # topology is stable, no need to continue
                return
        else:
            stable_times = 1
        last_count = current_count
        time.sleep(wait_period)
        if (datetime.datetime.now() - start_time).seconds >= timeout:
            raise Exception(f"Ipv4 topology is not stable after {timeout} seconds")
