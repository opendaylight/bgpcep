#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import datetime
import json
import re
import time
import logging

from lib import infra
from lib import templated_requests

ODL_IP = "127.0.0.1"
RESTCONF_PORT = "8181"

log = logging.getLogger(__name__)

def set_change_counter(topology_name):
    mapping = {"TOPOLOGY_NAME": topology_name}
    resposne = templated_requests.put_templated_request("templates/change_counter", mapping, json=False)

    return resposne


def get_change_count():
    response = templated_requests.get_request(url=f"http://{ODL_IP}:{RESTCONF_PORT}/rests/data/data-change-counter:data-change-counter?content=nonconfig")
    if response.status_code == 401:
        raise Exception("Anuthorized to get the change count")
    if response.status_code == 404:
        raise Exception("Change count not found")
    log.info(f"{response.request.headers=}")
    log.info(f"{response.text=}")
    log.info(f"{response.status_code=}")
    change_count = response.json()["data-change-counter:data-change-counter"]["counter"][0]["count"]

    return change_count

def wait_for_change_count_to_become_stable(minimum_value, timeout=120, wait_period=5, consecutive_times_stable_value=4):
    start_time = datetime.datetime.now()
    last_count = None
    stable_times = 1
    while True:
        current_count = get_change_count()
        log.info(f"Waiting for change count to become stable: {current_count=}, {last_count=}, {minimum_value=}, {stable_times=}")
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
            raise Exception(f"Change count is not stable after {timeout} seconds")