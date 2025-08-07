#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# BGP performance of ingesting from 1 iBGP peer, data change counter
# is NOT used.This suite uses play.py as single iBGP peer.
# The suite only looks at example-ipv4-topology, so RIB is not
# examined. The suite consists of two halves, differing on which side
# initiates BGP connection. State of "work is being done" is detected
# by increasing value of prefixes in topology. The time for
# test_wait_for_stable_* cases to finish is the main performance
# metric.After waiting for stability is done, full check on
# number of prefixes present is performed. Brief description how to
# configure BGP peer can be found here:
# https://wiki.opendaylight.org/view/BGP_LS_PCEP:User_Guide#BGP_Peer
# http://docs.opendaylight.org/en/stable-boron/user-guide/bgp-user-guide.html#bgp-peering

import logging
import os
import pytest

from lib import bgp
from lib import infra
from lib import ip_topology
from lib import utils


PREFIXES_COUNT = 600_000
TEST_DURATION_MULTIPLIER = int(os.environ["TEST_DURATION_MULTIPLIER"])
BGP_FILLING_TIMEOUT = TEST_DURATION_MULTIPLIER * (PREFIXES_COUNT * 9.0 / 10000 + 20)
BGP_EMPTYING_TIMEOUT = BGP_FILLING_TIMEOUT * 3 / 4

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=2)
class TestSinglePeerPrefixCount:
    bgp_speaker_process = None

    def test_single_peer_prefix_count(self, allure_step_with_separate_logging):

        with allure_step_with_separate_logging("step_check_for_empty_topology_before_talking"):
            """Wait for example-ipv4-topology to come up and empty. Give large
            timeout for case when BGP boots slower than restconf."""
            topology_count = utils.wait_until_function_returns_value(
                120, 1, 0, ip_topology.get_ipv4_topology_prefixes_count
            )
            assert (
                topology_count == 0
            ), f"Ipv4 topology is not empty as expected, there are {topology_count} prefixes present."

        with allure_step_with_separate_logging("step_reconfigure_odl_to_accept_connection"):
            """Configure BGP peer module with initiate-connection set to false."""
            bgp.set_bgp_neighbour(ip="127.0.0.1", passive_mode=True)

        with allure_step_with_separate_logging("step_start_talking_bgp_speaker"):
            """Start Python speaker to connect to ODL."""
            TestSinglePeerPrefixCount.bgp_speaker_process = bgp.start_bgp_speaker(
                ammount=PREFIXES_COUNT,
                insert=1,
                withdraw=0,
                prefill=0,
                update="single",
                listen=False,
                log_level="info",
            )
            assert infra.is_process_still_running(
                TestSinglePeerPrefixCount.bgp_speaker_process
            ), "Bgp speaker process is not running"

        with allure_step_with_separate_logging("step_wait_for_stable_talking_ip_topology"):
            """Wait until example-ipv4-topology becomes stable. This is done by
            checking stability of prefix count."""
            ip_topology.wait_for_ipv4_topology_prefixes_to_become_stable(
                excluded_value=0, timeout=BGP_FILLING_TIMEOUT
            )

        with allure_step_with_separate_logging("step_check_talking_ip_topology_count"):
            """Count the routes in example-ipv4-topology and fail if the count is
            not correct."""
            topology_count = ip_topology.get_ipv4_topology_prefixes_count()
            assert (
                topology_count == PREFIXES_COUNT
            ), f"Ipv4 topology does not contain all {PREFIXES_COUNT} expected advertised prefixes, but only {topology_count}"

        with allure_step_with_separate_logging("step_kill_talking_bgp_speaker"):
            """Abort the Python speaker."""
            bgp.stop_bgp_speaker(self.bgp_speaker_process)

        with allure_step_with_separate_logging("step_store_result_for_talking_bgp_speaker"):
            """Store results for debugging."""
            infra.shell(
                "mv totals-bgp.csv results/prefixcount-talking-totals-bgp.csv",
                check_rc=True,
            )
            infra.shell(
                "mv performance-bgp.csv results/prefixcount-talking-performance-bgp.csv",
                check_rc=True,
            )

        with allure_step_with_separate_logging("step_wait_for_stable_ip_topology_after_talking"):
            """Wait until example-ipv4-topology becomes stable again."""
            ip_topology.wait_for_ipv4_topology_prefixes_to_become_stable(
                excluded_value=PREFIXES_COUNT, timeout=BGP_EMPTYING_TIMEOUT
            )

        with allure_step_with_separate_logging("step_check_for_empty_ip_topology_after_talking"):
            topology_count = ip_topology.get_ipv4_topology_prefixes_count()
            assert (
                topology_count == 0
            ), f"Ipv4 topology is not empty as expected, there are {topology_count} prefixes present."

        with allure_step_with_separate_logging("step_start_listening_bgp_speaker"):
            """Start Python speaker in listening mode."""
            TestSinglePeerPrefixCount.bgp_speaker_process = bgp.start_bgp_speaker(
                ammount=PREFIXES_COUNT,
                insert=1,
                withdraw=0,
                prefill=0,
                update="single",
                listen=True,
                log_level="info",
            )
            assert infra.is_process_still_running(
                TestSinglePeerPrefixCount.bgp_speaker_process
            ), "Bgp speaker process is not running"

        with allure_step_with_separate_logging("step_reconfigure_odl_to_initiate_connection"):
            """Replace BGP peer config module, now with initiate-connection set to
            true."""
            bgp.set_bgp_neighbour(ip="127.0.0.1", passive_mode=False)

        with allure_step_with_separate_logging("step_wait_for_stable_listening_ip_topology"):
            """Wait until example-ipv4-topology becomes stable."""
            ip_topology.wait_for_ipv4_topology_prefixes_to_become_stable(
                excluded_value=0, timeout=BGP_FILLING_TIMEOUT
            )

        with allure_step_with_separate_logging("step_check_listening_ip_topology_count"):
            """Count the routes in example-ipv4-topology and fail if the count is
            not correct."""
            topology_count = ip_topology.get_ipv4_topology_prefixes_count()
            assert (
                topology_count == PREFIXES_COUNT
            ), f"Ipv4 topology does not contain all {PREFIXES_COUNT} expected advertised prefixes, but only {topology_count}"

        with allure_step_with_separate_logging("step_kill_listening_bgp_speaker"):
            """Abort the Python speaker."""
            bgp.stop_bgp_speaker(self.bgp_speaker_process)

        with allure_step_with_separate_logging("step_store_result_for_listening_bgp_speaker"):
            """Store results for debugging."""
            infra.shell(
                "mv totals-bgp.csv results/prefixcount-listening-totals-bgp.csv",
                check_rc=True,
            )
            infra.shell(
                "mv performance-bgp.csv results/prefixcount-listening-performance-bgp.csv",
                check_rc=True,
            )

        with allure_step_with_separate_logging("step_wait_for_stable_ip_topology_after_listening"):
            """Wait until example-ipv4-topology becomes stable again."""
            ip_topology.wait_for_ipv4_topology_prefixes_to_become_stable(
                excluded_value=PREFIXES_COUNT, timeout=BGP_EMPTYING_TIMEOUT
            )

        with allure_step_with_separate_logging("step_check_for_empty_ip_topology_after_listening"):
            """Example-ipv4-topology should be empty now."""
            topology_count = ip_topology.get_ipv4_topology_prefixes_count()
            assert (
                topology_count == 0
            ), f"Ipv4 topology is not empty as expected, there are {topology_count} prefixes present."

        with allure_step_with_separate_logging("step_delete_bgp_peer_configuartion"):
            """Revert the BGP configuration to the original state: without any
            configured peers."""
            bgp.delete_bgp_neighbour(ip="127.0.0.1")
