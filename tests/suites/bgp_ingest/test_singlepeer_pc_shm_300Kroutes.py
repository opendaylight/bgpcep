#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
#
# BGP performance of ingesting from 1 iBGP peer, data change
# counter is NOT used.This suite uses play.py as single iBGP peer
# which talks to single controller.Test suite checks changes
# of the the example-ipv4-topology on all nodes. RIB is not examined.
# test_singlepeer_pc_300kroutes: pc - prefix counting

import logging
import pytest

from libraries import bgp
from libraries import prefix_counting
from libraries import utils
from libraries.variables import variables


PREFIXES_COUNT = 300_000

TEST_DURATION_MULTIPLIER = variables.TEST_DURATION_MULTIPLIER
BGP_FILLING_TIMEOUT = TEST_DURATION_MULTIPLIER * (PREFIXES_COUNT * 6.0 / 10000 + 35)
BGP_EMPTYING_TIMEOUT = BGP_FILLING_TIMEOUT * 3 / 4
CHECK_PERIOD = 5
REPETITIONS = 4
ODL_IP = variables.ODL_IP
TOOLS_IP = variables.TOOLS_IP
BGP_TOOL_PORT = variables.BGP_TOOL_PORT
ODL_BGP_PORT = variables.ODL_BGP_PORT
HOLDTIME = 180
INSERT = 1
PREFILL = 0
WITHDRAW = 0
RESULTS_FILE_NAME = "bgp.csv"
UPDATE = "single"
RIB_INSTANCE = "example-bgp-rib"
BGP_TOOL_LOG_LEVEL = "info"
EXAMPLE_IPV4_TOPOLOGY = "example-ipv4-topology"

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=1)
class TestSinglePeer300KRoutes:
    bgp_speaker_process = None

    def test_single_peer_300K_routes(self, allure_step_with_separate_logging):

        with allure_step_with_separate_logging(
            "step_check_for_empty_topology_before_talking"
        ):
            """Wait for example-ipv4-topology to come up and empty.
            Give large timeout for case when BGP boots slower than restconf."""
            utils.wait_until_function_pass(120, 1, prefix_counting.check_ipv4_topology_is_empty, EXAMPLE_IPV4_TOPOLOGY)

        with allure_step_with_separate_logging(
            "step_reconfigure_odl_to_accept_connection"
        ):
            """Configure BGP peer module with initiate-connection set to false."""
            bgp.set_bgp_neighbour(ip=TOOLS_IP, holdtime=HOLDTIME, rib_instance=RIB_INSTANCE, passive_mode=True)

        with allure_step_with_separate_logging("step_start_talking_bgp_speaker"):
            """Start Python speaker to connect to ODL."""
            TestSinglePeer300KRoutes.bgp_speaker_process = (
                bgp.start_bgp_speaker_with_verify_and_retry(
                    speaker_ip=TOOLS_IP,
                    my_ip=TOOLS_IP,
                    my_port=BGP_TOOL_PORT,
                    peer_ip=ODL_IP,
                    peer_port=ODL_BGP_PORT,
                    ammount=PREFIXES_COUNT,
                    insert=INSERT,
                    withdraw=WITHDRAW,
                    prefill=PREFILL,
                    update=UPDATE,
                    listen=False,
                    log_level=BGP_TOOL_LOG_LEVEL,
                )
            )

        with allure_step_with_separate_logging(
            "step_wait_for_stable_talking_ip_topology"
        ):
            """Wait until example-ipv4-topology becomes stable. This is done by
            checking stability of prefix count as seen from all nodes."""
            prefix_counting.wait_for_ipv4_topology_prefixes_to_become_stable(
                excluded_value=0, timeout=BGP_FILLING_TIMEOUT, wait_period=CHECK_PERIOD, consecutive_times_stable_value=REPETITIONS
            )

        with allure_step_with_separate_logging("step_check_talking_ip_topology_count"):
            """Count the routes in example-ipv4-topology and fail if the count is
            not correct."""
            prefix_counting.check_ipv4_topology_prefixes_count(PREFIXES_COUNT, topology=EXAMPLE_IPV4_TOPOLOGY)

        with allure_step_with_separate_logging("step_kill_talking_bgp_speaker"):
            """Abort the Python speaker."""
            bgp.stop_bgp_speaker(self.bgp_speaker_process)

        with allure_step_with_separate_logging(
            "step_wait_for_stable_ip_topology_after_listening"
        ):
            """Wait until example-ipv4-topology becomes stable again."""
            prefix_counting.wait_for_ipv4_topology_prefixes_to_become_stable(
                excluded_value=PREFIXES_COUNT,
                timeout=BGP_FILLING_TIMEOUT,
                wait_period=CHECK_PERIOD,
                consecutive_times_stable_value=REPETITIONS
            )

        with allure_step_with_separate_logging(
            "step_check_for_empty_ip_topology_after_listening"
        ):
            """Example-ipv4-topology should be empty."""
            prefix_counting.check_ipv4_topology_is_empty(EXAMPLE_IPV4_TOPOLOGY)

        with allure_step_with_separate_logging("step_delete_bgp_peer_configuration"):
            """Revert the BGP configuration to the original state: without any
            configured peers."""
            bgp.delete_bgp_neighbour(ip=TOOLS_IP, rib_instance=RIB_INSTANCE)
