#
# Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# Based on the original Robot Framework integration test:
# https://github.com/opendaylight/integration-test/blob/901c7e139945b436d95a44b3b592904c3d7a4f9f/csit/suites/bgpcep/bgpingest/manypeers_peercount.robot
#

import logging

import allure

from libraries import bgp
from libraries import infra
from libraries import prefix_counting
from libraries import utils
from libraries.variables import variables


ODL_IP = variables.ODL_IP
TOOLS_IP = variables.TOOLS_IP
FIRST_PEER_IP = TOOLS_IP
BGP_TOOL_PORT = variables.BGP_TOOL_PORT
ODL_BGP_PORT = variables.ODL_BGP_PORT
TEST_DURATION_MULTIPLIER = variables.TEST_DURATION_MULTIPLIER
CHECK_PERIOD_PEER_COUNT_MANY = 20
REPETITIONS_PEER_COUNT_MANY = 4
# Lower bound for the topology filling timeout. The formula-based timeout scales
# with the route count; at the current tame placeholder scale it becomes too
# short to observe topology stability across several CHECK_PERIOD-spaced reads
# (which cannot even start until ingestion finishes). To be revisited together
# with the scale values.
MIN_FILLING_TIMEOUT = 240

BGP_TOOL_LOG_LEVEL = "info"
HOLDTIME = 180
RIB_INSTANCE = "example-bgp-rib"
# The BGP speaker's stdout (including each peer's per-peer statistics, tagged
# with its thread name, e.g. "BGP-Dummy-1:") is redirected to tmp/<this file>.
# Kept as start/stop_bgp_speaker's default name so that stop_bgp_speaker's debug
# dump of the same file succeeds.
BGP_PEERS_LOG_FILE_NAME = "play.py.out"
# How long to wait for every peer to report its expected received-prefix count.
CHECK_LOGS_RETRY_COUNT = 60
CHECK_LOGS_RETRY_PERIOD = 1


log = logging.getLogger(__name__)


class BaseTestManyPeerPeerCount:
    bgp_speaker_process = None

    def test_many_peers_peer_count(
        self,
        allure_step_with_separate_logging,
        bgp_peers_count,
        count_peer_count_many,
        insert,
        withdraw,
        prefill,
    ):
        test_description = getattr(self, "test_description", None)
        if test_description:
            allure.dynamic.description(test_description)

        bgp_filling_timeout = max(
            TEST_DURATION_MULTIPLIER * (count_peer_count_many * 9.0 / 10_000 + 40),
            MIN_FILLING_TIMEOUT,
        )
        bgp_emptying_timeout = bgp_filling_timeout * 3 / 4

        with allure_step_with_separate_logging(
            "step_check_for_empty_topology_before_talking"
        ):
            # Wait for example-ipv4-topology to come up and empty. Give large
            # timeout for case when BGP boots slower than restconf.
            utils.wait_until_function_pass(
                120, 1, prefix_counting.check_ipv4_topology_is_empty
            )

        with allure_step_with_separate_logging(
            "step_reconfigure_odl_to_accept_connections"
        ):
            # Configure BGP peers as route-reflector clients with
            # initiate-connection set to false. As route-reflector clients, ODL
            # reflects the routes received from one peer to all the others.
            bgp.set_bgp_rr_client_neighbours(
                first_neighbour_ip=FIRST_PEER_IP,
                count=bgp_peers_count,
                holdtime=HOLDTIME,
                peer_port=BGP_TOOL_PORT,
                rib_instance=RIB_INSTANCE,
                passive_mode=True,
                rr_client=True,
            )

        with allure_step_with_separate_logging("step_start_talking_bgp_managers"):
            # Start Python manager to connect speakers to ODL. Peers log their
            # received-prefix counters to BGP_PEERS_LOG_FILE_NAME.
            self.bgp_speaker_process = bgp.start_bgp_speaker(
                ammount=count_peer_count_many,
                multiplicity=bgp_peers_count,
                my_ip=TOOLS_IP,
                my_port=BGP_TOOL_PORT,
                peer_ip=ODL_IP,
                peer_port=ODL_BGP_PORT,
                insert=insert,
                withdraw=withdraw,
                prefill=prefill,
                listen=False,
                log_level=BGP_TOOL_LOG_LEVEL,
                log_file=BGP_PEERS_LOG_FILE_NAME,
            )

        with allure_step_with_separate_logging(
            "step_wait_for_stable_talking_ip_topology"
        ):
            # Wait until example-ipv4-topology reaches the target prefix count
            # and becomes stable.
            prefix_counting.wait_for_ipv4_topology_prefixes_to_become_stable(
                excluded_value=0,
                wait_period=CHECK_PERIOD_PEER_COUNT_MANY,
                consecutive_times_stable_value=REPETITIONS_PEER_COUNT_MANY,
                timeout=bgp_filling_timeout,
            )
            prefix_counting.check_ipv4_topology_prefixes_count(count_peer_count_many)

        with allure_step_with_separate_logging("step_check_logs_for_updates"):
            # Check each BGP peer's log for the expected number of received
            # prefixes. As route-reflector clients, every peer receives the
            # routes announced by all the other peers, i.e. the total count
            # minus its own share.
            expected_prefix_count = (
                count_peer_count_many - count_peer_count_many // bgp_peers_count
            )
            for index in range(1, bgp_peers_count + 1):
                expected_log_line = (
                    f"BGP-Dummy-{index}: "
                    f"total_received_nlri_prefix_counter: {expected_prefix_count}"
                )
                utils.wait_until_function_pass(
                    CHECK_LOGS_RETRY_COUNT,
                    CHECK_LOGS_RETRY_PERIOD,
                    infra.verify_string_occurence_count_in_file,
                    expected_log_line,
                    f"tmp/{BGP_PEERS_LOG_FILE_NAME}",
                    2,
                    exact=False,
                )

        with allure_step_with_separate_logging("step_kill_talking_bgp_speakers"):
            # Abort the Python speakers.
            bgp.stop_bgp_speaker(self.bgp_speaker_process)

        with allure_step_with_separate_logging(
            "step_wait_for_stable_ip_topology_after_talking"
        ):
            # Wait until example-ipv4-topology becomes stable again.
            prefix_counting.wait_for_ipv4_topology_prefixes_to_become_stable(
                excluded_value=count_peer_count_many,
                wait_period=CHECK_PERIOD_PEER_COUNT_MANY,
                consecutive_times_stable_value=REPETITIONS_PEER_COUNT_MANY,
                timeout=bgp_emptying_timeout,
            )

        with allure_step_with_separate_logging(
            "step_check_for_empty_ip_topology_after_talking"
        ):
            # Example-ipv4-topology should be empty now.
            prefix_counting.check_ipv4_topology_is_empty()

        with allure_step_with_separate_logging("step_delete_bgp_peers_configurations"):
            # Revert the BGP configuration to the original state: without any
            # configured peers.
            bgp.delete_bgp_neighbours(
                first_neighbour_ip=FIRST_PEER_IP,
                count=bgp_peers_count,
                rib_instance=RIB_INSTANCE,
            )
