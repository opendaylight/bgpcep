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
# Test suite performs basic iBGP functional test cases for BGP peers in
# different roles (iBGP, iBGP RR-client):
#
# Test Case 1: Two iBGP RR-client peers introduce prefixes Expected result:
# controller forwards updates towards both peers
#
# Test Case 2: Two iBGP peers: one RR client and one non-client introduces
# prefixes Expected result: controller forwards updates towards both peers
#
# Test Case 3: Two iBGP RR non-client peers introduce prefixes
# Expected result: controller does not forward any update towards peers
#
# Test Case 4: Two iBGP(play.py) RR-client peers configured, first of them
# configured with route-reflector-cluster-id, second inherits it's
# cluster-id from global config. Each of them introduces 3 prefixes.
# Expected result: controller forwards updates towards both peers and
# each of their adj-rib-in contains routes. First peer should contain
# default cluster-id and second cluster-id from first peers configuration.
#
# For polices see: https://wiki.opendaylight.org/view/BGP_LS_PCEP:BGP

import logging
import pytest

from libraries import bgp
from libraries import infra
from libraries import templated_requests
from libraries import prefix_counting
from libraries import utils
from libraries.variables import variables


ODL_IP = variables.ODL_IP
TOOLS_IP = variables.TOOLS_IP
BGP_PEER_LOG_LEVEL = "debug"
BGP_DATA_FOLDER = "variables/bgpuser/"
ODL_LOG_LEVEL = "INFO"
ODL_BGP_LOG_LEVEL = "DEFAULT"
BGP_PEER1_IP = "127.0.0.1"
BGP_PEER2_IP = "127.0.0.2"
PREFIX_LEN = 28
BGP_PEER1_FIRST_PREFIX_IP = "8.1.0.0"
BGP_PEER2_FIRST_PREFIX_IP = "8.2.0.0"
PREFIX_COUNT = 3
BGP_PEER1_PREFIX_COUNT = PREFIX_COUNT
BGP_PEER2_PREFIX_COUNT = PREFIX_COUNT
BGP_PEER1_LOG_FILE = "bgp_peer1.log"
BGP_PEER2_LOG_FILE = "bgp_peer2.log"
DEFAULT_LOG_CHECK_TIMEOUT = 20
DEFAULT_LOG_CHECK_PERIOD = 1

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=47)
class TestIbgpPeersBasic:
    bgp_peer1_process = None
    bgp_peer2_process = None

    def verify_topology_is_as_expected(self, tempalate_path):
        templated_requests.get_templated_request(tempalate_path, None, verify=True)

    def wait_until_expected_topology(self, template_path, retry_count=20, interval=1):
        utils.wait_until_function_pass(
            retry_count, interval, self.verify_topology_is_as_expected, template_path
        )

    def configure_ibgp_peer(self, ip: str, rr_client: bool, cluster_id: bool = False):
        template_folder = "cluster_id/ibgp_peer" if cluster_id else "ibgp_peers"
        mapping = {
            "IP": ip,
            "HOLDTIME": 180,
            "PEER_PORT": 17900,
            "BGP_RIB_OPENCONFIG": "example-bgp-rib",
            "RR_CLIENT": str(rr_client).lower(),
            "PASSIVE_MODE": "true",
        }
        templated_requests.put_templated_request(
            f"{BGP_DATA_FOLDER}/{template_folder}", mapping, json=False
        )

    def delete_ibgp_peer_configuration(self, ip: str, cluster_id: bool = False):
        template_folder = "cluster_id/ibgp_peer" if cluster_id else "ibgp_peers"
        mapping = {"IP": ip, "BGP_RIB_OPENCONFIG": "example-bgp-rib"}
        templated_requests.delete_templated_request(
            f"{BGP_DATA_FOLDER}/{template_folder}", mapping
        )

    def connect_bgp_peer1_and_verify(self, cluster_id: str = None):
        self.bgp_peer1_process = bgp.start_bgp_speaker(
            firstprefix=BGP_PEER1_FIRST_PREFIX_IP,
            prefixlen=PREFIX_LEN,
            ammount=3,
            my_ip=BGP_PEER1_IP,
            peer_ip=ODL_IP,
            cluster=cluster_id,
            log_level=BGP_PEER_LOG_LEVEL,
            log_file=BGP_PEER1_LOG_FILE,
        )
        utils.wait_until_function_pass(
            10,
            1,
            prefix_counting.check_example_ipv4_topology_contains,
            f"'prefix': '{BGP_PEER1_FIRST_PREFIX_IP}/{PREFIX_LEN}'",
        )

    def connect_bgp_peer2_and_verify(self, cluster_id: str = None):
        self.bgp_peer2_process = bgp.start_bgp_speaker(
            firstprefix=BGP_PEER2_FIRST_PREFIX_IP,
            prefixlen=PREFIX_LEN,
            ammount=3,
            my_ip=BGP_PEER2_IP,
            peer_ip=ODL_IP,
            cluster=cluster_id,
            log_level=BGP_PEER_LOG_LEVEL,
            log_file=BGP_PEER2_LOG_FILE,
        )
        utils.wait_until_function_pass(
            10,
            1,
            prefix_counting.check_example_ipv4_topology_contains,
            f"'prefix': '{BGP_PEER2_FIRST_PREFIX_IP}/{PREFIX_LEN}'",
        )

    def test_ibgp_peers_basic(self, allure_step_with_separate_logging):

        with allure_step_with_separate_logging("step_test_suite_setup"):
            """Configure karaf logging level"""
            infra.execute_karaf_command(f"log:set {ODL_LOG_LEVEL}")
            infra.execute_karaf_command(
                f"log:set ${ODL_BGP_LOG_LEVEL} org.opendaylight.bgpcep"
            )
            infra.execute_karaf_command(
                f"log:set ${ODL_BGP_LOG_LEVEL} org.opendaylight.protocol"
            )

        with allure_step_with_separate_logging(
            "step_tc1_configure_two_ibgp_route_reflector_client_peers"
        ):
            """Configure two iBGP peers as routing reflector clients."""
            self.configure_ibgp_peer(BGP_PEER1_IP, rr_client=True, cluster_id=False)
            self.configure_ibgp_peer(BGP_PEER2_IP, rr_client=True, cluster_id=False)

        with allure_step_with_separate_logging("step_tc1_connect_bgp_peer1"):
            """Connect BGP peer."""
            self.connect_bgp_peer1_and_verify()

        with allure_step_with_separate_logging("step_tc1_connect_bgp_peer2"):
            """Connect BGP peer."""
            self.connect_bgp_peer2_and_verify()

        with allure_step_with_separate_logging(
            "step_tc1_bgp_peer1_check_log_for_introduced_prefixes"
        ):
            """Check incomming updates for new routes."""
            utils.wait_until_function_pass(
                DEFAULT_LOG_CHECK_TIMEOUT,
                DEFAULT_LOG_CHECK_PERIOD,
                infra.verify_string_occurence_count_in_file,
                "nlri_prefix_received:",
                f"tmp/{BGP_PEER1_LOG_FILE}",
                BGP_PEER2_PREFIX_COUNT,
            )
            infra.verify_string_occurence_count_in_file(
                f"nlri_prefix_received: {BGP_PEER2_FIRST_PREFIX_IP}/{PREFIX_LEN}",
                f"tmp/{BGP_PEER1_LOG_FILE}",
                1,
            )
            infra.verify_string_occurence_count_in_file(
                "withdrawn_prefix_received:", f"tmp/{BGP_PEER1_LOG_FILE}", 0
            )

        with allure_step_with_separate_logging(
            "step_tc1_bgp_peer2_check_log_for_introduced_prefixes"
        ):
            """Check incomming updates for new routes."""
            utils.wait_until_function_pass(
                DEFAULT_LOG_CHECK_TIMEOUT,
                DEFAULT_LOG_CHECK_PERIOD,
                infra.verify_string_occurence_count_in_file,
                "nlri_prefix_received:",
                f"tmp/{BGP_PEER2_LOG_FILE}",
                BGP_PEER1_PREFIX_COUNT,
            )
            infra.verify_string_occurence_count_in_file(
                f"nlri_prefix_received: {BGP_PEER1_FIRST_PREFIX_IP}/{PREFIX_LEN}",
                f"tmp/{BGP_PEER2_LOG_FILE}",
                1,
            )
            infra.verify_string_occurence_count_in_file(
                "withdrawn_prefix_received:", f"tmp/{BGP_PEER2_LOG_FILE}", 0
            )

        with allure_step_with_separate_logging("step_tc1_disconnect_bgp_peer1"):
            """Stop BGP peer & store logs."""
            bgp.stop_bgp_speaker(self.bgp_peer1_process)
            infra.shell(f"tmp/{BGP_PEER1_LOG_FILE} results/tc1_{BGP_PEER1_LOG_FILE}")

        with allure_step_with_separate_logging(
            "step_tc1_bgp_peer2_check_log_for_withdrawn_prefixes"
        ):
            """Check incomming updates for withdrawn routes."""
            utils.wait_until_function_pass(
                DEFAULT_LOG_CHECK_TIMEOUT,
                DEFAULT_LOG_CHECK_PERIOD,
                infra.verify_string_occurence_count_in_file,
                "withdrawn_prefix_received:",
                f"tmp/{BGP_PEER2_LOG_FILE}",
                BGP_PEER1_PREFIX_COUNT,
            )
            infra.verify_string_occurence_count_in_file(
                f"withdrawn_prefix_received: {BGP_PEER1_FIRST_PREFIX_IP}/{PREFIX_LEN}",
                f"tmp/{BGP_PEER2_LOG_FILE}",
                1,
            )

        with allure_step_with_separate_logging("step_tc1_disconnect_bgp_peer2"):
            """Stop BGP peer & store logs."""
            bgp.stop_bgp_speaker(self.bgp_peer2_process)
            infra.shell(f"tmp/{BGP_PEER2_LOG_FILE} results/tc1_{BGP_PEER2_LOG_FILE}")

        with allure_step_with_separate_logging(
            "step_tc1_check_for_empty_ipv4_topology"
        ):
            """Checks for empty topology after"""
            utils.wait_until_function_pass(
                10,
                1,
                prefix_counting.check_example_ipv4_topology_does_not_contain,
                "prefix",
            )

        with allure_step_with_separate_logging(
            "step_tc1_delete_bgp_peers_configuration"
        ):
            """Delete all previously configured BGP peers."""
            self.delete_ibgp_peer_configuration(BGP_PEER1_IP, cluster_id=False)
            self.delete_ibgp_peer_configuration(BGP_PEER2_IP, cluster_id=False)

        with allure_step_with_separate_logging(
            "step_tc2_configure_one_ibgp_route_reflector_client_and_one_ibgp_non_client"
        ):
            """Configure iBGP peers: 1st one as RR client, 2nd one as RR non-client."""
            self.configure_ibgp_peer(BGP_PEER1_IP, rr_client=True, cluster_id=False)
            self.configure_ibgp_peer(BGP_PEER2_IP, rr_client=False, cluster_id=False)

        with allure_step_with_separate_logging("step_tc2_connect_bgp_peer1"):
            """Connect BGP peer."""
            self.connect_bgp_peer1_and_verify()

        with allure_step_with_separate_logging("step_tc2_connect_bgp_peer2"):
            """Connect BGP peer."""
            self.connect_bgp_peer2_and_verify()

        with allure_step_with_separate_logging(
            "step_tc2_bgp_peer1_check_log_for_introduced_prefixes"
        ):
            """Check incomming updates for new routes."""
            utils.wait_until_function_pass(
                DEFAULT_LOG_CHECK_TIMEOUT,
                DEFAULT_LOG_CHECK_PERIOD,
                infra.verify_string_occurence_count_in_file,
                "nlri_prefix_received:",
                f"tmp/{BGP_PEER1_LOG_FILE}",
                BGP_PEER2_PREFIX_COUNT,
            )
            infra.verify_string_occurence_count_in_file(
                f"nlri_prefix_received: {BGP_PEER2_FIRST_PREFIX_IP}/{PREFIX_LEN}",
                f"tmp/{BGP_PEER1_LOG_FILE}",
                1,
            )
            infra.verify_string_occurence_count_in_file(
                "withdrawn_prefix_received:", f"tmp/{BGP_PEER1_LOG_FILE}", 0
            )

        with allure_step_with_separate_logging(
            "step_tc2_bgp_peer2_check_log_for_introduced_prefixes"
        ):
            """Check incomming updates for new routes."""
            utils.wait_until_function_pass(
                DEFAULT_LOG_CHECK_TIMEOUT,
                DEFAULT_LOG_CHECK_PERIOD,
                infra.verify_string_occurence_count_in_file,
                "nlri_prefix_received:",
                f"tmp/{BGP_PEER2_LOG_FILE}",
                BGP_PEER1_PREFIX_COUNT,
            )
            infra.verify_string_occurence_count_in_file(
                f"nlri_prefix_received: {BGP_PEER1_FIRST_PREFIX_IP}/{PREFIX_LEN}",
                f"tmp/{BGP_PEER2_LOG_FILE}",
                1,
            )
            infra.verify_string_occurence_count_in_file(
                "withdrawn_prefix_received:", f"tmp/{BGP_PEER2_LOG_FILE}", 0
            )

        with allure_step_with_separate_logging("step_tc2_disconnect_bgp_peer1"):
            """Stop BGP peer & store logs."""
            bgp.stop_bgp_speaker(self.bgp_peer1_process)
            infra.shell(f"tmp/{BGP_PEER1_LOG_FILE} results/tc2_{BGP_PEER1_LOG_FILE}")

        with allure_step_with_separate_logging(
            "step_tc2_bgp_peer2_check_log_for_withdrawn_prefixes"
        ):
            """Check incomming updates for withdrawn routes."""
            utils.wait_until_function_pass(
                DEFAULT_LOG_CHECK_TIMEOUT,
                DEFAULT_LOG_CHECK_PERIOD,
                infra.verify_string_occurence_count_in_file,
                "withdrawn_prefix_received:",
                f"tmp/{BGP_PEER2_LOG_FILE}",
                BGP_PEER1_PREFIX_COUNT,
            )
            infra.verify_string_occurence_count_in_file(
                f"withdrawn_prefix_received: {BGP_PEER1_FIRST_PREFIX_IP}/{PREFIX_LEN}",
                f"tmp/{BGP_PEER2_LOG_FILE}",
                1,
            )

        with allure_step_with_separate_logging("step_tc2_disconnect_bgp_peer2"):
            """Stop BGP peer & store logs."""
            bgp.stop_bgp_speaker(self.bgp_peer2_process)
            infra.shell(f"tmp/{BGP_PEER2_LOG_FILE} results/tc2_{BGP_PEER2_LOG_FILE}")

        with allure_step_with_separate_logging(
            "step_tc2_check_for_empty_ipv4_topology"
        ):
            """Checks for empty topology after"""
            utils.wait_until_function_pass(
                10,
                1,
                prefix_counting.check_example_ipv4_topology_does_not_contain,
                "prefix",
            )

        with allure_step_with_separate_logging(
            "step_tc2_delete_bgp_peers_configuration"
        ):
            """Delete all previously configured BGP peers."""
            self.delete_ibgp_peer_configuration(BGP_PEER1_IP, cluster_id=False)
            self.delete_ibgp_peer_configuration(BGP_PEER2_IP, cluster_id=False)

        with allure_step_with_separate_logging(
            "step_tc3_configure_two_ibgp_non_client_peers"
        ):
            """Configure iBGP peers: 1st one as RR client, 2nd one as RR non-client."""
            self.configure_ibgp_peer(BGP_PEER1_IP, rr_client=False, cluster_id=False)
            self.configure_ibgp_peer(BGP_PEER2_IP, rr_client=False, cluster_id=False)

        with allure_step_with_separate_logging("step_tc3_connect_bgp_peer1"):
            """Connect BGP peer."""
            self.connect_bgp_peer1_and_verify()

        with allure_step_with_separate_logging("step_tc3_connect_bgp_peer2"):
            """Connect BGP peer."""
            self.connect_bgp_peer2_and_verify()

        with allure_step_with_separate_logging(
            "step_tc3_bgp_peer1_check_log_for_no_updates"
        ):
            """Check for no updates received by iBGP peer No. 1."""
            utils.wait_until_function_pass(
                DEFAULT_LOG_CHECK_TIMEOUT,
                DEFAULT_LOG_CHECK_PERIOD,
                infra.verify_string_occurence_count_in_file,
                "total_received_update_message_counter: 1",
                f"tmp/{BGP_PEER1_LOG_FILE}",
                2,
            )

        with allure_step_with_separate_logging("step_tc3_disconnect_bgp_peer1"):
            """Stop BGP peer & store logs."""
            bgp.stop_bgp_speaker(self.bgp_peer1_process)
            infra.shell(f"tmp/{BGP_PEER1_LOG_FILE} results/tc3_{BGP_PEER1_LOG_FILE}")

        with allure_step_with_separate_logging(
            "step_tc3_bgp_peer2_check_log_for_no_updates"
        ):
            """Check for no updates received by iBGP peer No. 2."""
            utils.wait_until_function_pass(
                DEFAULT_LOG_CHECK_TIMEOUT * 2,
                DEFAULT_LOG_CHECK_PERIOD,
                infra.verify_string_occurence_count_in_file,
                "total_received_update_message_counter: 1",
                f"tmp/{BGP_PEER2_LOG_FILE}",
                4,
            )

        with allure_step_with_separate_logging("step_tc3_disconnect_bgp_peer2"):
            """Stop BGP peer & store logs."""
            bgp.stop_bgp_speaker(self.bgp_peer2_process)
            infra.shell(f"tmp/{BGP_PEER2_LOG_FILE} results/tc3_{BGP_PEER2_LOG_FILE}")

        with allure_step_with_separate_logging(
            "step_tc3_check_for_empty_ipv4_topology"
        ):
            """Checks for empty topology after"""
            utils.wait_until_function_pass(
                10,
                1,
                prefix_counting.check_example_ipv4_topology_does_not_contain,
                "prefix",
            )

        with allure_step_with_separate_logging(
            "step_tc3_delete_bgp_peers_configuration"
        ):
            """Delete all previously configured BGP peers."""
            self.delete_ibgp_peer_configuration(BGP_PEER1_IP, cluster_id=False)
            self.delete_ibgp_peer_configuration(BGP_PEER2_IP, cluster_id=False)

        with allure_step_with_separate_logging(
            "step_tc4_configure_two_ibgp_rr_clients_with_cluster_id"
        ):
            """Configure two iBGP peers as routing reflector clients with cluster-id argument."""
            self.configure_ibgp_peer(BGP_PEER1_IP, rr_client=True, cluster_id=True)
            self.configure_ibgp_peer(BGP_PEER2_IP, rr_client=True, cluster_id=False)

        with allure_step_with_separate_logging("step_tc4_connect_bgp_peers"):
            """Connect BGP peers, each set to send 3 prefixes."""
            default_cluster_id = "192.0.2.2"
            self.connect_bgp_peer1_and_verify(cluster_id="127.0.0.4")
            self.connect_bgp_peer2_and_verify(cluster_id=BGP_PEER2_IP)

        with allure_step_with_separate_logging(
            "step_tc4_bgp_peer1_check_rib_out_for_introduced_prefixes"
        ):
            """Check incomming updates for new routes and respective cluster-ids
            on first peer which should contain default-cluster id from global config reflected
            from the second peer equal to router-id."""
            mapping = {
                "IP": BGP_PEER1_IP,
                "BGP_RIB_OPENCONFIG": "example-bgp-rib",
                "PEER_NUMBER": 2,
                "CLUSTER_ID": BGP_PEER2_IP,
                "DEFAULT_ID": "192.0.2.2",
            }
            utils.wait_until_function_pass(
                10,
                1,
                templated_requests.get_templated_request,
                f"{BGP_DATA_FOLDER}/cluster_id/peer_rib_out",
                mapping,
                verify=True,
            )

        with allure_step_with_separate_logging(
            "step_tc4_bgp_peer2_check_rib_out_for_introduced_prefixes"
        ):
            """Check incomming updates for new routes and respective cluster-ids
            in second peer which has local route-reflector-cluster-id."""
            mapping = {
                "IP": BGP_PEER2_IP,
                "BGP_RIB_OPENCONFIG": "example-bgp-rib",
                "PEER_NUMBER": 1,
                "CLUSTER_ID": BGP_PEER1_IP,
                "DEFAULT_ID": "127.0.0.4",
            }
            utils.wait_until_function_pass(
                10,
                1,
                templated_requests.get_templated_request,
                f"{BGP_DATA_FOLDER}/cluster_id/peer_rib_out",
                mapping,
                verify=True,
            )

        with allure_step_with_separate_logging("step_tc4_disconnect_bgp_peers"):
            """Stop BGP peer & store logs."""
            bgp.stop_bgp_speaker(self.bgp_peer1_process)
            infra.shell(f"tmp/{BGP_PEER1_LOG_FILE} results/tc4_{BGP_PEER1_LOG_FILE}")
            bgp.stop_bgp_speaker(self.bgp_peer2_process)
            infra.shell(f"tmp/{BGP_PEER2_LOG_FILE} results/tc4_{BGP_PEER2_LOG_FILE}")

        with allure_step_with_separate_logging(
            "step_tc4_check_for_empty_ipv4_topology"
        ):
            """Checks for empty topology after"""
            utils.wait_until_function_pass(
                10,
                1,
                prefix_counting.check_example_ipv4_topology_does_not_contain,
                "prefix",
            )

        with allure_step_with_separate_logging(
            "step_tc4_delete_bgp_peers_configuration"
        ):
            """Delete all previously configured BGP peers."""
            self.delete_ibgp_peer_configuration(BGP_PEER1_IP, cluster_id=True)
            self.delete_ibgp_peer_configuration(BGP_PEER2_IP, cluster_id=False)
