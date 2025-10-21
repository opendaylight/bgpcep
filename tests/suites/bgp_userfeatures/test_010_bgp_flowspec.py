#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# Test suite performs basic BGP functional test cases for BGP application
# peer operations and checks for IP4 topology updates and updates towards
# BGP peer as follows:
#
# Functional test for bgp flowspec.

import json
import logging
import os
import pytest
import time

from libraries import bgp
from libraries import flowspec
from libraries import infra
from libraries import pcep
from libraries import templated_requests
from libraries import utils
from libraries.variables import variables


ODL_IP = variables.ODL_IP
TOOLS_IP = variables.TOOLS_IP

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=35)
class TestBgpFlowspec:

    def set_bgp_peer_configuration(self):
        mapping = {
            "IP": TOOLS_IP,
            "HOLDTIME": 180,
            "PEER_PORT": 17900,
            "PASSIVE_MODE": "true",
            "BGP_RIB_OPENCONFIG": "example-bgp-rib",
        }
        templated_requests.put_templated_request(
            "variables/bgpflowspec/bgp_peer", mapping, json=False
        )

    def delete_bgp_peer_configuration(self):
        mapping = {"BGP_RIB_OPENCONFIG": "example-bgp-rib", "IP": TOOLS_IP}
        templated_requests.delete_templated_request(
            "variables/bgpflowspec/bgp_peer", mapping
        )

    def prepare_config_files(self):
        infra.shell("cp variables/bgpflowspec/bgp-flowspec-redirect.cfg tmp/")
        infra.shell(
            f"sed -i -e 's/EXABGPIP/{TOOLS_IP}/g' tmp/bgp-flowspec-redirect.cfg"
        )
        infra.shell(f"sed -i -e 's/ODLIP/{ODL_IP}/g' tmp/bgp-flowspec-redirect.cfg")
        infra.shell("cp variables/bgpflowspec/bgp-flowspec.cfg tmp/")
        infra.shell(f"sed -i -e 's/EXABGPIP/{TOOLS_IP}/g' tmp/bgp-flowspec.cfg")
        infra.shell(f"sed -i -e 's/ODLIP/{ODL_IP}/g' tmp/bgp-flowspec.cfg")

    def test_bgp_flowspec(self, allure_step_with_separate_logging):
        with allure_step_with_separate_logging("step_prepare_config_files"):
            self.prepare_config_files()

        with allure_step_with_separate_logging(
            "step_check_for_rmpty_topology_before_talking"
        ):
            """Sanity check bgp-flowspec:flowspec-routes is up but empty."""
            flowspec.wait_until_flowspec_data_is_empty(20, 3)

        with allure_step_with_separate_logging(
            "step_reconfigure_odl_to_accept_connection"
        ):
            """Configure BGP peer module with initiate-connection set to false."""
            self.set_bgp_peer_configuration()

        with allure_step_with_separate_logging("step_flowspec_test_1"):
            """Testing flowspec values for bgp-flowspec.cfg."""
            exabgp_process = bgp.start_exabgp_and_verify_connected(
                "tmp/bgp-flowspec.cfg", TOOLS_IP
            )
            utils.wait_until_function_pass(
                15,
                1,
                templated_requests.get_templated_request,
                "variables/bgpflowspec/bgp_flowspec",
                mapping=None,
                verify=True,
            )
            bgp.stop_exabgp(exabgp_process)
            flowspec.verify_flowspec_data_is_empty()

        with allure_step_with_separate_logging("step_flowspec_test_2"):
            """Testing flowspec values for bgp-flowspec-redirect.cfg."""
            exabgp_process = bgp.start_exabgp_and_verify_connected(
                "tmp/bgp-flowspec-redirect.cfg", TOOLS_IP
            )
            utils.wait_until_function_pass(
                15,
                1,
                templated_requests.get_templated_request,
                "variables/bgpflowspec/bgp_flowspec_redirect",
                mapping=None,
                verify=True,
            )
            bgp.stop_exabgp(exabgp_process)
            flowspec.verify_flowspec_data_is_empty()

        with allure_step_with_separate_logging(
            "step_deconfigure_odl_to_accept_connection"
        ):
            """Deconfigure BGP peer."""
            self.delete_bgp_peer_configuration()
