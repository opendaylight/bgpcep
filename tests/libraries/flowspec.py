#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import logging

from libraries import templated_requests
from libraries import utils
from libraries.variables import variables

ODL_IP = variables.ODL_IP
RESTCONF_PORT = variables.RESTCONF_PORT
TOOLS_IP = variables.TOOLS_IP

log = logging.getLogger(__name__)


def verify_flowspec_data_is_empty(host: str = ODL_IP):
    """Verifies loc-rib does not contain any bgp-flowspec data.

    Args:
        None
        host (str): Node to send the request to. Defaults to ODL_IP, which
            is node 1 in a cluster and the only node otherwise; pass another
            address from variables.CLUSTER_MEMBER_IPS to target a different
            cluster member.

    Returns:
        None
    """
    templated_requests.get_templated_request(
        "variables/bgpflowspec/empty_route", None, verify=True,
        host=host,
    )


def wait_until_flowspec_data_is_empty(
    retry_count: int = 20, interval: int = 1, host: str = ODL_IP
):
    """Repeatedly checks loc-rib until it does not contain any bgp-flowspec
    data.

    Args:
        retry_count (int): Maximum number of retries.
        interval (int): Time delay in seconds between each retry.
        host (str): Node to send the request to. Defaults to ODL_IP, which
            is node 1 in a cluster and the only node otherwise; pass another
            address from variables.CLUSTER_MEMBER_IPS to target a different
            cluster member.

    Returns:
        None
    """
    utils.wait_until_function_pass(
        retry_count, interval, verify_flowspec_data_is_empty, host=host
    )
