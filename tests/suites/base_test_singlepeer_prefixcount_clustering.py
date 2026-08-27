#
# Copyright (c) 2025 PANTHEON.tech, s.r.o.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# Based on the original Robot Framework integration tests:
# https://github.com/opendaylight/integration-test/blob/901c7e139945b436d95a44b3b592904c3d7a4f9f/csit/suites/bgpcep/bgpclustering/010_singlepeer_prefixcount_1route.robot
# https://github.com/opendaylight/integration-test/blob/901c7e139945b436d95a44b3b592904c3d7a4f9f/csit/suites/bgpcep/bgpclustering/singlepeer_pc_shm_300kroutes.robot
#

import logging

import allure

from libraries import bgp
from libraries import prefix_counting
from libraries import utils
from libraries.variables import variables

TEST_DURATION_MULTIPLIER = variables.TEST_DURATION_MULTIPLIER
ODL_IP = variables.ODL_IP
TOOLS_IP = variables.TOOLS_IP
BGP_TOOL_PORT = variables.BGP_TOOL_PORT
ODL_BGP_PORT = variables.ODL_BGP_PORT
CHECK_PERIOD = 5
REPETITIONS = 4
INITIAL_RESTCONF_RETRIES = 120
INITIAL_RESTCONF_INTERVAL = 1
HOLDTIME = 180
INSERT = 1
PREFILL = 0
WITHDRAW = 0
UPDATE = "single"
RIB_INSTANCE = "example-bgp-rib"
BGP_TOOL_LOG_LEVEL = "info"
EXAMPLE_IPV4_TOPOLOGY = "example-ipv4-topology"

log = logging.getLogger(__name__)


class BaseTestSinglePeerPrefixCountClustering:
    """Shared flow of the bgpclustering single peer prefix counting suites.

    The suites of this family only differ in the number of prefixes the single
    iBGP peer advertises, so the whole flow is kept here and the concrete
    suites supply the prefix count via parametrization.
    """

    bgp_speaker_process = None

    def test_single_peer_prefix_count_clustering(
        self,
        allure_step_with_separate_logging,
        prefixes_count,
    ):
        test_description = getattr(self, "test_description", None)
        if test_description:
            allure.dynamic.description(test_description)

        bgp_filling_timeout = TEST_DURATION_MULTIPLIER * (
            prefixes_count * 6.0 / 10_000 + 35
        )

        with allure_step_with_separate_logging(
            "step_check_for_empty_topology_before_talking"
        ):
            # Wait for example-ipv4-topology to come up and empty.
            # Give large timeout for case when BGP boots slower than restconf.
            utils.wait_until_function_pass(
                INITIAL_RESTCONF_RETRIES,
                INITIAL_RESTCONF_INTERVAL,
                prefix_counting.check_ipv4_topology_is_empty,
                EXAMPLE_IPV4_TOPOLOGY,
            )

        with allure_step_with_separate_logging(
            "step_reconfigure_odl_to_accept_connection"
        ):
            # Configure BGP peer module with initiate-connection set to false.
            bgp.set_bgp_neighbour(
                ip=TOOLS_IP,
                holdtime=HOLDTIME,
                peer_port=BGP_TOOL_PORT,
                rib_instance=RIB_INSTANCE,
                passive_mode=True,
            )

        with allure_step_with_separate_logging("step_start_talking_bgp_speaker"):
            # Start Python speaker to connect to ODL.
            self.bgp_speaker_process = bgp.start_bgp_speaker_with_verify_and_retry(
                speaker_ips=TOOLS_IP,
                my_ip=TOOLS_IP,
                my_port=BGP_TOOL_PORT,
                peer_ip=ODL_IP,
                peer_port=ODL_BGP_PORT,
                ammount=prefixes_count,
                insert=INSERT,
                withdraw=WITHDRAW,
                prefill=PREFILL,
                update=UPDATE,
                listen=False,
                log_level=BGP_TOOL_LOG_LEVEL,
            )

        with allure_step_with_separate_logging(
            "step_wait_for_stable_talking_ip_topology"
        ):
            # Wait until example-ipv4-topology becomes stable. This is done by
            # checking stability of prefix count.
            prefix_counting.wait_for_ipv4_topology_prefixes_to_become_stable(
                excluded_value=0,
                timeout=bgp_filling_timeout,
                wait_period=CHECK_PERIOD,
                consecutive_times_stable_value=REPETITIONS,
                topology=EXAMPLE_IPV4_TOPOLOGY,
            )

        with allure_step_with_separate_logging("step_check_talking_ip_topology_count"):
            # Count the routes in example-ipv4-topology and fail if the count is
            # not correct.
            prefix_counting.check_ipv4_topology_prefixes_count(
                prefixes_count, topology=EXAMPLE_IPV4_TOPOLOGY
            )

        with allure_step_with_separate_logging("step_kill_talking_bgp_speaker"):
            # Abort the Python speaker.
            bgp.stop_bgp_speaker(self.bgp_speaker_process)

        with allure_step_with_separate_logging(
            "step_wait_for_stable_ip_topology_after_listening"
        ):
            # Wait until example-ipv4-topology becomes stable again.
            prefix_counting.wait_for_ipv4_topology_prefixes_to_become_stable(
                excluded_value=prefixes_count,
                timeout=bgp_filling_timeout,
                wait_period=CHECK_PERIOD,
                consecutive_times_stable_value=REPETITIONS,
                topology=EXAMPLE_IPV4_TOPOLOGY,
            )

        with allure_step_with_separate_logging(
            "step_check_for_empty_ip_topology_after_listening"
        ):
            # Example-ipv4-topology should be empty.
            prefix_counting.check_ipv4_topology_is_empty(EXAMPLE_IPV4_TOPOLOGY)

        with allure_step_with_separate_logging("step_delete_bgp_peer_configuration"):
            # Revert the BGP configuration to the original state: without any
            # configured peers.
            bgp.delete_bgp_neighbour(ip=TOOLS_IP, rib_instance=RIB_INSTANCE)
