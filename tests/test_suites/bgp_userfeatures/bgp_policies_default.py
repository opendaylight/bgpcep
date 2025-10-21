#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# Functional test for bgp routing policies
#
# This suite uses 7 peers: app peer, 2x rr-client, 2x ebgp, 2x ibgp
# Tests results on effective-rib-in dependant on their respective
# configurations. Peers 1,2,4,5 are testing multiple ipv4 routes with
# additional arguments. Peers 3,6 have ipv4 and ipv6 mpls-labeled routes.

import json
import logging
import os
import pytest
import time

from lib import bgp
from lib import infra
from lib import karaf
from lib import pcep
from lib import templated_requests
from lib import ip_topology
from lib import utils


ODL_IP = os.environ["ODL_IP"]
TOOLS_IP = os.environ["TOOLS_IP"]
POLICIES_VAR = "data/bgpfunctional/bgppolicies"
CMD = "env exabgp.tcp.port=1790 exabgp --debug"
PEER_TYPES = ("ibgp_peer", "ibgp_peer", "ebgp_peer", "ebgp_peer", "rr_client_peer", "rr_client_peer")
HOLDTIME = 180
RIB_INSTANCE = "example-bgp-rib"

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=15)
class TestBgpPoliciesDefault:
    exabgp_processes = []

    def setup_config_files(self):
        """Uploads exabgp config files and replaces variables within those
        config files with desired values."""
        for index in range(1, 7):
            config_file_name = f"exabgp{index}.cfg"
            infra.shell(f"cp {POLICIES_VAR}/exabgp_configs/{config_file_name} tmp/")
            infra.shell(f"sed -i -e 's/ODLIP/{ODL_IP}/g' tmp/{config_file_name}")
            infra.shell(f"sed -i -e 's/ROUTEREFRESH/disable/g' tmp/{config_file_name}")
            infra.shell(f"sed -i -e 's/ADDPATH/disable/g' tmp/{config_file_name}")
            rc, stdout = infra.shell(f"cat tmp/{config_file_name}")
            log.info(stdout)

    def verify_rib_status(self):
        """Verify output from effective-rib-in for each of the 6 exabgp peers and app peer.
        First request is for full example-bgp-rib and it's output is logged for debug purposes.
        Each of the peers have different output which is stored in folder by their respective
        numbers as peer_${index} (peer_1, peer_2 ...)"""
        # gets and outputs full rib output for debug purposes if one of the peers reports faulty data.
        response = templated_requests.get_templated_request(f"{POLICIES_VAR}/rib_state", dict())
        log.info(response.json())
        for index in range(1, 7):
            mapping = {"IP": f"127.0.0.{index+1}"}
            utils.wait_until_function_pass(5, 3, templated_requests.get_templated_request, f"{POLICIES_VAR}/effective_rib_in/peer_{index}", mapping, verify=True)
        # application peer verification
        mapping = {"IP": ODL_IP}
        utils.wait_until_function_pass(5, 3, templated_requests.get_templated_request, f"{POLICIES_VAR}/app_peer_rib", mapping, verify=True)

    def verify_rib_status_empty(self):
        """Checks that example-ipv4-topology is ready, and therefore full rib is ready to be configured."""
        utils.wait_until_function_pass(20, 3, templated_requests.get_templated_request, f"{POLICIES_VAR}/topology_state", dict(), verify=True)

    def test_bgp_policies_default(self, allure_step_with_separate_logging):

        with allure_step_with_separate_logging("step_test_suite_setup"):
            self.setup_config_files()

        with allure_step_with_separate_logging("step_verify_rib_empty"):
            """Checks empty example-ipv4-topology ready."""
            self.verify_rib_status_empty()

        with allure_step_with_separate_logging("step_configure_app_peer"):
            """Configures bgp application peer, and configures it's routes."""
            mapping = {"IP": ODL_IP,"BGP_RIB_OPENCONFIG": "example-bgp-rib"}
            templated_requests.post_templated_request(f"{POLICIES_VAR}/app_peer", mapping, json=False)
            templated_requests.post_templated_request(f"{POLICIES_VAR}/app_peer_route", mapping, json=False)
            
        with allure_step_with_separate_logging("step_reconfigure_odl_to_accept_connections"):
            """Configure BGP peer modules with initiate-connection set to false.
            Configures 6 different peers, two internal, two external and two route-reflectors."""
            for index, peer_type in enumerate(PEER_TYPES):
                mapping = {"IP": f"127.0.0.{index+2}", "BGP_RIB_OPENCONFIG": RIB_INSTANCE, "RIB_INSTACE_NAME": RIB_INSTANCE, "HOLDTIME": HOLDTIME, "PASSIVE_MODE": "true"}
                templated_requests.put_templated_request(f"{POLICIES_VAR}/{peer_type}", mapping, json=False)
            
        with allure_step_with_separate_logging("step_start_exabgps"):
            """Start 6 exabgps as processes in background, each with it's own configuration."""
            for index in range(1, 7):
                exabgp_process = bgp.start_exabgp(f"tmp/exabgp{index}.cfg", log_file=f"exa{index}.log")
                self.exabgp_processes.append(exabgp_process)
            
        with allure_step_with_separate_logging("step_verify_rib_filled"):
            """Verifies that sent routes are present in particular ribs."""
            self.verify_rib_status()

        with allure_step_with_separate_logging("step_stop_all_peers"):
            """Send command to kill all exabgp processes."""
            bgp.kill_all_bgp_speakers()
            for index in range(1, 7):
                infra.shell(f"cp tmp/exa{index}.log results/")

        with allure_step_with_separate_logging("step_delete_bgp_peer_configuration"):
            """Revert the BGP configuration to the original state: without any configured peers."""
            for index, peer_type in enumerate(PEER_TYPES):
                mapping = {"IP": f"127.0.0.{index+2}", "BGP_RIB_OPENCONFIG": RIB_INSTANCE, "RIB_INSTACE_NAME": RIB_INSTANCE}
                templated_requests.delete_templated_request(f"{POLICIES_VAR}/{peer_type}", mapping)
            
        with allure_step_with_separate_logging("step_deconfigure_app_peer"):
            """Revert the BGP configuration to the original state: without application peer."""
            mapping = {"IP": ODL_IP,"BGP_RIB_OPENCONFIG": "example-bgp-rib"}
            templated_requests.delete_templated_request(f"{POLICIES_VAR}/app_peer", mapping)
            templated_requests.delete_templated_request(f"{POLICIES_VAR}/app_peer_route", mapping)
