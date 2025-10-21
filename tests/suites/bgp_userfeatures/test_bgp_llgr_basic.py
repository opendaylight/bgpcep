#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# This suite tests simple connection between one ibgp peer (goabgp) and Odl. 
# Peer is configured with ipv6, and gobgp connectes to odl via ipv6.

import json
import logging
import os
import pytest
import time

from libraries import bgp
from libraries import infra
from libraries import pcep
from libraries import templated_requests
from libraries import utils
from libraries.variables import variables


ODL_IP = variables.ODL_IP
TOOLS_IP = variables.TOOLS_IP
GOBGP_CFG = "gobgp.cfg"
GOBGP_BINARY_URL = "https://github.com/osrg/gobgp/releases/download/v2.18.0/gobgp_2.18.0_linux_386.tar.gz"
FILE_NAME = "gobgp_2.18.0_linux_386.tar.gz"

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=15)
class TestBgpLlgrBasic:
    gobgp_process = None

    def reconfigure_odl_to_accept_connections(self):
        mapping = {"BGP_RIB_OPENCONFIG": "example-bgp-rib",
                   "IP": TOOLS_IP,
                   "HOLDTIME": 180, 
                   "PEER_PORT": 17900,
                   "PASSIVE_MODE": "true"}
        templated_requests.put_templated_request("variables/bgpfunctional/ipv6/bgp_peer", mapping, json=False)
    
    def delete_bgp_peer_configuration(self):
        mapping = {"BGP_RIB_OPENCONFIG": "example-bgp-rib", "IP":TOOLS_IP }
        templated_requests.delete_templated_request("variables/bgpfunctional/ipv6/bgp_peer", mapping)

    def prepare_gobgp_config_file(self):
        infra.shell("cp variables/bgpfunctional/gobgp/gobgp.cfg tmp/")
        infra.shell(f"sed -i -e 's/GOBGPIP/{TOOLS_IP}/g' tmp/gobgp.cfg")
        infra.shell(f"sed -i -e 's/ODLIP/{ODL_IP}/g' tmp/gobgp.cfg")
        infra.shell(f"sed -i -e 's/ROUTERID/{TOOLS_IP}/g' tmp/gobgp.cfg")
        infra.shell("sed -i -e 's/ROUTEREFRESH/disable/g' tmp/gobgp.cfg")
        infra.shell("sed -i -e 's/ADDPATH/disable/g' tmp/gobgp.cfg")
        rc, stdout = infra.shell("cat tmp/gobgp.cfg")
        log.info(f"Updated tmp/exa-md5.cfg config:\n{stdout}")

    def test_bgp_llgr_basic(self, allure_step_with_separate_logging):

        with allure_step_with_separate_logging("step_setup_gobgp"):
            """???."""
            infra.download_file(GOBGP_BINARY_URL)
            infra.shell(f"tar -xzf tmp/{FILE_NAME} -C tmp")
            self.prepare_gobgp_config_file()

        with allure_step_with_separate_logging("step_reconfigure_odl_to_accept_connections"):
            """Configure BGP peer modules with initiate-connection set to false with short ipv6 address."""
            self.reconfigure_odl_to_accept_connections()
        
        with allure_step_with_separate_logging("step_start_gobgp"):
            """Starts gobgp peer simulator."""
            self.gobgp_process = bgp.start_gobgp_and_verify_connected(f"tmp/gobgpd", f"tmp/{GOBGP_CFG}", TOOLS_IP)

        with allure_step_with_separate_logging("step_delete_bgp_peer_configuration"):
            """Revert the BGP configuration to the original state: without any configured peers."""
            self.delete_bgp_peer_configuration()

        with allure_step_with_separate_logging("step_stop_gobgp"):
            """Save gobgp logs as gobgp.log, and stop gobgp with SIGINT bash signal"""
            infra.shell("cp tmp/gobgp.log results/")
            bgp.stop_gobgp(self.gobgp_process)

