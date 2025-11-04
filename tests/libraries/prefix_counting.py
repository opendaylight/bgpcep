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
import re
import time
import logging

from libraries import templated_requests
from libraries.variables import variables

ODL_IP = variables.ODL_IP
RESTCONF_PORT = variables.RESTCONF_PORT
PC_NW_TOPOLOGY = f"{variables.REST_API}/network-topology:network-topology/topology"

log = logging.getLogger(__name__)


def get_ipv4_topology(topology: str = "example-ipv4-topology") -> json:
    """Returns example ipv4 topology from ODL using RESTCONF

    Args:
        topology (str): name of the IPv4 topology to be checked.

    Returns:
        json: IPv4 topology
    """
    topology = templated_requests.get_from_uri(
        uri=f"{PC_NW_TOPOLOGY}={topology}?content=nonconfig",
        expected_code=200,
    )

    return topology.json()


def get_ipv4_topology_prefixes_count(topology: str = "example-ipv4-topology"):
    """Returns number of prefixes stored in ipv4 topology

    Args:
        topology (str): name of the IPv4 topology to be checked.

    Returns:
        int: Number of prefixes stored in example ipv4 topology.
    """
    topology = get_ipv4_topology(topology=topology)
    topology_count = len(re.findall("'prefix': '", str(topology)))
    log.info(f"Current example ipv4 topology prefixes count is: {topology_count}")
    gc.collect()

    return topology_count


def check_ipv4_topology_prefixes_count(
    expected_count: int, topology: str = "example-ipv4-topology"
):
    """Check that the count of prefixes matches the expected count

    Args:
        topology (str): name of the IPv4 topology to be checked.

    Returns:
        None
    """
    actual_count = get_ipv4_topology_prefixes_count(topology=topology)
    assert actual_count == expected_count


def check_ipv4_topology_is_empty(topology: str = "example-ipv4-topology"):
    """Verifies if the ipv4 topology does not containe any prefix

    Args:
        topology (str): name of the IPv4 topology to be checked.

    Returns:
        None
    """
    check_ipv4_topology_prefixes_count(0, topology=topology)


def wait_for_ipv4_topology_prefixes_to_become_stable(
    excluded_value: int,
    timeout: int = 120,
    wait_period: int = 5,
    consecutive_times_stable_value: int = 4,
    topology: str = "example-ipv4-topology",
):
    """Waits until the number of prefixes in exmaple ipv4 topology stabilizes

    It needs to stabilize on concrete constant vlaue not equal to the
    excluded value within pre-defined timeout. This value need to be stable
    for consecutive number of calls.

    Args:
        excluded_value (int): Value which is not observed as stable.
            (Usually this value is equal to the previous count before
            last operation was executed)
        timetou (int): timeout in seconds.
        wait_period (int): Interval between reading change counter values
            in seconds.
        conscutive_times_stable_value: Sets how many consequtive calls to read
            the change counter value needs to return the same value
            to be considered stable.
        topology (str): name of the IPv4 topology to be checked.

    Returns:
        int: Number of prefixes observed in example ipv4 topology.
    """
    start_time = datetime.datetime.now()
    last_count = None
    stable_times = 1
    while True:
        current_count = get_ipv4_topology_prefixes_count(topology=topology)
        log.info(
            f"Waiting for ipv4 topology prefixes to become stable: {current_count=}, "
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
