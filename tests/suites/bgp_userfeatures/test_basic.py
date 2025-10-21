#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# Basic tests for odl-bgpcep-bgp-all feature.
#
# Test suite performs basic BGP functional test cases:
# BGP peer initiated connection
# - introduce and check 3 prefixes in one update message
# ODL controller initiated connection:
# - introduce and check 3 prefixes in one update message
# - introduce 2 prefixes in first update message and then additional
#   2 prefixes in another update while the very first prefix
#   is withdrawn
# - introduce 3 prefixes and try to withdraw the first one
#   (to be ignored by controller) in a single update message
#
# TC_R (test case reset) tests session-reset functionality.
# Resets the session, and than verifies that example-ipv4-topology is empty
# again.
#
# TC_LA (test case local address) tests configuration of internal peer with
# local-address configured
# - configure peer with local-address and connect bgp-speaker to it
#   with tools_system_ip
# - check filled topology
#
# TC_PG (test case peer group) tests configuration and reconfiguration of
# peer-groups and neighbors configured by them.
# - configure peer-group, and assign neighbor to this peer-group
# - check filled topology
# - reconfigure peer-group without ipv4 unicast afi-safi
# - check empty topology
# - reconfigre neighbor without peer-group, delete peer-group
#
# Brief description how to perform BGP functional test:
# https://wiki.opendaylight.org/view/BGP_LS_PCEP:Lithium_Feature_Tests#How_to_test_2

import json
import logging
import os
import pytest
import time

from libraries import bgp
from libraries import infra
from libraries import templated_requests
from libraries import utils
from libraries.variables import variables


ODL_IP = variables.ODL_IP
TOOLS_IP = variables.TOOLS_IP
BGP_DATA_FOLDER = "variables/bgpuser/"
ODL_LOG_LEVEL = "INFO"
ODL_BGP_LOG_LEVEL = "DEFAULT"
BGP_TOOL_LOG_LEVEL = "info"
PEER_GROUP = "internal-neighbors"

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=15)
class TestBasic:
    bgp_speaker_process = None

    def configure_peer_group(self, peer_group_folder: str):
        """Configures peer group which is template for all the neighbors which are going
        to be configured. Also after PUT, this case verifies presence of peer group within
        peer-groups."""
        mapping = {
            "PEER_GROUP_NAME": PEER_GROUP,
            "HOLDTIME": 180,
            "BGP_RIB_OPENCONFIG": "example-bgp-rib",
            "RR_CLIENT": "false",
            "PASSIVE_MODE": "true",
        }
        templated_requests.put_templated_request(
            f"{BGP_DATA_FOLDER}/{peer_group_folder}", mapping, json=False
        )
        templated_requests.get_templated_request(
            f"{BGP_DATA_FOLDER}/verify_{peer_group_folder}.sulfur", mapping, verify=True
        )

    def deconfigure_peer_group(self):
        """Deconfigures peer group which is template for all the neighbors"""
        mapping = {
            "PEER_GROUP_NAME": PEER_GROUP,
            "BGP_RIB_OPENCONFIG": "example-bgp-rib",
        }
        templated_requests.delete_templated_request(
            f"{BGP_DATA_FOLDER}/peer_group", mapping, json=False
        )

    def restart_talking_bgp_speaker(self):
        """Abort the Python speaker. Also, attempt to stop failing fast. And Start it again.
        We have to restart it this way because we reset session before"""
        bgp.stop_bgp_speaker(self.bgp_speaker_process)
        self.bgp_speaker_process = bgp.start_bgp_speaker(
            ammount=3, my_ip=TOOLS_IP, peer_ip=ODL_IP, log_level=BGP_TOOL_LOG_LEVEL
        )
        utils.verify_process_did_not_stop_immediately(self.bgp_speaker_process.pid)

    def verify_number_of_speaker_connections(self, how_many):
        """Run ss command parse it for number of established connections."""
        count = infra.count_port_occurences(17900, "ESTAB", "python")
        assert (
            count == how_many
        ), f"Number of found occurences of bgp speaker process port {count} does not match expected number {how_many}"

    def check_speaker_is_connected(self):
        """Give it several tries to see exactly one established connection."""
        utils.wait_until_function_pass(
            5, 1, self.verify_number_of_speaker_connections, 1
        )

    def check_speaker_is_not_connected(self):
        """Give it a few tries to see zero established connections."""
        utils.wait_until_function_pass(
            5, 1, self.verify_number_of_speaker_connections, 0
        )

    def compare_topology(self, folder_name: str):
        """Get current example-ipv4-topology as json, and compare it to expected result."""
        templated_requests.get_templated_request(
            f"{BGP_DATA_FOLDER}/{folder_name}", dict(), verify=True
        )

    def wait_for_topology_to_change_to(
        self, folder_name: str, retry: int = 10, interval: int = 1
    ):
        """Wait until Compare_Topology matches expected result."""
        utils.wait_until_function_pass(
            retry, interval, self.compare_topology, folder_name
        )

    def verify_that_topology_does_not_change_from(
        self, folder_name: str, retry: int = 10, interval: int = 1
    ):
        """Verify that Compare_Topology keeps passing, it will hold its last result."""
        utils.verify_function_does_not_fail_within_timeout(
            retry, interval, self.compare_topology, folder_name
        )

    def test_basic(self, allure_step_with_separate_logging):

        with allure_step_with_separate_logging("step_test_suite_setup"):
            """Configure karaf logging level."""
            infra.execute_karaf_command(f"log:set {ODL_LOG_LEVEL}")
            infra.execute_karaf_command(
                f"log:set ${ODL_BGP_LOG_LEVEL} org.opendaylight.bgpcep"
            )
            infra.execute_karaf_command(
                f"log:set ${ODL_BGP_LOG_LEVEL} org.opendaylight.protocol"
            )

        with allure_step_with_separate_logging(
            "step_check_for_empty_topology_before_talking"
        ):
            """Sanity check example-ipv4-topology is up but empty."""
            self.wait_for_topology_to_change_to("empty_topology", retry=180)

        with allure_step_with_separate_logging(
            "step_reconfigure_odl_to_initiate_connection"
        ):
            """Configure ibgp peer with local-address."""
            mapping = {
                "IP": TOOLS_IP,
                "PEER_PORT": 17900,
                "LOCAL": ODL_IP,
                "HOLDTIME": 180,
                "BGP_RIB_OPENCONFIG": "example-bgp-rib",
                "PASSIVE_MODE": "false",
            }
            templated_requests.put_templated_request(
                f"{BGP_DATA_FOLDER}/ibgp_local_address", mapping, json=False
            )

        with allure_step_with_separate_logging(
            "step_tc_la_start_bgp_speaker_and_verify_connected"
        ):
            """Verify that peer is present in odl's rib under local-address ip."""
            self.bgp_speaker_process = bgp.start_bgp_speaker_and_verify_connected(
                speaker_ip=TOOLS_IP,
                ammount=3,
                listen=True,
                my_ip=TOOLS_IP,
                peer_ip=ODL_IP,
                log_level=BGP_TOOL_LOG_LEVEL,
            )

        with allure_step_with_separate_logging("step_tc_la_kill_bgp_speaker"):
            """Abort the Python speaker."""
            bgp.stop_bgp_speaker(self.bgp_speaker_process)

        with allure_step_with_separate_logging(
            "step_tc_la_delete_bgp_peer_configuration"
        ):
            """Delete peer configuration."""
            mapping = {"IP": TOOLS_IP, "BGP_RIB_OPENCONFIG": "example-bgp-rib"}
            templated_requests.delete_templated_request(
                f"{BGP_DATA_FOLDER}/ibgp_local_address", mapping
            )

        with allure_step_with_separate_logging(
            "step_reconfigure_odl_to_accept_connection"
        ):
            """Configure BGP peer module with initiate-connection set to false."""
            mapping = {
                "IP": TOOLS_IP,
                "PEER_PORT": 17900,
                "HOLDTIME": 180,
                "BGP_RIB_OPENCONFIG": "example-bgp-rib",
                "PASSIVE_MODE": "true",
            }
            templated_requests.put_templated_request(
                f"{BGP_DATA_FOLDER}/bgp_peer", mapping, json=False
            )

        with allure_step_with_separate_logging("step_start_talking_bgp_speaker"):
            """Start Python speaker to connect to ODL, verify that the tool does not promptly exit."""
            self.bgp_speaker_process = bgp.start_bgp_speaker(
                ammount=3, my_ip=TOOLS_IP, peer_ip=ODL_IP, log_level=BGP_TOOL_LOG_LEVEL
            )
            utils.verify_process_did_not_stop_immediately(self.bgp_speaker_process.pid)

        with allure_step_with_separate_logging(
            "step_check_talking_connection_is_established"
        ):
            """See TCP (BGP) connection in established state.
            This case is separate from the previous one, to resemble structure of the second half of this suite more closely.
            """
            self.check_speaker_is_connected()

        with allure_step_with_separate_logging("step_check_talking_topology_is_filled"):
            """See new routes in example-ipv4-topology as a proof that synchronization was correct."""
            self.wait_for_topology_to_change_to("filled_topology")

        with allure_step_with_separate_logging("step_tc_r_reset_bgp_peer_session"):
            """Reset Peer Session."""
            mapping = {"IP": TOOLS_IP, "RIB_INSTANCE_NAME": "example-bgp-rib"}
            templated_requests.post_templated_request(
                f"{BGP_DATA_FOLDER}/peer_session/restart", mapping, json=False
            )

        with allure_step_with_separate_logging(
            "step_tc_r_check_for_empty_topology_after_resetting"
        ):
            """See example-ipv4-topology empty after resetting session."""
            self.wait_for_topology_to_change_to("empty_topology")

        with allure_step_with_separate_logging(
            "step_tc_pg_reconfigure_odl_with_peer_group_to_accept_connection"
        ):
            """Configure BGP peer module with initiate-connection set to false."""
            mapping = {
                "IP": TOOLS_IP,
                "PEER_GROUP_NAME": PEER_GROUP,
                "BGP_RIB_OPENCONFIG": "example-bgp-rib",
            }
            templated_requests.delete_templated_request(
                f"{BGP_DATA_FOLDER}/bgp_peer", mapping
            )
            self.configure_peer_group("peer_group")
            mapping = {
                "IP": TOOLS_IP,
                "PEER_GROUP_NAME": PEER_GROUP,
                "BGP_RIB_OPENCONFIG": "example-bgp-rib",
            }
            templated_requests.put_templated_request(
                f"{BGP_DATA_FOLDER}/bgp_peer_group", mapping, json=False
            )

        with allure_step_with_separate_logging(
            "step_tc_pg_restart_talking_bgp_speaker"
        ):
            """Abort the Python speaker."""
            self.restart_talking_bgp_speaker()

        with allure_step_with_separate_logging("step_check_talking_topology_is_filled"):
            """See new routes in example-ipv4-topology as a proof that synchronization was correct."""
            self.wait_for_topology_to_change_to("filled_topology")

        with allure_step_with_separate_logging(
            "step_tc_pg_reconfigure_odl_with_peer_group_without_ipv4_unicast"
        ):
            """Configure BGP peer module with initiate-connection set to false."""
            self.configure_peer_group(peer_group_folder="peer_group_without_ipv4")

        with allure_step_with_separate_logging(
            "step_tc_pg_check_for_empty_topology_after_deconfiguration"
        ):
            """Sanity check example-ipv4-topology is up but empty."""
            self.wait_for_topology_to_change_to("empty_topology")

        with allure_step_with_separate_logging(
            "step_tc_pg_reconfigure_odl_to_accept_connection"
        ):
            """Configure BGP peer module with initiate-connection set to false."""
            mapping = {
                "IP": TOOLS_IP,
                "PEER_PORT": 17900,
                "PEER_GROUP_NAME": PEER_GROUP,
                "BGP_RIB_OPENCONFIG": "example-bgp-rib",
                "HOLDTIME": 180,
                "PASSIVE_MODE": "true",
            }
            templated_requests.delete_templated_request(
                f"{BGP_DATA_FOLDER}/bgp_peer_group", mapping
            )
            templated_requests.put_templated_request(
                f"{BGP_DATA_FOLDER}/bgp_peer", mapping, json=False
            )

        with allure_step_with_separate_logging(
            "step_tc_pg_reconfigure_odl_to_accept_connection"
        ):
            """Configure BGP peer module with initiate-connection set to false."""
            mapping = {
                "IP": TOOLS_IP,
                "PEER_PORT": 17900,
                "PEER_GROUP_NAME": PEER_GROUP,
                "BGP_RIB_OPENCONFIG": "example-bgp-rib",
                "HOLDTIME": 180,
                "PASSIVE_MODE": "true",
            }
            templated_requests.delete_templated_request(
                f"{BGP_DATA_FOLDER}/bgp_peer_group", mapping
            )
            templated_requests.put_templated_request(
                f"{BGP_DATA_FOLDER}/bgp_peer", mapping, json=False
            )

        with allure_step_with_separate_logging("step_kill_talking_bgp_speaker"):
            """Abort the Python speaker."""
            bgp.stop_bgp_speaker(self.bgp_speaker_process)

        with allure_step_with_separate_logging(
            "step_check_for_empty_topology_after_talking"
        ):
            """See example-ipv4-topology empty again."""
            self.wait_for_topology_to_change_to("empty_topology")

        with allure_step_with_separate_logging("step_start_listening_bgp_speaker"):
            """Start Python speaker in listening mode, verify that the tool does not exit quickly."""
            self.bgp_speaker_process = bgp.start_bgp_speaker(
                ammount=3,
                listen=True,
                my_ip=TOOLS_IP,
                peer_ip=ODL_IP,
                log_level=BGP_TOOL_LOG_LEVEL,
            )
            utils.verify_process_did_not_stop_immediately(self.bgp_speaker_process.pid)

        with allure_step_with_separate_logging(
            "step_check_listening_connection_is_not_established_yet"
        ):
            """See no TCP connection, as both ODL and tool are in listening mode."""
            self.check_speaker_is_not_connected()

        with allure_step_with_separate_logging(
            "step_check_for_empty_topology_before_listening"
        ):
            """Sanity check example-ipv4-topology is still empty."""
            self.verify_that_topology_does_not_change_from("empty_topology")

        with allure_step_with_separate_logging(
            "step_reconfigure_odl_to_initiate_connection"
        ):
            """Replace BGP peer config module, now with initiate-connection set to true.."""
            mapping = {
                "IP": TOOLS_IP,
                "PEER_PORT": 17900,
                "HOLDTIME": 180,
                "BGP_RIB_OPENCONFIG": "example-bgp-rib",
                "PASSIVE_MODE": "false",
            }
            templated_requests.put_templated_request(
                f"{BGP_DATA_FOLDER}/bgp_peer", mapping, json=False
            )

        with allure_step_with_separate_logging(
            "step_check_listening_connection_is_established"
        ):
            """See TCP (BGP) connection in established state."""
            self.check_speaker_is_connected()

        with allure_step_with_separate_logging(
            "step_check_listening_topology_is_filled"
        ):
            """See new routes in example-ipv4-topology as a proof that synchronization was correct."""
            self.wait_for_topology_to_change_to("filled_topology")

        with allure_step_with_separate_logging("step_kill_listening_bgp_speaker"):
            """Abort the Python speaker."""
            bgp.stop_bgp_speaker(self.bgp_speaker_process)

        with allure_step_with_separate_logging(
            "step_check_for_empty_topology_after_listening"
        ):
            """Post-condition: Check example-ipv4-topology is empty again."""
            self.wait_for_topology_to_change_to("empty_topology")

        with allure_step_with_separate_logging(
            "step_start_listening_bgp_speaker_case_2"
        ):
            """BGP Speaker introduces 2 prefixes in the first update & another 2 prefixes while the very first is withdrawn in 2nd update"""
            self.bgp_speaker_process = bgp.start_bgp_speaker(
                ammount=3,
                listen=True,
                my_ip=TOOLS_IP,
                peer_ip=ODL_IP,
                prefill=2,
                insert=2,
                withdraw=1,
                updates="single",
                firstprefix="8.0.0.240",
                log_level=BGP_TOOL_LOG_LEVEL,
            )
            utils.verify_process_did_not_stop_immediately(self.bgp_speaker_process.pid)

        with allure_step_with_separate_logging(
            "step_check_listening_connection_is_established_case_2"
        ):
            """See TCP (BGP) connection in established state."""
            self.check_speaker_is_connected()

        with allure_step_with_separate_logging(
            "step_check_listening_topology_is_filled_case_2"
        ):
            """See TCP (BGP) connection in established state."""
            self.wait_for_topology_to_change_to("filled_topology")

        with allure_step_with_separate_logging(
            "step_kill_listening_bgp_speaker_case_2"
        ):
            """Abort the Python speaker."""
            bgp.stop_bgp_speaker(self.bgp_speaker_process)

        with allure_step_with_separate_logging(
            "step_check_for_empty_topology_after_listening_case_2"
        ):
            """Post-condition: Check example-ipv4-topology is empty again."""
            self.wait_for_topology_to_change_to("empty_topology")

        with allure_step_with_separate_logging(
            "step_start_listening_bgp_speaker_case_3"
        ):
            """BGP Speaker introduces 3 prefixes while the first one occures again in the withdrawn list (to be ignored by controller)."""
            self.bgp_speaker_process = bgp.start_bgp_speaker(
                ammount=2,
                listen=True,
                my_ip=TOOLS_IP,
                peer_ip=ODL_IP,
                prefill=0,
                insert=3,
                withdraw=1,
                updates="single",
                log_level=BGP_TOOL_LOG_LEVEL,
            )
            utils.verify_process_did_not_stop_immediately(self.bgp_speaker_process.pid)

        with allure_step_with_separate_logging(
            "step_check_listening_connection_is_established_case_3"
        ):
            """See TCP (BGP) connection in established state."""
            self.check_speaker_is_connected()

        with allure_step_with_separate_logging(
            "step_check_listening_topology_is_filled_case_3"
        ):
            """See new routes in example-ipv4-topology as a proof that synchronization was correct."""
            self.wait_for_topology_to_change_to("filled_topology")

        with allure_step_with_separate_logging(
            "step_kill_listening_bgp_speaker_case_3"
        ):
            """Abort the Python speaker."""
            bgp.stop_bgp_speaker(self.bgp_speaker_process)

        with allure_step_with_separate_logging(
            "step_check_for_empty_topology_after_listening_case_3"
        ):
            """Post-condition: Check example-ipv4-topology is empty again."""
            self.wait_for_topology_to_change_to("empty_topology")

        with allure_step_with_separate_logging("step_delete_bgp_peer_configuration"):
            """Revert the BGP configuration to the original state: without any configured peers."""
            mapping = {
                "IP": TOOLS_IP,
                "PEER_GROUP_NAME": PEER_GROUP,
                "BGP_RIB_OPENCONFIG": "example-bgp-rib",
            }
            templated_requests.delete_templated_request(
                f"{BGP_DATA_FOLDER}/bgp_peer", mapping
            )
