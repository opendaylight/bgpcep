#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# These variables are considered global and immutable, so their names are in ALL_CAPS.
#

from netconf_testlib.variables import Variables as NetconfVariables


class Variables(NetconfVariables):
    """
    Defines all global test settings, which can be overridden by environment
    variables.
    """

    BGP_TOOL_PORT: int = 17900
    ODL_BGP_PORT: int = 1790
    RESTCONF_PORT: int = 8181
    RESTCONF_ROOT: str = "rests"
    TOOLS_USER: str = "admin"
    TOOLS_PASSWORD: str = "admin"
    TEST_DURATION_MULTIPLIER: int = 1
    TOPOLOGY_URL: str = "rests/data/network-topology:network-topology/topology"
    DEFAULT_PCEP_STATS_UPDATE_INTERVAL: int = 5
    MAX_HTTP_RESPONSE_BODY_LOG_SIZE: int = 500


variables = Variables()
