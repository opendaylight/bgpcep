#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
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
BGP_TOOL_PORT = variables.BGP_TOOL_PORT
ODL_BGP_PORT = variables.ODL_BGP_PORT
KARAF_LOG_LEVEL = variables.KARAF_LOG_LEVEL
FIRST_PEER_IP = TOOLS_IP
TEST_DURATION_MULTIPLIER = variables.TEST_DURATION_MULTIPLIER
CHECK_PERIOD_PREFIX_COUNT_MANY = 20
REPETITIONS_PREFIX_COUNT_MANY = 4

BGP_TOOL_LOG_LEVEL = "info"
KARAF_LOG_LEVEL = "INFO"
KARAF_BGPCEP_LOG_LEVEL = KARAF_LOG_LEVEL
KARAF_PROTOCOL_LOG_LEVEL = KARAF_BGPCEP_LOG_LEVEL
HOLDTIME = 180
RESULTS_FILE_NAME = "bgp.csv"
UPDATE = "single"
RIB_INSTANCE = "example-bgp-rib"


log = logging.getLogger(__name__)


class BaseTestManyPeerPrefixCount:
    bgp_speaker_process = None

    def test_many_peers_prefix_count(
        self,
        allure_step_with_separate_logging,
        bgp_peers_count,
        count_prefix_count_many,
        insert,
        withdraw,
        prefill,
    ):
        test_description = getattr(self, "test_description", None)
        if test_description:
            allure.dynamic.description(test_description)

        bgp_filling_timeout = TEST_DURATION_MULTIPLIER * (
            count_prefix_count_many * 4.0 / 10000 + 80
        )
        bgp_emptying_timeout = bgp_filling_timeout * 3 / 4

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
                first_neighbour_ip=FIRST_PEER_IP,
                count=bgp_peers_count,
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
            self.bgp_speaker_process = bgp.start_bgp_speaker(
                ammount=count_prefix_count_many,
                multiplicity=bgp_peers_count,
                my_ip=TOOLS_IP,
                my_port=BGP_TOOL_PORT,
                peer_ip=ODL_IP,
                peer_port=ODL_BGP_PORT,
                insert=insert,
                withdraw=withdraw,
                prefill=prefill,
                update=UPDATE,
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
                timeout=bgp_filling_timeout,
            )

        with allure_step_with_separate_logging("step_check_talking_ip_topology_count"):
            """Count the routes in example-ipv4-topology and fail if the count is
            not correct."""
            prefix_counting.check_ipv4_topology_prefixes_count(count_prefix_count_many)

        with allure_step_with_separate_logging("step_kill_talking_bgp_speakers"):
            """Abort the Python speakers. Also, attempt to stop failing fast."""
            bgp.stop_bgp_speaker(self.bgp_speaker_process)

        with allure_step_with_separate_logging(
            "step_wait_for_stable_ip_topology_after_talking"
        ):
            """Wait until example-ipv4-topology becomes stable again."""
            prefix_counting.wait_for_ipv4_topology_prefixes_to_become_stable(
                excluded_value=count_prefix_count_many,
                wait_period=CHECK_PERIOD_PREFIX_COUNT_MANY,
                consecutive_times_stable_value=REPETITIONS_PREFIX_COUNT_MANY,
                timeout=bgp_emptying_timeout,
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
                first_neighbour_ip=FIRST_PEER_IP,
                count=bgp_peers_count,
                rib_instance=RIB_INSTANCE,
            )
