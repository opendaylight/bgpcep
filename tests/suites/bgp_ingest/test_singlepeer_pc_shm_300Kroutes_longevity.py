#
# Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# Based on the original Robot Framework integration test:
# https://github.com/opendaylight/integration-test/blob/901c7e139945b436d95a44b3b592904c3d7a4f9f/csit/suites/bgpcep/bgpclustering/singlepeer_pc_shm_300kroutes_longevity.robot
#

import logging
import textwrap

import allure
import pytest

from libraries import bgp
from libraries import prefix_counting
from libraries import utils
from libraries.variables import variables
from suites.base_test_singlepeer_prefixcount_clustering import (
    EXAMPLE_IPV4_TOPOLOGY,
    HOLDTIME,
    INITIAL_RESTCONF_INTERVAL,
    INITIAL_RESTCONF_RETRIES,
    RIB_INSTANCE,
    TEST_DURATION_MULTIPLIER,
    TOOLS_IP,
    run_ingest_cycle,
)
from suites.suite_order import SuiteOrder


PREFIXES_COUNT = 300_000
LONGEVITY_TEST_DURATION_IN_SECS = variables.LONGEVITY_TEST_DURATION_IN_SECS
BGP_TOOL_PORT = variables.BGP_TOOL_PORT
# Robot waits a second between scenario repetitions.
SCENARIO_INTERVAL = 1

log = logging.getLogger(__name__)


@pytest.mark.bgp
@pytest.mark.ibgp
@pytest.mark.longevity
@pytest.mark.performance
@pytest.mark.single_device
@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=SuiteOrder.BGP_INGEST_PC_SHM_300K_LONGEVITY)
class TestSinglePeer300KRoutesLongevity:

    @allure.description(
        textwrap.dedent(
            """
            **BGP longevity soak of ingesting from 1 iBGP peer**

            Data change counter is NOT used. This suite uses play.py as single \
            iBGP peer which talks to single controller. Test suite checks \
            changes of the example-ipv4-topology. RIB is not examined.

            The BGP peer is configured once, then the advertise and withdraw \
            scenario is repeated for the whole configured duration, failing as \
            soon as any repetition does. The peer configuration is removed at \
            the end.

            singlepeer_pc_shm_300kroutes_longevity: pc - prefix counting, \
            shm - shard monitoring.
        """
        )
    )
    def test_single_peer_300K_routes_longevity(
        self, allure_step_with_separate_logging
    ):
        bgp_filling_timeout = TEST_DURATION_MULTIPLIER * (
            PREFIXES_COUNT * 6.0 / 10_000 + 35
        )

        with allure_step_with_separate_logging(
            "step_check_for_empty_topology_before_talking"
        ):
            # Wait for example-ipv4-topology to come up and empty. Give large
            # timeout for case when BGP boots slower than restconf.
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
            # The peer stays configured for every repetition below.
            bgp.set_bgp_neighbour(
                ip=TOOLS_IP,
                holdtime=HOLDTIME,
                peer_port=BGP_TOOL_PORT,
                rib_instance=RIB_INSTANCE,
                passive_mode=True,
            )

        try:
            with allure_step_with_separate_logging("step_repeat_scenario_longevity"):
                # Advertise and withdraw repeatedly for the whole duration,
                # failing as soon as any repetition fails.
                log.info(
                    f"Repeating the ingest scenario for "
                    f"{LONGEVITY_TEST_DURATION_IN_SECS}s"
                )
                utils.verify_function_does_not_fail_for_duration(
                    LONGEVITY_TEST_DURATION_IN_SECS,
                    SCENARIO_INTERVAL,
                    run_ingest_cycle,
                    allure_step_with_separate_logging,
                    PREFIXES_COUNT,
                    bgp_filling_timeout,
                )
        finally:
            with allure_step_with_separate_logging(
                "step_delete_bgp_peer_configuration"
            ):
                # Revert the BGP configuration to the original state: without
                # any configured peers. Done even when a repetition failed, so
                # a failed soak does not leave the peer behind.
                bgp.delete_bgp_neighbour(ip=TOOLS_IP, rib_instance=RIB_INSTANCE)
