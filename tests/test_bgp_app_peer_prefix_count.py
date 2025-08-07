#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# BGP performance of ingesting from 1 BGP application peer
# Test suite performs basic BGP performance test cases for
# BGP application peer. BGP application peer introduces routes
# using restconf - in two steps:
# 1. introduces the 100000 number of routes in one POST request
# 2. POSTs the rest of routes (up to the 180000 number) one by one
# Test suite checks that the prefixes are propagated to IPv4 topology
# and announced to BGP peer via updates. Test case where the BGP peer
# is disconnected and reconnected and all routes are deleted by
# BGP application peer are performed as well. Brief description
# how to configure BGP application peer and how to use
# restconf application peer interface:
# https://wiki.opendaylight.org/view/BGP_LS_PCEP:User_Guide#BGP_Application_Peer
# https://wiki.opendaylight.org/view/BGP_LS_PCEP:Programmer_Guide#BGP
# http://docs.opendaylight.org/en/stable-boron/user-guide/bgp-user-guide.html#bgp-peering
# http://docs.opendaylight.org/en/stable-boron/user-guide/bgp-user-guide.html#application-peer-configuration
# Reported bugs:
# Bug 4689 - Not a reasonable duration of 1M prefix introduction
# from BGP application peer via restconf
# Bug 4791 - BGPSessionImpl: Failed to send message Update logged
# even all UPDATE mesages received by iBGP peer


import logging
import os
import pytest

from lib import bgp
from lib import infra
from lib import ip_topology
from lib import utils


PREFILL_COUNT = 100_000
ADDITIONAL_COUNT = 80_000
TOTAL_COUNT = PREFILL_COUNT + ADDITIONAL_COUNT
TEST_DURATION_MULTIPLIER = int(os.environ["TEST_DURATION_MULTIPLIER"])
BGP_FILLING_TIMEOUT = TEST_DURATION_MULTIPLIER * (TOTAL_COUNT * 4.0 / 10000 + 50)
BGP_EMPTYING_TIMEOUT = BGP_FILLING_TIMEOUT * 3 / 4
CHECK_INTERVAL = 5
CHECK_RETRY_COUNT = int(BGP_FILLING_TIMEOUT / CHECK_INTERVAL)

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=4)
class TestBgpAppPeerPrefixCount:
    bgp_speaker_process = None
    last_change_count_single = 1

    def test_bgp_app_single_peer_prfix_count(self, allure_step_with_separate_logging):

        with allure_step_with_separate_logging(
            "step_check_for_empty_topology_before_starting"
        ):
            """Wait for example-ipv4-topology to come up and empty.
            Give large timeout for case when BGP boots slower than restconf."""
            topology_count = utils.wait_until_function_returns_value(
                120, 1, 0, ip_topology.get_ipv4_topology_prefixes_count
            )
            assert (
                topology_count == 0
            ), f"Ipv4 topology is not empty as expected, there are {topology_count} " \
               f"prefixes present."

        with allure_step_with_separate_logging(
            "step_reconfigure_odl_to_accept_connections"
        ):
            """Configure BGP peer module with initiate-connection set to false."""
            bgp.set_bgp_neighbour(ip="127.0.0.1", passive_mode=True)

        with allure_step_with_separate_logging(
            "step_reconfigure_odl_to_accept_bgp_application_peer"
        ):
            """Configure BGP application peer module."""
            bgp.set_bgp_application_peer(ip="10.0.0.10")

        with allure_step_with_separate_logging("step_connect_bgp_peers"):
            """Start BGP peer tool"""
            TestBgpAppPeerPrefixCount.bgp_speaker_process = bgp.start_bgp_speaker(
                ammount=0, listen=False, log_level="info"
            )
            assert infra.is_process_still_running(
                TestBgpAppPeerPrefixCount.bgp_speaker_process
            ), "Bgp speaker process is not running"

        with allure_step_with_separate_logging(
            "step_bgp_application_peer_prefill_routes"
        ):
            """Start BGP application peer tool and prefill routes."""
            bgp.start_bgp_app_peer(
                count=PREFILL_COUNT, log_level="info", timeout=BGP_FILLING_TIMEOUT
            )
            infra.shell("mv bgp_app_peer.log results/bgp_app_peer_prefill.log")

        with allure_step_with_separate_logging(
            "step_wait_for_ip_topology_is_prefilled"
        ):
            """Wait until example-ipv4-topology reaches the target prfix count."""
            count = utils.wait_until_function_returns_value(
                retry_count=CHECK_RETRY_COUNT,
                interval=CHECK_INTERVAL,
                expected_value=PREFILL_COUNT,
                function=ip_topology.get_ipv4_topology_prefixes_count,
            )
            assert (
                count == PREFILL_COUNT
            ), "Not all expected prefixes were found in ipv4 topology"

        with allure_step_with_separate_logging(
            "step_check_bgp_peer_updates_for_prefilled_routes"
        ):
            """Count the routes introduced by updates."""
            count = infra.wait_for_string_in_file(
                CHECK_RETRY_COUNT,
                CHECK_INTERVAL,
                f"total_received_nlri_prefix_counter: {PREFILL_COUNT}",
                "bgp_peer.log",
            )
            assert (
                1 <= count
            ), "Did not find expected received prefixes in bgp_peer.log file"

        with allure_step_with_separate_logging(
            "step_bgp_application_peer_intorduce_single_routes"
        ):
            """Start BGP application peer tool and introduce routes."""
            bgp.start_bgp_app_peer(
                count=ADDITIONAL_COUNT,
                command="add",
                prefix="12.0.0.0",
                log_level="info",
                timeout=BGP_FILLING_TIMEOUT,
            )
            infra.shell(
                "mv bgp_app_peer.log results/bgp_app_peer_singles.log", check_rc=True
            )

        with allure_step_with_separate_logging("step_wait_for_ip_topology_is_filled"):
            """Wait until example-ipv4-topology reaches the target prfix count."""
            count = utils.wait_until_function_returns_value(
                CHECK_RETRY_COUNT,
                CHECK_INTERVAL,
                TOTAL_COUNT,
                ip_topology.get_ipv4_topology_prefixes_count,
            )
            assert (
                count == TOTAL_COUNT
            ), "Not all expected prefixes were found in ipv4 topology"

        with allure_step_with_separate_logging(
            "step_check_bgp_peer_updates_for_all_routes"
        ):
            """Count the routes introduced by updates."""
            count = infra.wait_for_string_in_file(
                CHECK_RETRY_COUNT,
                CHECK_INTERVAL,
                f"total_received_nlri_prefix_counter: {TOTAL_COUNT}",
                "bgp_peer.log",
            )
            assert (
                1 <= count
            ), "Did not find expected received prefixes in bgp_peer.log file"

        with allure_step_with_separate_logging("step_disconnect_bgp_peer"):
            """Stop BGP peer tool."""
            bgp.stop_bgp_speaker(self.bgp_speaker_process)
            infra.shell(
                "mv bgp_peer.log results/bgp_peer_disconnect.log", check_rc=True
            )

        with allure_step_with_separate_logging("step_reconnect_bgp_peer"):
            """Start BGP peer tool."""
            TestBgpAppPeerPrefixCount.bgp_speaker_process = bgp.start_bgp_speaker(
                ammount=0, listen=False, log_level="info"
            )
            assert infra.is_process_still_running(
                TestBgpAppPeerPrefixCount.bgp_speaker_process
            ), "Bgp speaker process is not running"

        with allure_step_with_separate_logging(
            "step_check_bgp_peer_updates_for_reintroduced_routes"
        ):
            """Count the routes introduced by updates."""
            count = infra.wait_for_string_in_file(
                CHECK_RETRY_COUNT,
                CHECK_INTERVAL,
                f"total_received_nlri_prefix_counter: {TOTAL_COUNT}",
                "bgp_peer.log",
            )
            assert (
                1 <= count
            ), "Did not find expected received prefixes in bgp_peer.log file"

        with allure_step_with_separate_logging(
            "step_bgp_application_peer_delete_all_routes"
        ):
            """Start BGP application peer tool and delete all routes."""
            bgp.start_bgp_app_peer(
                command="delete-all", log_level="info", timeout=BGP_EMPTYING_TIMEOUT
            )
            infra.shell(
                "mv bgp_app_peer.log results/bgp_app_peer_delete_all.log", check_rc=True
            )

        with allure_step_with_separate_logging(
            "step_wait_for_stable_topology_after_deletion"
        ):
            """Wait until example-ipv4-topology becomes stable again."""
            ip_topology.wait_for_ipv4_topology_prefixes_to_become_stable(
                excluded_value=TOTAL_COUNT, timeout=BGP_EMPTYING_TIMEOUT
            )

        with allure_step_with_separate_logging(
            "step_check_for_empty_topology_after_deletion"
        ):
            """Example-ipv4-topology should be empty now."""
            count = ip_topology.get_ipv4_topology_prefixes_count()
            assert (
                0 == count
            ), "There are still some prefixes in ipv4 topology after deletion, " \
               "expected empty"

        with allure_step_with_separate_logging(
            "step_check_bgp_peer_updates_for_prefix_withdrawals"
        ):
            """Count the routes withdrawn by updates."""
            count = infra.wait_for_string_in_file(
                CHECK_RETRY_COUNT,
                CHECK_INTERVAL,
                f"total_received_withdrawn_prefix_counter: {TOTAL_COUNT}",
                "bgp_peer.log",
            )
            assert (
                1 <= count
            ), "Did not find expected received prefixes in bgp_peer.log file"

        with allure_step_with_separate_logging("step_stop_bgp_peer"):
            """Stop BGP peer tool."""
            bgp.stop_bgp_speaker(self.bgp_speaker_process)
            infra.shell("mv bgp_peer.log results/bgp_peer_reconnect.log", check_rc=True)

        with allure_step_with_separate_logging("step_delete_bgp_peer_configuration"):
            """Revert the BGP configuration to the original state: without any
            configured peers."""
            bgp.delete_bgp_neighbour("127.0.0.1")

        with allure_step_with_separate_logging(
            "step_delete_bgp_application_peer_configuration"
        ):
            """Revert the BGP configuration to the original state: without any
            configured peers."""
            bgp.delete_bgp_application_peer(ip="10.0.0.10")
