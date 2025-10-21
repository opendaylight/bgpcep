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
        
def verify_flowspec_data_is_empty():
    templated_requests.get_templated_request("data/bgpflowspec/empty_route", None, verify=True)

def wait_until_flowspec_data_is_empty(retry_count=20, interval=1):
    utils.wait_until_function_pass(retry_count, interval, verify_flowspec_data_is_empty)
