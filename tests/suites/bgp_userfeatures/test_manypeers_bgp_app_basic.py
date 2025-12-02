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
# Test case 1: Initial BGP peer connection with pre-filled topology,
# POST and simple DELETE requests used.
# BGP_Application_Peer_Post_3_Initial_Routes,
# Check_Example-IPv4-Topology_Is_Filled_With_3_Routes,Connect_BGP_Peer,
# BGP_Peer_Check_Incomming_Updates_For_3_Introduced_Prefixes
# BGP_Application_Peer_Delete_3_Initial_Routes,
# Check_Example-IPv4-Topology_Is_Empty,
# Peer_Check_Incomming_Updates_For_3_Withdrawn_Prefixes, Stop_BGP_Peer.
#
# Test case 2: PUT and DELETE all routes requests while BGP peer
# is connected. Reconnect_BGP_Peer, BGP_Application_Peer_Put_3_Routes,
# Check_Example-IPv4-Topology_Is_Filled_With_3_Routes,
# BGP_Peer_Check_Incomming_Updates_For_3_Introduced_Prefixes,
# BGP_Application_Peer_Delete_All_Routes,
# Check_Example-IPv4-Topology_Is_Empty,
# BGP_Peer_Check_Incomming_Updates_For_3_Withdrawn_Prefixes, Stop_BGP_Peer.
#
# Test case 3: Repeated BGP peer re-connection with pre-filled topology.
# BGP_Application_Peer_Put_3_Routes,
# Check_Example-IPv4-Topology_Is_Filled_With_3_Routes,
# Reconnect_BGP_Peer_And_Check_Incomming_Updates_For_3_Introduced_Prefixes,
# BGP_Application_Peer_Delete_All_Routes,
# Check_Example-IPv4-Topology_Is_Empty,
# BGP_Peer_Check_Incomming_Updates_For_3_Withdrawn_Prefixes, Stop_BGP_Peer.

import logging
import pytest

from libraries import bgp
from libraries import infra
from libraries import templated_requests
from libraries import utils
from libraries.variables import variables


BGP_PEERS_COUNT = 20
ODL_IP = variables.ODL_IP
RESTCONF_PORT = variables.RESTCONF_PORT
ODL_BGP_PORT = variables.ODL_BGP_PORT
TOOLS_IP = variables.TOOLS_IP
BGP_TOOL_PORT = variables.BGP_TOOL_PORT
BGP_VARIABLES_FOLDER = "variables/bgpuser/"
HOLDTIME = 180
BGP_PEER_LOG_LEVEL = "debug"
BGP_APP_PEER_LOG_LEVEL = "debug"
BGP_PEER_COMMAND = f"python3 tools/fastbgp/play.py --multiplicity={BGP_PEERS_COUNT} --amount 0 --myip={TOOLS_IP} --myport={BGP_TOOL_PORT} --peerip={ODL_IP} --peerport={ODL_BGP_PORT} --{BGP_PEER_LOG_LEVEL} >bgp_peer.log 2>&1"
BGP_PEER_OPTIONS = ""
BGP_APP_PEER_ID = ODL_IP
BGP_APP_PEER_POST_COMMAND = f"python3 tools/fastbgp/bgp_app_peer.py --host {ODL_IP} --port {RESTCONF_PORT} --command post --count 3 --prefix 8.0.1.0 --prefixlen 28 --xml tools/fastbgp/ipv4-routes-template.xml --{BGP_APP_PEER_LOG_LEVEL}"
BGP_APP_PEER_PUT_COMMAND = f"python3 tools/fastbgp/bgp_app_peer.py --host {ODL_IP} --port {RESTCONF_PORT} --command put --count 3 --prefix 8.0.1.0 --prefixlen 28 --xml tools/fastbgp/ipv4-routes-template.xml --{BGP_APP_PEER_LOG_LEVEL}"
BGP_APP_PEER_DELETE_COMMAND = f"python3 tools/fastbgp/bgp_app_peer.py --host {ODL_IP} --port {RESTCONF_PORT} --command delete --count 3 --prefix 8.0.1.0 --prefixlen 28 --xml tools/fastbgp/ipv4-routes-template.xml --{BGP_APP_PEER_LOG_LEVEL}"
BGP_APP_PEER_DELETE_ALL_COMMAND = f"python3 tools/fastbgp/bgp_app_peer.py --host {ODL_IP} --port {RESTCONF_PORT} --command delete-all --xml tools/fastbgp/ipv4-routes-template.xml --{BGP_APP_PEER_LOG_LEVEL}"
BGP_APP_PEER_GET_COMMAND = f"python3 tools/fastbgp/bgp_app_peer.py --host {ODL_IP} --port {RESTCONF_PORT} --command get --xml tools/fastbgp/ipv4-routes-template.xml --{BGP_APP_PEER_LOG_LEVEL}"
BGP_APP_PEER_OPTIONS = "2>&1 >/dev/null"
BGP_APP_PEER_TIMEOUT = 30
BGP_PEER_APP_NAME = "example-bgp-peer-app"
RIB_INSTANCE = "example-bgp-rib"
PROTOCOL_OPENCONFIG = RIB_INSTANCE
DEVICE_NAME = "controller-config"
BGP_PEER_NAME = "example-bgp-peer"
RIB_INSTANCE = "example-bgp-rib"
SCRIPT_URI_OPT = f"--uri data/bgp-rib:application-rib={ODL_IP}/tables=bgp-types%3Aipv4-address-family,bgp-types%3Aunicast-subsequent-address-family"

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=43)
class TestBgpAppPeerBasic:
    bgp_peer_process = None

    def compare_topology(self, tempalate_path):
        """Get current example-ipv4-topology as json, and compare it to expected result."""
        templated_requests.get_templated_request(tempalate_path, None, verify=True)

    def wait_for_topology_to_change_to(self, template_path, retry_count=10, interval=1):
        """Wait until Compare_Topology matches expected result."""
        utils.wait_until_function_pass(
            retry_count, interval, self.compare_topology, template_path
        )

    def teardown_everything(self):
        """Make sure Python tool was killed."""
        infra.search_and_kill_process("play\.py")
        infra.search_and_kill_process("bgp_app_peer\.py")

    def test_bgp_app_peer_basic(self, allure_step_with_separate_logging):

        with allure_step_with_separate_logging(
            "step_reconfigure_odl_to_accept_bgp_peer_connection"
        ):
            """Configure BGP peer module with initiate-connection set to false."""
            for i in range(BGP_PEERS_COUNT):
                mapping = {
                    "IP": f"127.0.1.{i}",
                    "BGP_RIB_OPENCONFIG": PROTOCOL_OPENCONFIG,
                    "HOLDTIME": HOLDTIME,
                    "PEER_PORT": BGP_TOOL_PORT,
                    "PASSIVE_MODE": "true",
                }
                templated_requests.put_templated_request(
                    f"{BGP_VARIABLES_FOLDER}/bgp_peer", mapping, json=False
                )

        with allure_step_with_separate_logging(
            "step_reconfigure_odl_to_accept_bgp_application_peer"
        ):
            """Configure BGP application peer module."""
            mapping = {"IP": BGP_APP_PEER_ID, "BGP_RIB_OPENCONFIG": PROTOCOL_OPENCONFIG}
            templated_requests.put_templated_request(
                f"{BGP_VARIABLES_FOLDER}/bgp_application_peer", mapping, json=False
            )

        with allure_step_with_separate_logging(
            "step_check_for_empty_example_ipv4_topology"
        ):
            """Sanity check example-ipv4-topology is up but empty."""
            self.wait_for_topology_to_change_to(
                "variables/bgpuser/empty_topology", retry_count=180
            )

        with allure_step_with_separate_logging(
            "step_tc1_bgp_application_peer_post_3_initial_routes"
        ):
            """Start BGP application peer tool and give it 30s."""
            infra.shell(
                f"{BGP_APP_PEER_POST_COMMAND} {SCRIPT_URI_OPT} {BGP_APP_PEER_OPTIONS}",
                timeout=BGP_APP_PEER_TIMEOUT,
            )
            infra.backup_file(
                src_dir=".",
                src_file_name="bgp_app_peer.log",
                target_file_name="bgp_app_peer_initial_post_tc1.log",
            )

        with allure_step_with_separate_logging(
            "step_tc1_check_example_ipv4_topology_is_filled_with_3_routes"
        ):
            """See new routes in example-ipv4-topology as a proof that synchronization was correct."""
            self.wait_for_topology_to_change_to("variables/bgpuser/filled_topology")

        with allure_step_with_separate_logging("step_tc1_connect_bgp_peer"):
            """Start BGP peer tool."""
            self.bgp_speaker_process = infra.shell(
                f"{BGP_PEER_COMMAND} {BGP_PEER_OPTIONS}", run_in_background=True
            )
            utils.verify_process_did_not_stop_immediately(self.bgp_speaker_process.pid)

        with allure_step_with_separate_logging(
            "step_tc1_bgp_peer_check_incomming_updates_for_3_introduced_prefixes"
        ):
            """Check incomming updates for new routes."""
            infra.wait_for_string_in_file(
                20, 1, "nlri_prefix_received:", "bgp_peer.log", threshold=3*BGP_PEERS_COUNT
            )
            infra.verify_string_occurence_count_in_file(
                "nlri_prefix_received: 8.0.1.0/28", "bgp_peer.log", BGP_PEERS_COUNT
            )
            infra.verify_string_occurence_count_in_file(
                "nlri_prefix_received: 8.0.1.16/28", "bgp_peer.log", BGP_PEERS_COUNT
            )
            infra.verify_string_occurence_count_in_file(
                "nlri_prefix_received: 8.0.1.32/28", "bgp_peer.log", BGP_PEERS_COUNT
            )
            infra.verify_string_occurence_count_in_file(
                "withdrawn_prefix_received:", "bgp_peer.log", 0
            )

        with allure_step_with_separate_logging(
            "step_tc1_bgp_application_peer_delete_3_initial_routes"
        ):
            """Start BGP application peer tool and give him 30s."""
            infra.shell(
                f"{BGP_APP_PEER_DELETE_COMMAND} {SCRIPT_URI_OPT} {BGP_APP_PEER_OPTIONS}",
                timeout=BGP_APP_PEER_TIMEOUT,
            )
            infra.backup_file(
                src_dir=".",
                src_file_name="bgp_app_peer.log",
                target_file_name="bgp_app_peer_initial_delete_tc1.log",
            )

        with allure_step_with_separate_logging(
            "step_tc1_check_example_ipv4_topology_is_empty"
        ):
            """See new routes are deleted."""
            self.wait_for_topology_to_change_to(
                "variables/bgpuser/empty_topology", 10, 1
            )

        with allure_step_with_separate_logging(
            "step_tc1_peer_check_incomming_updates_for_3_withdrawn_prefixes"
        ):
            """Check incomming updates for new routes."""
            infra.wait_for_string_in_file(
                10, 1, "withdrawn_prefix_received:", "bgp_peer.log", threshold=3*BGP_PEERS_COUNT
            )
            infra.verify_string_occurence_count_in_file(
                "withdrawn_prefix_received: 8.0.1.0/28", "bgp_peer.log", BGP_PEERS_COUNT
            )
            infra.verify_string_occurence_count_in_file(
                "withdrawn_prefix_received: 8.0.1.16/28", "bgp_peer.log", BGP_PEERS_COUNT
            )
            infra.verify_string_occurence_count_in_file(
                "withdrawn_prefix_received: 8.0.1.32/28", "bgp_peer.log", BGP_PEERS_COUNT
            )
            infra.verify_string_occurence_count_in_file(
                "nlri_prefix_received:", "bgp_peer.log", 3 * BGP_PEERS_COUNT
            )

        with allure_step_with_separate_logging("step_tc1_stop_bgp_peer"):
            """Stop BGP peer tool."""
            bgp.stop_bgp_speaker(self.bgp_speaker_process)
            infra.backup_file(
                src_dir=".",
                src_file_name="bgp_peer.log",
                target_file_name="bgp_peer_tc1.log",
            )

        with allure_step_with_separate_logging("step_tc2_reconnect_bgp_peer"):
            """Start BGP peer tool."""
            self.bgp_speaker_process = infra.shell(
                f"{BGP_PEER_COMMAND} {BGP_PEER_OPTIONS}", run_in_background=True
            )
            utils.verify_process_did_not_stop_immediately(self.bgp_speaker_process.pid)
            utils.wait_until_function_pass(
                10,
                1,
                infra.verify_string_occurence_count_in_file,
                "nlri_prefix_received:",
                "bgp_peer.log",
                0,
            )
            infra.verify_string_occurence_count_in_file(
                "withdrawn_prefix_received:", "bgp_peer.log", 0
            )

        with allure_step_with_separate_logging(
            "step_tc2_bgp_application_peer_put_3_routes"
        ):
            """Start BGP application peer tool and give him 30s."""
            bgp.start_bgp_app_peer(
                3,
                command="put",
                prefix="8.0.1.0",
                prefix_len=28,
                uri=SCRIPT_URI_OPT,
                log_level="debug",
                timeout=30,
            )
            infra.shell(
                f"{BGP_APP_PEER_PUT_COMMAND} {SCRIPT_URI_OPT} {BGP_APP_PEER_OPTIONS}",
                timeout=BGP_APP_PEER_TIMEOUT,
            )
            infra.backup_file(
                src_dir=".",
                src_file_name="bgp_app_peer.log",
                target_file_name="bgp_app_peer_put_tc2.log",
            )

        with allure_step_with_separate_logging(
            "step_tc2_check_example_ipv4_topology_is_filled_with_3_routes"
        ):
            """See new routes in example-ipv4-topology as a proof that synchronization was correct."""
            self.wait_for_topology_to_change_to("variables/bgpuser/filled_topology")

        with allure_step_with_separate_logging(
            "step_tc2_bgp_peer_check_incomming_updates_for_3_introduced_prefixes"
        ):
            """Check incomming updates for new routes."""
            infra.wait_for_string_in_file(
                10, 1, "nlri_prefix_received:", "bgp_peer.log", threshold=3*BGP_PEERS_COUNT
            )
            infra.verify_string_occurence_count_in_file(
                "nlri_prefix_received: 8.0.1.0/28", "bgp_peer.log", BGP_PEERS_COUNT
            )
            infra.verify_string_occurence_count_in_file(
                "nlri_prefix_received: 8.0.1.16/28", "bgp_peer.log", BGP_PEERS_COUNT
            )
            infra.verify_string_occurence_count_in_file(
                "nlri_prefix_received: 8.0.1.32/28", "bgp_peer.log", BGP_PEERS_COUNT
            )
            infra.verify_string_occurence_count_in_file(
                "withdrawn_prefix_received:", "bgp_peer.log", 0
            )

        with allure_step_with_separate_logging(
            "step_tc2_bgp_application_peer_delete_all_routes"
        ):
            """Start BGP application peer tool and give him 30s."""
            infra.shell(
                f"{BGP_APP_PEER_DELETE_ALL_COMMAND} {SCRIPT_URI_OPT} {BGP_APP_PEER_OPTIONS}",
                timeout=BGP_APP_PEER_TIMEOUT,
            )
            infra.backup_file(
                src_dir=".",
                src_file_name="bgp_app_peer.log",
                target_file_name="bgp_app_peer_delete_all_tc2.log",
            )

        with allure_step_with_separate_logging(
            "step_tc2_check_example_ipv4_topology_is_empty"
        ):
            """See new routes are deleted."""
            self.wait_for_topology_to_change_to(
                "variables/bgpuser/empty_topology", 10, 1
            )

        with allure_step_with_separate_logging(
            "step_tc2_bgp_peer_check_incomming_updates_for_3_withdrawn_prefixes"
        ):
            """Check incomming updates for new routes."""
            infra.wait_for_string_in_file(
                10, 1, "withdrawn_prefix_received:", "bgp_peer.log", threshold=3*BGP_PEERS_COUNT
            )
            infra.verify_string_occurence_count_in_file(
                "withdrawn_prefix_received: 8.0.1.0/28", "bgp_peer.log", BGP_PEERS_COUNT
            )
            infra.verify_string_occurence_count_in_file(
                "withdrawn_prefix_received: 8.0.1.16/28", "bgp_peer.log", BGP_PEERS_COUNT
            )
            infra.verify_string_occurence_count_in_file(
                "withdrawn_prefix_received: 8.0.1.32/28", "bgp_peer.log", BGP_PEERS_COUNT
            )
            infra.verify_string_occurence_count_in_file(
                "nlri_prefix_received:", "bgp_peer.log", 3*BGP_PEERS_COUNT
            )

        with allure_step_with_separate_logging("step_tc2_stop_bgp_peer"):
            """Stop BGP peer tool."""
            bgp.stop_bgp_speaker(self.bgp_speaker_process)
            infra.backup_file(
                src_dir=".",
                src_file_name="bgp_peer.log",
                target_file_name="bgp_peer_tc2.log",
            )

        with allure_step_with_separate_logging(
            "step_tc3_bgp_application_peer_put_3_routes"
        ):
            """Start BGP application peer tool and give him 30s."""
            infra.shell(
                f"{BGP_APP_PEER_PUT_COMMAND} {SCRIPT_URI_OPT} {BGP_APP_PEER_OPTIONS}",
                timeout=BGP_APP_PEER_TIMEOUT,
            )
            infra.backup_file(
                src_dir=".",
                src_file_name="bgp_app_peer.log",
                target_file_name="bgp_app_peer_put_tc3.log",
            )

        with allure_step_with_separate_logging(
            "step_tc3_check_example_ipv4_topology_is_filled_with_3_routes"
        ):
            """See new routes in example-ipv4-topology as a proof that synchronization was correct."""
            self.wait_for_topology_to_change_to("variables/bgpuser/filled_topology")

        with allure_step_with_separate_logging(
            "step_tc3_reconnect_bgp_peer_and_check_incomming_updates_for_3_introduced_prefixes"
        ):
            """Start BGP peer tool."""
            self.bgp_speaker_process = infra.shell(
                f"{BGP_PEER_COMMAND} {BGP_PEER_OPTIONS}", run_in_background=True
            )

            utils.verify_process_did_not_stop_immediately(self.bgp_speaker_process.pid)
            infra.wait_for_string_in_file(
                20, 1, "nlri_prefix_received:", "bgp_peer.log", threshold=3*BGP_PEERS_COUNT
            )
            infra.verify_string_occurence_count_in_file(
                "nlri_prefix_received:", "bgp_peer.log", 3*BGP_PEERS_COUNT
            )
            infra.verify_string_occurence_count_in_file(
                "nlri_prefix_received: 8.0.1.0/28", "bgp_peer.log", BGP_PEERS_COUNT
            )
            infra.verify_string_occurence_count_in_file(
                "nlri_prefix_received: 8.0.1.16/28", "bgp_peer.log", BGP_PEERS_COUNT
            )
            infra.verify_string_occurence_count_in_file(
                "nlri_prefix_received: 8.0.1.32/28", "bgp_peer.log", BGP_PEERS_COUNT
            )
            infra.verify_string_occurence_count_in_file(
                "withdrawn_prefix_received:", "bgp_peer.log", 0
            )

        with allure_step_with_separate_logging(
            "step_tc3_bgp_application_peer_delete_all_routes"
        ):
            """Start BGP application peer tool and give him 30s."""
            infra.shell(
                f"{BGP_APP_PEER_DELETE_ALL_COMMAND} {SCRIPT_URI_OPT} {BGP_APP_PEER_OPTIONS}",
                timeout=BGP_APP_PEER_TIMEOUT,
            )
            infra.backup_file(
                src_dir=".",
                src_file_name="bgp_app_peer.log",
                target_file_name="bgp_app_peer_delete_all_tc3.log",
            )

        with allure_step_with_separate_logging(
            "step_tc3_check_example_ipv4_topology_is_empty"
        ):
            """See new routes are deleted."""
            self.wait_for_topology_to_change_to(
                "variables/bgpuser/empty_topology", 10, 1
            )

        with allure_step_with_separate_logging(
            "step_tc3_bgp_peer_check_incomming_updates_for_3_withdrawn_prefixes"
        ):
            """Check incomming updates for new routes."""
            infra.wait_for_string_in_file(
                10, 1, "withdrawn_prefix_received:", "bgp_peer.log", threshold=3*BGP_PEERS_COUNT
            )
            infra.verify_string_occurence_count_in_file(
                "withdrawn_prefix_received: 8.0.1.0/28", "bgp_peer.log", BGP_PEERS_COUNT
            )
            infra.verify_string_occurence_count_in_file(
                "withdrawn_prefix_received: 8.0.1.16/28", "bgp_peer.log", BGP_PEERS_COUNT
            )
            infra.verify_string_occurence_count_in_file(
                "withdrawn_prefix_received: 8.0.1.32/28", "bgp_peer.log", BGP_PEERS_COUNT
            )
            infra.verify_string_occurence_count_in_file(
                "nlri_prefix_received:", "bgp_peer.log", 3*BGP_PEERS_COUNT
            )

        with allure_step_with_separate_logging("step_tc3_stop_bgp_peer"):
            """Stop BGP peer tool."""
            bgp.stop_bgp_speaker(self.bgp_speaker_process)
            infra.backup_file(
                src_dir=".",
                src_file_name="bgp_peer.log",
                target_file_name="bgp_peer_tc3.log",
            )

        with allure_step_with_separate_logging("step_delete_bgp_peer_configuration"):
            """Revert the BGP configuration to the original state: without any configured peers."""
            mapping = {"IP": TOOLS_IP, "BGP_RIB_OPENCONFIG": PROTOCOL_OPENCONFIG}
            templated_requests.delete_templated_request(
                f"{BGP_VARIABLES_FOLDER}/bgp_peer", mapping
            )

        with allure_step_with_separate_logging(
            "step_delete_bgp_application_peer_configuration"
        ):
            """Revert the BGP configuration to the original state: without any configured peers."""
            mapping = {"IP": ODL_IP, "BGP_RIB_OPENCONFIG": PROTOCOL_OPENCONFIG}
            templated_requests.delete_templated_request(
                f"{BGP_VARIABLES_FOLDER}/bgp_application_peer", mapping
            )

