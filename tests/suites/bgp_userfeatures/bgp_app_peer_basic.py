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

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=15)
class TestBgpAppPeerBasic:
    bgp_peer_process = None

    def verify_topology_is_as_expected(self, tempalate_path):
        templated_requests.get_templated_request(tempalate_path, None, verify=True)

    def wait_until_expected_topology(self, template_path, retry_count=20, interval=1):
        utils.wait_until_function_pass(retry_count, interval, self.verify_topology_is_as_expected, template_path)

    def test_bgp_app_peer_basic(self, allure_step_with_separate_logging):

        with allure_step_with_separate_logging("step_reconfigure_odl_to_accept_bgp_peer_connection"):
            """Configure BGP peer module with initiate-connection set to false."""
            bgp.set_bgp_neighbour(ip=TOOLS_IP, passive_mode=True)

        with allure_step_with_separate_logging("step_reconfigure_odl_to_accept_bgp_application_peer"):
            """Configure BGP application peer module."""
            bgp.set_bgp_application_peer(ip=ODL_IP)

        with allure_step_with_separate_logging("step_check_for_empty_example_ipv4_topology"):
            """Sanity check example-ipv4-topology is up but empty."""
            self.wait_until_expected_topology("variables/bgpuser/empty_topology")

        with allure_step_with_separate_logging("step_tc1_bgp_application_peer_post_3_initial_routes"):
            """Start BGP application peer tool and give it 30s."""
            bgp.start_bgp_app_peer(3, command="post", prefix="8.0.1.0", prefix_len=28, rib_ip=ODL_IP, log_level="debug", timeout=30)
            infra.shell("cp bgp_app_peer.log results/bgp_app_peer_initial_post_tc1.log")

        with allure_step_with_separate_logging("step_tc1_check_example_ipv4_topology_is_filled_with_3_routes"):
            """See new routes in example-ipv4-topology as a proof that synchronization was correct."""
            self.wait_until_expected_topology("variables/bgpuser/filled_topology")

        with allure_step_with_separate_logging("step_tc1_connect_bgp_peer"):
            """Start BGP peer tool."""
            self.bgp_speaker_process = bgp.start_bgp_speaker(0, my_ip=TOOLS_IP, log_level="debug")
            utils.verify_process_did_not_stop_immediately(self.bgp_speaker_process.pid)

        with allure_step_with_separate_logging("step_tc1_bgp_peer_check_incomming_updates_for_3_introduced_prefixes"):
            """Check incomming updates for new routes."""
            infra.wait_for_string_in_file(10, 1, "nlri_prefix_received:", "bgp_peer.log", threshold=3)
            infra.verify_string_occurence_count_in_file("nlri_prefix_received: 8.0.1.0/28", "bgp_peer.log", 1)
            infra.verify_string_occurence_count_in_file("nlri_prefix_received: 8.0.1.16/28", "bgp_peer.log", 1)
            infra.verify_string_occurence_count_in_file("nlri_prefix_received: 8.0.1.32/28", "bgp_peer.log", 1)
            infra.verify_string_occurence_count_in_file("withdrawn_prefix_received:", "bgp_peer.log", 0)

        with allure_step_with_separate_logging("step_tc1_bgp_application_peer_delete_3_initial_routes"):
            """Start BGP application peer tool and give him 30s."""
            bgp.start_bgp_app_peer(3, command="delete", prefix="8.0.1.0", prefix_len=28, rib_ip=ODL_IP, log_level="debug", timeout=30)
            infra.shell("cp bgp_app_peer.log results/bgp_app_peer_initial_delete_tc1.log")

        with allure_step_with_separate_logging("step_tc1_check_example_ipv4_topology_is_empty"):
            """See new routes are deleted."""
            self.wait_until_expected_topology("variables/bgpuser/empty_topology", 10, 1)

        with allure_step_with_separate_logging("step_tc1_peer_check_incomming_updates_for_3_withdrawn_prefixes"):
            """Check incomming updates for new routes."""
            infra.wait_for_string_in_file(10, 1, "withdrawn_prefix_received:", "bgp_peer.log", threshold=3)
            infra.verify_string_occurence_count_in_file("withdrawn_prefix_received: 8.0.1.0/28", "bgp_peer.log", 1)
            infra.verify_string_occurence_count_in_file("withdrawn_prefix_received: 8.0.1.16/28", "bgp_peer.log", 1)
            infra.verify_string_occurence_count_in_file("withdrawn_prefix_received: 8.0.1.32/28", "bgp_peer.log", 1)
            infra.verify_string_occurence_count_in_file("nlri_prefix_received:", "bgp_peer.log", 3)

        with allure_step_with_separate_logging("step_tc1_stop_bgp_peer"):
            """Stop BGP peer tool."""
            bgp.stop_bgp_speaker(self.bgp_speaker_process)
            infra.shell("cp  bgp_peer.log results/bgp_peer_tc1.log")

        with allure_step_with_separate_logging("step_tc2_reconnect_bgp_peer"):
            """Start BGP peer tool."""
            self.bgp_speaker_process = bgp.start_bgp_speaker(0, my_ip=TOOLS_IP, log_level="debug")
            utils.verify_process_did_not_stop_immediately(self.bgp_speaker_process.pid)
            utils.wait_until_function_pass(10, 1, infra.verify_string_occurence_count_in_file, "nlri_prefix_received:", "bgp_peer.log", 0)
            infra.verify_string_occurence_count_in_file("withdrawn_prefix_received:", "bgp_peer.log", 0)

        with allure_step_with_separate_logging("step_tc2_bgp_application_peer_put_3_routes"):
            """Start BGP application peer tool and give him 30s."""
            bgp.start_bgp_app_peer(3, command="put", prefix="8.0.1.0", prefix_len=28, rib_ip=ODL_IP, log_level="debug", timeout=30)
            infra.shell("cp bgp_app_peer.log results/bgp_app_peer_put_tc2.log")

        with allure_step_with_separate_logging("step_tc2_check_example_ipv4_topology_is_filled_with_3_routes"):
            """See new routes in example-ipv4-topology as a proof that synchronization was correct."""
            self.wait_until_expected_topology("variables/bgpuser/filled_topology")

        with allure_step_with_separate_logging("step_tc2_bgp_peer_check_incomming_updates_for_3_introduced_prefixes"):
            """Check incomming updates for new routes."""
            infra.wait_for_string_in_file(10, 1, "nlri_prefix_received:", "bgp_peer.log", threshold=3)
            infra.verify_string_occurence_count_in_file("nlri_prefix_received: 8.0.1.0/28", "bgp_peer.log", 1)
            infra.verify_string_occurence_count_in_file("nlri_prefix_received: 8.0.1.16/28", "bgp_peer.log", 1)
            infra.verify_string_occurence_count_in_file("nlri_prefix_received: 8.0.1.32/28", "bgp_peer.log", 1)
            infra.verify_string_occurence_count_in_file("withdrawn_prefix_received:", "bgp_peer.log", 0)

        with allure_step_with_separate_logging("step_tc2_bgp_application_peer_delete_all_routes"):
            """Start BGP application peer tool and give him 30s."""
            bgp.start_bgp_app_peer(0, command="delete-all", rib_ip=ODL_IP, log_level="debug", timeout=30)
            infra.shell("cp bgp_app_peer.log results/bgp_app_peer_delete_all_tc2.log")

        with allure_step_with_separate_logging("step_tc2_check_example_ipv4_topology_is_empty"):
            """See new routes are deleted."""
            self.wait_until_expected_topology("variables/bgpuser/empty_topology", 10, 1)

        with allure_step_with_separate_logging("step_tc2_bgp_peer_check_incomming_updates_for_3_withdrawn_prefixes"):
            """Check incomming updates for new routes."""
            infra.wait_for_string_in_file(10, 1, "withdrawn_prefix_received:", "bgp_peer.log", threshold=3)
            infra.verify_string_occurence_count_in_file("withdrawn_prefix_received: 8.0.1.0/28", "bgp_peer.log", 1)
            infra.verify_string_occurence_count_in_file("withdrawn_prefix_received: 8.0.1.16/28", "bgp_peer.log", 1)
            infra.verify_string_occurence_count_in_file("withdrawn_prefix_received: 8.0.1.32/28", "bgp_peer.log", 1)
            infra.verify_string_occurence_count_in_file("nlri_prefix_received:", "bgp_peer.log", 3)

        with allure_step_with_separate_logging("step_tc2_stop_bgp_peer"):
            """Stop BGP peer tool."""
            bgp.stop_bgp_speaker(self.bgp_speaker_process)
            infra.shell("cp  bgp_peer.log results/bgp_peer_tc2.log")

        with allure_step_with_separate_logging("step_tc3_bgp_application_peer_put_3_routes"):
            """Start BGP application peer tool and give him 30s."""
            bgp.start_bgp_app_peer(3, command="put", prefix="8.0.1.0", prefix_len=28, rib_ip=ODL_IP, log_level="debug", timeout=30)
            infra.shell("cp bgp_app_peer.log results/bgp_app_peer_put_tc3.log")

        with allure_step_with_separate_logging("step_tc3_check_example_ipv4_topology_is_filled_with_3_routes"):
            """See new routes in example-ipv4-topology as a proof that synchronization was correct."""
            self.wait_until_expected_topology("variables/bgpuser/filled_topology")

        with allure_step_with_separate_logging("step_tc3_reconnect_bgp_peer_and_check_incomming_updates_for_3_introduced_prefixes"):
            """Start BGP peer tool."""
            self.bgp_speaker_process = bgp.start_bgp_speaker(0, my_ip=TOOLS_IP, log_level="debug")
            
            utils.verify_process_did_not_stop_immediately(self.bgp_speaker_process.pid)
            infra.wait_for_string_in_file(20, 1, "nlri_prefix_received:", "bgp_peer.log", threshold=3)
            infra.verify_string_occurence_count_in_file("nlri_prefix_received:", "bgp_peer.log", 3)
            infra.verify_string_occurence_count_in_file("nlri_prefix_received: 8.0.1.0/28", "bgp_peer.log", 1)
            infra.verify_string_occurence_count_in_file("nlri_prefix_received: 8.0.1.16/28", "bgp_peer.log", 1)
            infra.verify_string_occurence_count_in_file("nlri_prefix_received: 8.0.1.32/28", "bgp_peer.log", 1)
            infra.verify_string_occurence_count_in_file("withdrawn_prefix_received:", "bgp_peer.log", 0)

        with allure_step_with_separate_logging("step_tc3_bgp_application_peer_delete_all_routes"):
            """Start BGP application peer tool and give him 30s."""
            bgp.start_bgp_app_peer(0, command="delete-all", rib_ip=ODL_IP, log_level="debug", timeout=30)
            infra.shell("cp bgp_app_peer.log results/bgp_app_peer_delete_all_tc3.log")

        with allure_step_with_separate_logging("step_tc3_check_example_ipv4_topology_is_empty"):
            """See new routes are deleted."""
            self.wait_until_expected_topology("variables/bgpuser/empty_topology", 10, 1)

        with allure_step_with_separate_logging("step_tc3_bgp_peer_check_incomming_updates_for_3_withdrawn_prefixes"):
            """Check incomming updates for new routes."""
            infra.wait_for_string_in_file(10, 1, "withdrawn_prefix_received:", "bgp_peer.log", threshold=3)
            infra.verify_string_occurence_count_in_file("withdrawn_prefix_received: 8.0.1.0/28", "bgp_peer.log", 1)
            infra.verify_string_occurence_count_in_file("withdrawn_prefix_received: 8.0.1.16/28", "bgp_peer.log", 1)
            infra.verify_string_occurence_count_in_file("withdrawn_prefix_received: 8.0.1.32/28", "bgp_peer.log", 1)
            infra.verify_string_occurence_count_in_file("nlri_prefix_received:", "bgp_peer.log", 3)

        with allure_step_with_separate_logging("step_tc3_stop_bgp_peer"):
            """Stop BGP peer tool."""
            bgp.stop_bgp_speaker(self.bgp_speaker_process)
            infra.shell("cp  bgp_peer.log results/bgp_peer_tc3.log")

        with allure_step_with_separate_logging("step_delete_bgp_peer_configuration"):
            """Revert the BGP configuration to the original state: without any configured peers."""
            bgp.delete_bgp_neighbour(ip=TOOLS_IP)

        with allure_step_with_separate_logging("step_delete_bgp_application_peer_configuration"):
            """Revert the BGP configuration to the original state: without any configured peers."""
            bgp.delete_bgp_application_peer(ip=ODL_IP)

