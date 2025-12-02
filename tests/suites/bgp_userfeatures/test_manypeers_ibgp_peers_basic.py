#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# Basic tests for iBGP peers.
#
# Test suite performs basic iBGP functional test cases for BGP peers in
# different roles (iBGP, iBGP RR-client):
#
# Test Case 1: Two iBGP RR-client peers groups introduce prefixes
# Expected result: controller forwards updates towards both peers groups
#
# Test Case 2: Two iBGP peers groups: one RR client group and one non-client
# group introduces prefixes
# Expected result: controller forwards updates towards both peers groups
#
# Test Case 3: Two iBGP RR non-client peers groups introduce prefixes
# Expected result: controller does not forward any update towards peers groups
#
# Test Case 4: Two iBGP(play.py) RR-client peers configured, first group
# configured with route-reflector-cluster-id, second inherits it's
# cluster-id from global config. Each of them introduces 3 prefixes.
# Expected result: controller forwards updates towards both peers groups
# and each of their adj-rib-in contains routes. First peers group should
# contain default cluster-id and second cluster-id from first peers
# configuration.
#
# For polices see: https://wiki.opendaylight.org/view/BGP_LS_PCEP:BGP

import ipaddress
import logging
from typing import List

from jinja2 import Environment, FileSystemLoader
import pytest

from libraries import bgp
from libraries import infra
from libraries import prefix_counting
from libraries import templated_requests
from libraries import utils
from libraries.variables import variables


BGP_PEERS_COUNT = 20
ODL_IP = variables.ODL_IP
TOOLS_IP = variables.TOOLS_IP
BGP_PEER_LOG_LEVEL = "debug"
BGP_DATA_FOLDER = "variables/bgpuser/"
ODL_LOG_LEVEL = "INFO"
ODL_BGP_LOG_LEVEL = "DEFAULT"
BGP_PEERS1_IP = "127.0.1.0"
BGP_PEERS2_IP = "127.0.2.0"
PREFIX_LEN = 28
BGP_PEER1_FIRST_PREFIX_IP = "8.1.0.0"
BGP_PEER2_FIRST_PREFIX_IP = "8.2.0.0"
PREFIXES_PER_PEER = 1
PREFIX_COUNT = int(BGP_PEERS_COUNT * PREFIXES_PER_PEER / 2)
BGP_PEER1_LOG_FILE = "bgp_peer1.log"
BGP_PEER2_LOG_FILE = "bgp_peer2.log"
DEFAULT_LOG_CHECK_TIMEOUT = 20
DEFAULT_LOG_CHECK_PERIOD = 1

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=68)
class TestIbgpPeersBasic:
    bgp_peer1_process = None
    bgp_peer2_process = None

    def verify_topology_is_as_expected(self, tempalate_path):
        templated_requests.get_templated_request(tempalate_path, None, verify=True)

    def wait_until_expected_topology(self, template_path, retry_count=20, interval=1):
        utils.wait_until_function_pass(
            retry_count, interval, self.verify_topology_is_as_expected, template_path
        )

    def configure_ibgp_peers(
        self, ips: List[str], rr_client: bool, cluster_id: bool = False
    ):
        template_folder = "cluster_id/ibgp_peer" if cluster_id else "ibgp_peers"
        for ip in ips:
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

    def delete_ibgp_peer_configuration(self, ips: List[str], cluster_id: bool = False):
        template_folder = "cluster_id/ibgp_peer" if cluster_id else "ibgp_peers"
        for ip in ips:
            mapping = {"IP": ip, "BGP_RIB_OPENCONFIG": "example-bgp-rib"}
            templated_requests.delete_templated_request(
                f"{BGP_DATA_FOLDER}/{template_folder}", mapping
            )

    def connect_bgp_peers1_and_verify(self, cluster_id: str = None):
        self.bgp_peer1_process = bgp.start_bgp_speaker(
            firstprefix=BGP_PEER1_FIRST_PREFIX_IP,
            prefixlen=PREFIX_LEN,
            ammount=PREFIX_COUNT,
            my_ip=BGP_PEERS1_IP,
            peer_ip=ODL_IP,
            cluster=cluster_id,
            log_level=BGP_PEER_LOG_LEVEL,
            log_file=BGP_PEER1_LOG_FILE,
            multiplicity=int(BGP_PEERS_COUNT / 2),
        )
        utils.wait_until_function_pass(
            10,
            1,
            prefix_counting.check_example_ipv4_topology_contains,
            f"'prefix': '{BGP_PEER1_FIRST_PREFIX_IP}/{PREFIX_LEN}'",
        )

    def connect_bgp_peers2_and_verify(self, cluster_id: str = None):
        self.bgp_peer2_process = bgp.start_bgp_speaker(
            firstprefix=BGP_PEER2_FIRST_PREFIX_IP,
            prefixlen=PREFIX_LEN,
            ammount=PREFIX_COUNT,
            my_ip=BGP_PEERS2_IP,
            peer_ip=ODL_IP,
            cluster=cluster_id,
            log_level=BGP_PEER_LOG_LEVEL,
            log_file=BGP_PEER2_LOG_FILE,
            multiplicity=int(BGP_PEERS_COUNT / 2),
        )
        utils.wait_until_function_pass(
            10,
            1,
            prefix_counting.check_example_ipv4_topology_contains,
            f"'prefix': '{BGP_PEER2_FIRST_PREFIX_IP}/{PREFIX_LEN}'",
        )

    def check_peer_adj_rib_out(self, peer_ip, skipped_prefix):
        # Get adj-rib-out data as stored in ODL
        mapping = {"IP": peer_ip, "BGP_RIB_OPENCONFIG": "example-bgp-rib"}
        response = templated_requests.get_templated_request(
            f"{BGP_DATA_FOLDER}/cluster_id/peer_rib_out", mapping
        )

        # Get exepected adj-rib-out data based on peer_rib_out.j2 jinja template
        prefix = ipaddress.IPv4Address(BGP_PEER1_FIRST_PREFIX_IP)
        cluster_id = ipaddress.IPv4Address(BGP_PEERS1_IP)
        peers1_data = [
            {
                "prefix": prefix + i * 16,
                "cluster_id": cluster_id + i,
                "default_id": "127.0.0.4",
            }
            for i in range(int(BGP_PEERS_COUNT / 2))
            if prefix + i * 16 != skipped_prefix
        ]
        prefix = ipaddress.IPv4Address(BGP_PEER2_FIRST_PREFIX_IP)
        peers2_data = [
            {
                "prefix": prefix + i * 16,
                "cluster_id": BGP_PEERS2_IP,
                "default_id": "192.0.2.2",
            }
            for i in range(int(BGP_PEERS_COUNT / 2))
            if prefix + i * 16 != skipped_prefix
        ]
        peers_data = peers1_data + peers2_data
        env = Environment(
            loader=FileSystemLoader(
                f"{BGP_DATA_FOLDER}/cluster_id/expected_peer_rib_out_manypeers"
            )
        )
        # generate config file for bgp-flowspec-manypeers.cfg
        template = env.get_template("peer_rib_out.j2")
        expected_response = template.render(
            {"peers_data": peers_data, "skipped_prefix": skipped_prefix}
        )

        utils.verify_jsons_match(
            response.text,
            expected_response,
            json1_data_label="received data",
            json2_data_label="expected data",
        )

    def test_ibgp_peers_basic(self, allure_step_with_separate_logging):

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
            "step_tc1_configure_two_ibgp_route_reflector_client_peers"
        ):
            """Configure two iBGP group peers as routing reflector clients."""
            bgp_peers1_first_ip = ipaddress.IPv4Address(BGP_PEERS1_IP)
            bgp_peers1_ips = [
                str(bgp_peers1_first_ip + i) for i in range(int(BGP_PEERS_COUNT / 2))
            ]
            self.configure_ibgp_peers(bgp_peers1_ips, rr_client=True, cluster_id=False)
            bgp_peers2_first_ip = ipaddress.IPv4Address(BGP_PEERS2_IP)
            bgp_peers2_ips = [
                str(bgp_peers2_first_ip + i) for i in range(int(BGP_PEERS_COUNT / 2))
            ]
            self.configure_ibgp_peers(bgp_peers2_ips, rr_client=True, cluster_id=False)

        with allure_step_with_separate_logging("step_tc1_connect_bgp_peer1"):
            """Connect BGP peers."""
            self.connect_bgp_peers1_and_verify()

        with allure_step_with_separate_logging("step_tc1_connect_bgp_peer2"):
            """Connect BGP peers."""
            self.connect_bgp_peers2_and_verify()

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
                (PREFIX_COUNT * 2 - PREFIXES_PER_PEER) * int(BGP_PEERS_COUNT / 2),
                exact=False,
            )
            infra.verify_string_occurence_count_in_file(
                f"nlri_prefix_received: {BGP_PEER2_FIRST_PREFIX_IP}/{PREFIX_LEN}",
                f"tmp/{BGP_PEER1_LOG_FILE}",
                int(BGP_PEERS_COUNT / 2),
                exact=False,
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
                (PREFIX_COUNT * 2 - PREFIXES_PER_PEER) * int(BGP_PEERS_COUNT / 2),
                exact=False,
            )
            infra.verify_string_occurence_count_in_file(
                f"nlri_prefix_received: {BGP_PEER1_FIRST_PREFIX_IP}/{PREFIX_LEN}",
                f"tmp/{BGP_PEER2_LOG_FILE}",
                int(BGP_PEERS_COUNT / 2),
                exact=False,
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
                PREFIX_COUNT * int(BGP_PEERS_COUNT / 2),
                exact=False,
            )
            infra.verify_string_occurence_count_in_file(
                f"withdrawn_prefix_received: {BGP_PEER1_FIRST_PREFIX_IP}/{PREFIX_LEN}",
                f"tmp/{BGP_PEER2_LOG_FILE}",
                int(BGP_PEERS_COUNT / 2),
                exact=False,
            )

        with allure_step_with_separate_logging("step_tc1_disconnect_bgp_peer2"):
            """Stop BGP peers & store logs."""
            bgp.stop_bgp_speaker(self.bgp_peer2_process)
            infra.shell(f"tmp/{BGP_PEER2_LOG_FILE} results/tc1_{BGP_PEER2_LOG_FILE}")

        with allure_step_with_separate_logging(
            "step_tc1_check_for_empty_ipv4_topology"
        ):
            """Checks for empty topology after."""
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
            bgp_peers1_first_ip = ipaddress.IPv4Address(BGP_PEERS1_IP)
            bgp_peers1_ips = [
                str(bgp_peers1_first_ip + i) for i in range(int(BGP_PEERS_COUNT / 2))
            ]
            self.delete_ibgp_peer_configuration(bgp_peers1_ips, cluster_id=False)
            bgp_peers2_first_ip = ipaddress.IPv4Address(BGP_PEERS2_IP)
            bgp_peers2_ips = [
                str(bgp_peers2_first_ip + i) for i in range(int(BGP_PEERS_COUNT / 2))
            ]
            self.delete_ibgp_peer_configuration(bgp_peers2_ips, cluster_id=False)

        with allure_step_with_separate_logging(
            "step_tc2_configure_one_ibgp_route_reflector_client_and_one_ibgp_non_client"
        ):
            """Configure iBGP peers groups: 1st one as RR client, 2nd
            one as RR non-client."""
            bgp_peers1_first_ip = ipaddress.IPv4Address(BGP_PEERS1_IP)
            bgp_peers1_ips = [
                str(bgp_peers1_first_ip + i) for i in range(int(BGP_PEERS_COUNT / 2))
            ]
            self.configure_ibgp_peers(bgp_peers1_ips, rr_client=True, cluster_id=False)
            bgp_peers2_first_ip = ipaddress.IPv4Address(BGP_PEERS2_IP)
            bgp_peers2_ips = [
                str(bgp_peers2_first_ip + i) for i in range(int(BGP_PEERS_COUNT / 2))
            ]
            self.configure_ibgp_peers(bgp_peers2_ips, rr_client=False, cluster_id=False)

        with allure_step_with_separate_logging("step_tc2_connect_bgp_peer1"):
            """Connect BGP peers."""
            self.connect_bgp_peers1_and_verify()

        with allure_step_with_separate_logging("step_tc2_connect_bgp_peer2"):
            """Connect BGP peers."""
            self.connect_bgp_peers2_and_verify()

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
                (PREFIX_COUNT * 2 - PREFIXES_PER_PEER) * int(BGP_PEERS_COUNT / 2),
                exact=False,
            )
            infra.verify_string_occurence_count_in_file(
                f"nlri_prefix_received: {BGP_PEER2_FIRST_PREFIX_IP}/{PREFIX_LEN}",
                f"tmp/{BGP_PEER1_LOG_FILE}",
                int(BGP_PEERS_COUNT / 2),
                exact=False,
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
                PREFIX_COUNT * int(BGP_PEERS_COUNT / 2),
                exact=False,
            )
            infra.verify_string_occurence_count_in_file(
                f"nlri_prefix_received: {BGP_PEER1_FIRST_PREFIX_IP}/{PREFIX_LEN}",
                f"tmp/{BGP_PEER2_LOG_FILE}",
                int(BGP_PEERS_COUNT / 2),
                exact=False,
            )
            infra.verify_string_occurence_count_in_file(
                "withdrawn_prefix_received:", f"tmp/{BGP_PEER2_LOG_FILE}", 0
            )

        with allure_step_with_separate_logging("step_tc2_disconnect_bgp_peer1"):
            """Stop BGP peers & store logs."""
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
                PREFIX_COUNT * int(BGP_PEERS_COUNT / 2),
                exact=False,
            )
            infra.verify_string_occurence_count_in_file(
                f"withdrawn_prefix_received: {BGP_PEER1_FIRST_PREFIX_IP}/{PREFIX_LEN}",
                f"tmp/{BGP_PEER2_LOG_FILE}",
                int(BGP_PEERS_COUNT / 2),
                exact=False,
            )

        with allure_step_with_separate_logging("step_tc2_disconnect_bgp_peer2"):
            """Stop BGP peers & store logs."""
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
            bgp_peers1_first_ip = ipaddress.IPv4Address(BGP_PEERS1_IP)
            bgp_peers1_ips = [
                str(bgp_peers1_first_ip + i) for i in range(int(BGP_PEERS_COUNT / 2))
            ]
            self.delete_ibgp_peer_configuration(bgp_peers1_ips, cluster_id=False)
            bgp_peers2_first_ip = ipaddress.IPv4Address(BGP_PEERS2_IP)
            bgp_peers2_ips = [
                str(bgp_peers2_first_ip + i) for i in range(int(BGP_PEERS_COUNT / 2))
            ]
            self.delete_ibgp_peer_configuration(bgp_peers2_ips, cluster_id=False)

        with allure_step_with_separate_logging(
            "step_tc3_configure_two_ibgp_non_client_peers"
        ):
            """Configure iBGP peers groups: 1st one as RR client,
            2nd one as RR non-client."""
            bgp_peers1_first_ip = ipaddress.IPv4Address(BGP_PEERS1_IP)
            bgp_peers1_ips = [
                str(bgp_peers1_first_ip + i) for i in range(int(BGP_PEERS_COUNT / 2))
            ]
            self.configure_ibgp_peers(bgp_peers1_ips, rr_client=False, cluster_id=False)
            bgp_peers2_first_ip = ipaddress.IPv4Address(BGP_PEERS2_IP)
            bgp_peers2_ips = [
                str(bgp_peers2_first_ip + i) for i in range(int(BGP_PEERS_COUNT / 2))
            ]
            self.configure_ibgp_peers(bgp_peers2_ips, rr_client=False, cluster_id=False)

        with allure_step_with_separate_logging("step_tc3_connect_bgp_peer1"):
            """Connect BGP peers."""
            self.connect_bgp_peers1_and_verify()

        with allure_step_with_separate_logging("step_tc3_connect_bgp_peer2"):
            """Connect BGP peers."""
            self.connect_bgp_peers2_and_verify()

        with allure_step_with_separate_logging(
            "step_tc3_bgp_peer1_check_log_for_no_updates"
        ):
            """Check for no updates received by iBGP peer No. 1."""
            utils.wait_until_function_pass(
                DEFAULT_LOG_CHECK_TIMEOUT * 2,
                DEFAULT_LOG_CHECK_PERIOD,
                infra.verify_string_occurence_count_in_file,
                "total_received_update_message_counter: 1",
                f"tmp/{BGP_PEER1_LOG_FILE}",
                BGP_PEERS_COUNT,
                exact=False,
            )

        with allure_step_with_separate_logging("step_tc3_disconnect_bgp_peer1"):
            """Stop BGP peers & store logs."""
            bgp.stop_bgp_speaker(self.bgp_peer1_process)
            infra.shell(f"tmp/{BGP_PEER1_LOG_FILE} results/tc3_{BGP_PEER1_LOG_FILE}")

        with allure_step_with_separate_logging(
            "step_tc3_bgp_peer2_check_log_for_no_updates"
        ):
            """Check for no updates received by iBGP peer No. 2."""
            utils.wait_until_function_pass(
                DEFAULT_LOG_CHECK_TIMEOUT * 4,
                DEFAULT_LOG_CHECK_PERIOD,
                infra.verify_string_occurence_count_in_file,
                "total_received_update_message_counter: 1",
                f"tmp/{BGP_PEER2_LOG_FILE}",
                4,
                exact=False,
            )

        with allure_step_with_separate_logging("step_tc3_disconnect_bgp_peer2"):
            """Stop BGP peers & store logs."""
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
            bgp_peers1_first_ip = ipaddress.IPv4Address(BGP_PEERS1_IP)
            bgp_peers1_ips = [
                str(bgp_peers1_first_ip + i) for i in range(int(BGP_PEERS_COUNT / 2))
            ]
            self.delete_ibgp_peer_configuration(bgp_peers1_ips, cluster_id=False)
            bgp_peers2_first_ip = ipaddress.IPv4Address(BGP_PEERS2_IP)
            bgp_peers2_ips = [
                str(bgp_peers2_first_ip + i) for i in range(int(BGP_PEERS_COUNT / 2))
            ]
            self.delete_ibgp_peer_configuration(bgp_peers2_ips, cluster_id=False)

        with allure_step_with_separate_logging(
            "step_tc4_configure_two_ibgp_rr_clients_with_cluster_id"
        ):
            """Configure two iBGP peers groups  as routing reflector clients
            with cluster-id argument."""
            bgp_peers1_first_ip = ipaddress.IPv4Address(BGP_PEERS1_IP)
            bgp_peers1_ips = [
                str(bgp_peers1_first_ip + i) for i in range(int(BGP_PEERS_COUNT / 2))
            ]
            self.configure_ibgp_peers(bgp_peers1_ips, rr_client=True, cluster_id=True)
            bgp_peers2_first_ip = ipaddress.IPv4Address(BGP_PEERS2_IP)
            bgp_peers2_ips = [
                str(bgp_peers2_first_ip + i) for i in range(int(BGP_PEERS_COUNT / 2))
            ]
            self.configure_ibgp_peers(bgp_peers2_ips, rr_client=True, cluster_id=False)

        with allure_step_with_separate_logging("step_tc4_connect_bgp_peers"):
            """Connect BGP peers, each set to send 3 prefixes."""
            default_cluster_id = "192.0.2.2"
            self.connect_bgp_peers1_and_verify(cluster_id="127.0.0.4")
            self.connect_bgp_peers2_and_verify(cluster_id=BGP_PEERS2_IP)

        with allure_step_with_separate_logging(
            "step_tc4_bgp_peer1_check_rib_out_for_introduced_prefixes"
        ):
            """Check incomming updates for new routes and respective cluster-ids
            on first peers group which should contain default-cluster id from global
            config reflected from the second peers group equal to router-id."""
            bgp_peer_ip = ipaddress.IPv4Address(BGP_PEERS1_IP)
            prefix_to_be_skipped = ipaddress.IPv4Address(BGP_PEER1_FIRST_PREFIX_IP)
            for _ in range(int(BGP_PEERS_COUNT / 2)):
                self.check_peer_adj_rib_out(bgp_peer_ip, prefix_to_be_skipped)
                bgp_peer_ip += 1
                prefix_to_be_skipped += 16

        with allure_step_with_separate_logging(
            "step_tc4_bgp_peer2_check_rib_out_for_introduced_prefixes"
        ):
            """Check incomming updates for new routes and respective cluster-ids
            in second peer which has local route-reflector-cluster-id."""
            bgp_peer_ip = ipaddress.IPv4Address(BGP_PEERS2_IP)
            prefix_to_be_skipped = ipaddress.IPv4Address(BGP_PEER2_FIRST_PREFIX_IP)
            for _ in range(int(BGP_PEERS_COUNT / 2)):
                self.check_peer_adj_rib_out(bgp_peer_ip, prefix_to_be_skipped)
                bgp_peer_ip += 1
                prefix_to_be_skipped += 16

        with allure_step_with_separate_logging("step_tc4_disconnect_bgp_peers"):
            """Stop BGP peers & store logs."""
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
            bgp_peers1_first_ip = ipaddress.IPv4Address(BGP_PEERS1_IP)
            bgp_peers1_ips = [
                str(bgp_peers1_first_ip + i) for i in range(int(BGP_PEERS_COUNT / 2))
            ]
            self.delete_ibgp_peer_configuration(bgp_peers1_ips, cluster_id=True)
            bgp_peers2_first_ip = ipaddress.IPv4Address(BGP_PEERS2_IP)
            bgp_peers2_ips = [
                str(bgp_peers2_first_ip + i) for i in range(int(BGP_PEERS_COUNT / 2))
            ]
            self.delete_ibgp_peer_configuration(bgp_peers2_ips, cluster_id=False)
