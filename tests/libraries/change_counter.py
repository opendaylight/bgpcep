#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import datetime
import os
import requests
import time
import logging

from libraries import templated_requests
from libraries.variables import variables

ODL_IP = variables.ODL_IP
TOOLS_IP = variables.TOOLS_IP

log = logging.getLogger(__name__)


def set_change_counter(topology_name: str) -> requests.Response:
    """Sets change counter to track changes for specific topology

    Uses RESTCONF to set the change counter.

    Args:
        topology_name (str): Name of the topology which changes
            should be observed.

    Returns:
        requests.Response: Requests library response as returned by PUT call.
    """
    mapping = {"TOPOLOGY_NAME": topology_name}
    resposne = templated_requests.put_templated_request(
        "variables/bgpuser/change_counter", mapping, json=False
    )

    return resposne


def get_change_count(data_change_counter: str = "data-change-counter") -> int:
    """Returns number of changes observed by specific change_counter in ODL.

    It uses RESTCONF to get the change count.

    Args:
        data_change_counter (str): Name of the specific change counter.

    Returns:
        int: Number of changes observed by the specific change counter.
    """
    response = templated_requests.get_from_uri(
        uri=f"rests/data/data-change-counter:{data_change_counter}?content=nonconfig",
        expected_code=200,
    )
    change_count = response.json()["data-change-counter:data-change-counter"][
        "counter"
    ][0]["count"]
    log.info(f"Current change count is: {change_count}")

    return change_count


def wait_for_change_count_to_become_stable(
    minimum_value: int,
    timeout: int = 500,
    wait_period: int = 20,
    consecutive_times_stable_value: int = 4,
):
    """Waits until the change count stabilizes on concrete vlaue or times out.

    This value need to be stable for consecutive number of calls and it needs
    to be higher or equal to provided mininum_value.

    Args:
        minimum_value (int): Minimum value which is required to be returned
            by the change counter to be accapted.
        timetou (int): timeout in seconds
        wait_period (int): interval between reading change counter values
            in seconds.
        conscutive_times_stable_value: Sets how many consequtive calls to read
            the change counter value should return the same value
            to be considered stable.

    Returns:
        int: Number of changes observed by the specific change counter.
    """
    start_time = datetime.datetime.now()
    last_count = None
    stable_times = 1
    while True:
        current_count = get_change_count()
        log.info(
            f"Waiting for change count to become stable: {current_count=}, " \
            f"{last_count=}, {minimum_value=}, {stable_times=}"
        )
        if current_count >= minimum_value and current_count == last_count:
            stable_times += 1
            if stable_times >= consecutive_times_stable_value:
                # change count is stable, no need to continue
                return
        else:
            stable_times = 1
        last_count = current_count
        time.sleep(wait_period)
        if (datetime.datetime.now() - start_time).seconds >= timeout:
            raise AssertionError(
                f"Expected change count to be stable after {timeout} seconds"
            )
