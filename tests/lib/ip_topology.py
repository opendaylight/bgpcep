#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import datetime
import gc
import json
import os
import re
import time
import logging

from lib import templated_requests
from lib import utils

ODL_IP = os.environ["ODL_IP"]
RESTCONF_PORT = os.environ["RESTCONF_PORT"]

log = logging.getLogger(__name__)


def get_ipv4_topology() -> json:
    """Returns example ipv4 topology from ODL using RESTCONF.

    Args:
        None

    Returns:
        json: Example ipv4 topology
    """
    topology = templated_requests.get_request(
        url=f"http://{ODL_IP}:{RESTCONF_PORT}/rests/data/network-topology:network-topology/topology=example-ipv4-topology?content=nonconfig",
        expected_code=200,
    )

    return topology.json()


def get_ipv4_topology_prefixes_count():
    """Returns number of prefixes stored in example ipv4 topology.

    Args:
        None

    Returns:
        int: Number of prefixes stored in example ipv4 topology.
    """
    topology = get_ipv4_topology()
    topology_count = len(re.findall("'prefix': '", str(topology)))
    log.info(f"Current example ipv4 topology prefixes count is: {topology_count}")
    gc.collect()

    return topology_count

def check_example_ipv4_topology_contains(string_to_check: str):
    """Check the example-ipv4-topology content for string."""
    topology = get_ipv4_topology()
    assert string_to_check in str(topology), f"example-ipv4-topology does not contain expected '{string_to_check}' substring."

def check_example_ipv4_topology_does_not_contain(string_to_check: str):
    """Check the example-ipv4-topology does not contain the string."""
    topology = get_ipv4_topology()
    assert string_to_check not in str(topology), f"example-ipv4-topology does contain not expected '{string_to_check}' substring."

def wait_for_ipv4_topology_prefixes_to_become_stable(
    excluded_value: int,
    timeout: int = 120,
    wait_period: int = 5,
    consecutive_times_stable_value: int = 4,
):
    """Waits until the number of prefixes in exmaple ipv4 topology stabilizes

    It needs to stabilize on concrete constant vlaue not equal to the
    excluded value within pre-defined timeout. This value need to be stable
    for consecutive number of calls.

    Args:
        excluded_value (int): Value which is not observed as stable.
            (Usually this value is equal to the previous count before
            last operation was executed)
        timetou (int): timeout in seconds
        wait_period (int): Interval between reading change counter values
            in seconds.
        conscutive_times_stable_value: Sets how many consequtive calls to read
            the change counter value needs to return the same value
            to be considered stable.

    Returns:
        int: Number of prefixes observed in example ipv4 topology.
    """
    start_time = datetime.datetime.now()
    last_count = None
    stable_times = 1
    while True:
        current_count = get_ipv4_topology_prefixes_count()
        log.info(
            f"Waiting for ipv4 topology prefixes to become stable: {current_count=}, " \
            f"{last_count=}, {excluded_value=}, {stable_times=}"
        )
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
            raise AssertionError(
                f"Expected Ipv4 topology to be stable after {timeout} seconds"
            )
        
def verify_ip_topology_is_empty():
    templated_requests.get_templated_request("data/bgpuser/empty_topology", None, verify=True)

def wait_until_ip_topology_is_empty(retry_count=20, interval=1):
    utils.wait_until_function_pass(retry_count, interval, verify_ip_topology_is_empty)
