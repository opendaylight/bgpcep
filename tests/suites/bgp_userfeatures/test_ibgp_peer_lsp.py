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
#
# Basic tests for iBGP peers.
# Test suite performs basic iBGP functional test case for carrying LSP State
# Information in BGP as described in
# http://tools.ietf.org/html/draft-ietf-idr-te-lsp-distribution-03

import logging
import pytest

from libraries import bgp
from libraries import infra
from libraries import templated_requests
from libraries import utils
from libraries.variables import variables


ODL_IP = variables.ODL_IP
RESTCONF_PORT = variables.RESTCONF_PORT
TOOLS_IP = variables.TOOLS_IP
BGP_TOOL_PORT = variables.BGP_TOOL_PORT
ODL_BGP_PORT = variables.ODL_BGP_PORT
BGP_VARIABLES_FOLDER = "variables/bgpuser/"
COUNT = 1
HOLDTIME = 180
DEFAULT_RIB_CHECK_PERIOD = 1
DEFAULT_RIB_CHECK_COUNTS = 10
BGP_PEER_LOG_LEVEL = "debug"
BGP_PEER_LOG_FILE = "bgp_peer.log"
BGP_PEER_COMMAND = f"python3 tools/fastbgp/play.py --amount {COUNT} --myip={TOOLS_IP} --myport={BGP_TOOL_PORT} --peerip={ODL_IP} --peerport={ODL_BGP_PORT} --{BGP_PEER_LOG_LEVEL} --logfile {BGP_PEER_LOG_FILE} --bgpls True"
BGP_PEER_OPTIONS = f"2>&1 >tmp/{BGP_PEER_LOG_FILE}"
JSONKEYSTR = "linkstate-route"
BGP_PEER_NAME = "example-bgp-peer"
SKIP_PARAMS = "--skipattr"
RIB_INSTANCE = "example-bgp-rib"
PROTOCOL_OPENCONFIG = RIB_INSTANCE
ROUTE_KEY = "AAUAFQcAAAAAAAAAAQECAwQAAQABBQYHCA=="

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=50)
class TestIbgpPeerLsp:
    flowspec_process = None

    def test_ibgp_peer_lsp(self, allure_step_with_separate_logging):
        with allure_step_with_separate_logging("step_tc1_configure_ibgp_peer"):
            """Configure BGP peer module with initiate-connection set to false."""
            mapping = {
                "IP": TOOLS_IP,
                "HOLDTIME": HOLDTIME,
                "PEER_PORT": BGP_TOOL_PORT,
                "PASSIVE_MODE": "true",
                "BGP_RIB_OPENCONFIG": PROTOCOL_OPENCONFIG,
            }
            templated_requests.put_templated_request(
                "variables/bgpuser/bgp_peer", mapping, json=False
            )

        with allure_step_with_separate_logging(
            "step_tc1_check_example_bgp_rib_is_empty"
        ):
            """Check RIB for none linkstate-routes."""
            bgp.check_example_bgp_rib_does_not_contain(JSONKEYSTR)

        with allure_step_with_separate_logging("step_tc1_connect_bgp_peer"):
            """Connect BGP peer with advertising the routes without mandatory params like LOC_PREF."""
            infra.log_message_to_karaf(
                "Error = WELL_KNOWN_ATTR_MISSING is EXPECTED in this test case, and should be thrown when missing mandatory attributes."
            )
            self.bgp_speaker_process = infra.shell(
                f"{BGP_PEER_COMMAND} {SKIP_PARAMS} {BGP_PEER_OPTIONS}",
                run_in_background=True,
            )
            utils.verify_process_did_not_stop_immediately(self.bgp_speaker_process.pid)

        with allure_step_with_separate_logging("step_tc1_check_example_bgp_rib"):
            """Check RIB for not containig linkstate-route(s), because update messages were not good."""
            utils.verify_function_does_not_fail_within_timeout(
                DEFAULT_RIB_CHECK_COUNTS,
                DEFAULT_RIB_CHECK_PERIOD,
                bgp.check_example_bgp_rib_does_not_contain,
                JSONKEYSTR,
            )

        with allure_step_with_separate_logging("step_tc1_disconnect_bgp_peer"):
            """Stop BGP peer & store logs."""
            bgp.stop_bgp_speaker(self.bgp_speaker_process)
            infra.backup_file(
                src_file_name=BGP_PEER_LOG_FILE, target_file_name="tc1_bgp_peer.log"
            )

        with allure_step_with_separate_logging("step_tc1_deconfigure_ibgp_peer"):
            """Revert the BGP configuration to the original state: without any configured peers."""
            mapping = {
                "IP": TOOLS_IP,
                "BGP_RIB_OPENCONFIG": PROTOCOL_OPENCONFIG,
            }
            templated_requests.delete_templated_request(
                "variables/bgpuser/bgp_peer", mapping
            )

        with allure_step_with_separate_logging("step_tc2_configure_ibgp_peer"):
            """Configures BGP peer module with initiate-connection set to false."""
            mapping = {
                "IP": TOOLS_IP,
                "HOLDTIME": HOLDTIME,
                "PEER_PORT": BGP_TOOL_PORT,
                "PASSIVE_MODE": "true",
                "BGP_RIB_OPENCONFIG": PROTOCOL_OPENCONFIG,
            }
            templated_requests.put_templated_request(
                "variables/bgpuser/bgp_peer", mapping, json=False
            )

        with allure_step_with_separate_logging(
            "step_tc2_check_example_bgp_rib_is_empty"
        ):
            """Check RIB for none linkstate-routes."""
            bgp.check_example_bgp_rib_does_not_contain(JSONKEYSTR)

        with allure_step_with_separate_logging("step_tc2_connect_bgp_peer"):
            """Connect BGP peer."""
            self.bgp_speaker_process = infra.shell(
                f"{BGP_PEER_COMMAND} {BGP_PEER_OPTIONS}", run_in_background=True
            )
            utils.verify_process_did_not_stop_immediately(self.bgp_speaker_process.pid)

        with allure_step_with_separate_logging("step_tc2_check_example_bgp_rib"):
            """Check RIB for linkstate-route(s) and check all of their attributes."""
            mapping = {"IP": TOOLS_IP, "ROUTE_KEY": ROUTE_KEY}
            utils.wait_until_function_pass(
                DEFAULT_RIB_CHECK_COUNTS,
                DEFAULT_RIB_CHECK_PERIOD,
                templated_requests.get_templated_request,
                "variables/bgpuser/lsp/effective_rib_in",
                mapping,
                verify=True,
            )

        with allure_step_with_separate_logging("step_tc2_disconnect_bgp_peer"):
            """Stop BGP peer & store logs."""
            bgp.stop_bgp_speaker(self.bgp_speaker_process)
            infra.backup_file(
                src_file_name=BGP_PEER_LOG_FILE, target_file_name="tc2_bgp_peer.log"
            )

        with allure_step_with_separate_logging("step_tc2_deconfigure_ibgp_peer"):
            """Revert the BGP configuration to the original state: without any configured peers."""
            mapping = {
                "IP": TOOLS_IP,
                "BGP_RIB_OPENCONFIG": PROTOCOL_OPENCONFIG,
            }
            templated_requests.delete_templated_request(
                "variables/bgpuser/bgp_peer", mapping
            )
