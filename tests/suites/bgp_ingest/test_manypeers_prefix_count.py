#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# This suite uses play.py processes as iBGP peers.
# This is analogue of single peer performance suite, which uses many peers.
# Each peer is of ibgp type, and they contribute to the same example-bgp-rib,
# and thus to the same single example-ipv4-topology.
# The suite only looks at example-ipv4-topology, so RIB is not examined.
#
# The suite consists of two halves, differing on which side initiates BGP connection.
# State of "work is being done" is detected by increasing value of prefixes in topology.
# The time for wait_for_*to_become_stable cases to finish is the main performance metric.
# After waiting for stability is done, full check on number of prefixes present is performed.
#
# TODO: Currently, if a bug causes prefix count to remain at zero,
# affected test cases will wait for max time. Reconsider.
# If zero is allowed as stable, higher period or repetitions would be required.
#
# The prefix counting is quite heavyweight and may induce large variation in time.
# Try the other version of the suite (test_manypeers_change_count.py) to get better precision.
#
# ODL distinguishes peers by their IP addresses.
# Currently, this suite requires python utils to be started on ODL System,
# to guarantee IP address block is available for them to bind to.
# TODO: Figure out how to use Docker and docker IP pool available in RelEng.
#
# Currently, 127.0.0.1 is hardcoded as the first peer address to use.
# TODO: Figure out how to make it configurable.
# As peer IP adresses are set incrementally, we need ipaddr to be used in PyTest somehow.
#
# Brief description how to configure BGP peer can be found here:
# https://wiki.opendaylight.org/view/BGP_LS_PCEP:User_Guide#BGP_Peer
# http://docs.opendaylight.org/en/stable-boron/user-guide/bgp-user-guide.html#bgp-peering
#
# TODO: Is there a need for version of this suite where ODL connects to pers?
# Note that configuring ODL is slow, which may affect measured performance singificantly.
# Advanced TODO: Give manager ability to start pushing on trigger long after connections are established.

import logging
import os
import pytest

from libraries import bgp
from libraries import infra
from libraries import prefix_counting
from libraries import utils
from libraries.variables import variables


COUNT_PREFIX_COUNT_MANY = 600_000
BGP_PEERS_COUNT = 2
ODL_IP = variables.ODL_IP
TOOLS_IP = variables.TOOLS_IP
BGP_TOOL_PORT = variables.BGP_TOOL_PORT
ODL_BGP_PORT = variables.ODL_BGP_PORT
KARAF_LOG_LEVEL = variables.KARAF_LOG_LEVEL
FIRST_PEER_IP = TOOLS_IP
TEST_DURATION_MULTIPLIER = variables.TEST_DURATION_MULTIPLIER
BGP_FILLING_TIMEOUT = TEST_DURATION_MULTIPLIER * (
    COUNT_PREFIX_COUNT_MANY * 3.0 / 10000 + 20
)
BGP_EMPTYING_TIMEOUT = BGP_FILLING_TIMEOUT * 3 / 4
CHECK_PERIOD_PREFIX_COUNT_MANY = 20
REPETITIONS_PREFIX_COUNT_MANY = 4

BGP_TOOL_LOG_LEVEL = "info"
KARAF_LOG_LEVEL = "INFO"
KARAF_BGPCEP_LOG_LEVEL = KARAF_LOG_LEVEL
KARAF_PROTOCOL_LOG_LEVEL = KARAF_BGPCEP_LOG_LEVEL
HOLDTIME = 180
INSERT = 1
PREFILL = 0
WITHDRAW = 0
RESULTS_FILE_NAME = "bgp.csv"
UPDATE = "single"
RIB_INSTANCE = "example-bgp-rib"


log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=5)
class TestManyPeerPrefixCount:
    bgp_speaker_process = None

    def test_many_peers_prefix_count(self, allure_step_with_separate_logging):

        with allure_step_with_separate_logging(
            "step_check_for_empty_topology_before_talking"
        ):
            """Wait for example-ipv4-topology to come up and empty. Give large
            timeout for case when BGP boots slower than restconf."""
            utils.wait_until_function_pass(
                120, 1, prefix_counting.check_ipv4_topology_is_empty
            )

        with allure_step_with_separate_logging(
            "step_reconfigure_odl_to_accept_connections"
        ):
            """Configure BGP peers with initiate-connection set to false."""
            bgp.set_bgp_neighbours(
                first_neigbout_ip=FIRST_PEER_IP,
                count=BGP_PEERS_COUNT,
                holdtime=HOLDTIME,
                peer_port=BGP_TOOL_PORT,
                rib_instance=RIB_INSTANCE,
                passive_mode=True,
            )

        with allure_step_with_separate_logging("step_change_karaf_logging_levels"):
            """We may want to set more verbose logging here after configuration is
            done."""
            infra.execute_karaf_command(
                f"log:set {KARAF_BGPCEP_LOG_LEVEL} org.opendaylight.bgpcep"
            )
            infra.execute_karaf_command(
                f"log:set {KARAF_PROTOCOL_LOG_LEVEL} org.opendaylight.protocol"
            )

        with allure_step_with_separate_logging("step_start_talking_bgp_managers"):
            """Start Python manager to connect speakers to ODL."""
            TestManyPeerPrefixCount.bgp_speaker_process = bgp.start_bgp_speaker(
                ammount=COUNT_PREFIX_COUNT_MANY,
                multiplicity=BGP_PEERS_COUNT,
                my_ip=TOOLS_IP,
                my_port=BGP_TOOL_PORT,
                peer_ip=ODL_IP,
                peer_port=ODL_BGP_PORT,
                listen=False,
            )

        with allure_step_with_separate_logging(
            "step_wait_for_stable_talking_ip_topology"
        ):
            """Wait until example-ipv4-topology becomes stable. This is done by
            checking stability of prefix count."""
            prefix_counting.wait_for_ipv4_topology_prefixes_to_become_stable(
                excluded_value=0,
                wait_period=CHECK_PERIOD_PREFIX_COUNT_MANY,
                consecutive_times_stable_value=REPETITIONS_PREFIX_COUNT_MANY,
                timeout=BGP_FILLING_TIMEOUT,
            )

        with allure_step_with_separate_logging("step_check_talking_ip_topology_count"):
            """Count the routes in example-ipv4-topology and fail if the count is
            not correct."""
            prefix_counting.check_ipv4_topology_prefixes_count(COUNT_PREFIX_COUNT_MANY)

        with allure_step_with_separate_logging("step_kill_talking_bgp_speakers"):
            """Abort the Python speakers. Also, attempt to stop failing fast."""
            bgp.stop_bgp_speaker(self.bgp_speaker_process)

        with allure_step_with_separate_logging(
            "step_wait_for_stable_ip_topology_after_talking"
        ):
            """Wait until example-ipv4-topology becomes stable again."""
            prefix_counting.wait_for_ipv4_topology_prefixes_to_become_stable(
                excluded_value=COUNT_PREFIX_COUNT_MANY,
                wait_period=CHECK_PERIOD_PREFIX_COUNT_MANY,
                consecutive_times_stable_value=REPETITIONS_PREFIX_COUNT_MANY,
                timeout=BGP_EMPTYING_TIMEOUT,
            )

        with allure_step_with_separate_logging(
            "step_check_for_empty_ip_topology_after_talking"
        ):
            """Example-ipv4-topology should be empty now."""
            prefix_counting.check_ipv4_topology_is_empty()

        with allure_step_with_separate_logging("step_restore_karaf_logging_levels"):
            """Set logging on bgpcep and protocol to the global value."""
            infra.execute_karaf_command(
                f"log:set {KARAF_LOG_LEVEL} org.opendaylight.bgpcep"
            )
            infra.execute_karaf_command(
                f"log:set {KARAF_LOG_LEVEL} org.opendaylight.protocol"
            )

        with allure_step_with_separate_logging("step_delete_bgp_peers_configurations"):
            """Revert the BGP configuration to the original state: without any
            configured peers."""
            bgp.delete_bgp_neighbours(
                first_neigbout_ip=FIRST_PEER_IP,
                count=BGP_PEERS_COUNT,
                rib_instance=RIB_INSTANCE,
            )
