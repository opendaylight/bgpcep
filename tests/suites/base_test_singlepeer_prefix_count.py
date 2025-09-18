#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# This suite uses play.py as single iBGP peer.
# The suite only looks at example-ipv4-topology, so RIB is not examined.
#
# The suite consists of two halves, differing on which side initiates BGP
# connection. State of "work is being done" is detected by increasing
# value of prefixes in topology. The time for wait_for_*to_become_stable
# cases to finish is the main performance metric. After waiting for stability
# is done, full check on number of prefixes present is performed.
#
# Brief description how to configure BGP peer can be found here:
# https://wiki.opendaylight.org/view/BGP_LS_PCEP:User_Guide#BGP_Peer
# http://docs.opendaylight.org/en/stable-boron/user-guide/bgp-user-guide.html#bgp-peering
#
# TODO: Currently, if a bug causes prefix count to remain at zero,
# affected test cases will wait for max time. Reconsider.
# If zero is allowed as stable, higher period or repetitions would be required.
#
# The prefix counting is quite heavyweight and may induce large variation in time.
# Try the other version of the suite (test_singlepeer_change_count.py) to get better precision.

import logging

from libraries import bgp
from libraries import infra
from libraries import prefix_counting
from libraries import utils
from libraries.variables import variables

TEST_DURATION_MULTIPLIER = variables.TEST_DURATION_MULTIPLIER
CHECK_PERIOD_PREFIX_COUNT_SINGLE = 10
REPETITIONS_PREFIX_COUNT_SINGLE = 4
ODL_IP = variables.ODL_IP
TOOLS_IP = variables.TOOLS_IP
BGP_TOOL_PORT = variables.BGP_TOOL_PORT
ODL_BGP_PORT = variables.ODL_BGP_PORT
BGP_TOOL_LOG_LEVEL = "info"
HOLDTIME = 180
RESULTS_FILE_NAME = "bgp.csv"
UPDATE = "single"
RIB_INSTANCE = "example-bgp-rib"

log = logging.getLogger(__name__)


class BaseTestSinglePeerPrefixCount:
    bgp_speaker_process = None

    def test_single_peer_prefix_count(
        self,
        allure_step_with_separate_logging,
        count_prefix_count_single,
        insert,
        withdraw,
        prefill,
    ):
        bgp_filling_timeout = TEST_DURATION_MULTIPLIER * (
            count_prefix_count_single * 9.0 / 10000 + 20
        )
        bgp_emptying_timeout = bgp_filling_timeout * 3 / 4

        with allure_step_with_separate_logging(
            "step_check_for_empty_topology_before_talking"
        ):
            """Wait for example-ipv4-topology to come up and empty. Give large
            timeout for case when BGP boots slower than restconf."""
            # TODO: Choose which tags to assign and make sure they are assigned
            # correctly.
            utils.wait_until_function_pass(
                120, 1, prefix_counting.check_ipv4_topology_is_empty
            )

        with allure_step_with_separate_logging(
            "step_reconfigure_odl_to_accept_connection"
        ):
            """Configure BGP peer module with initiate-connection set to false."""
            bgp.set_bgp_neighbour(
                ip=TOOLS_IP,
                holdtime=HOLDTIME,
                peer_port=BGP_TOOL_PORT,
                rib_instance=RIB_INSTANCE,
                passive_mode=True,
            )

        with allure_step_with_separate_logging("step_start_talking_bgp_speaker"):
            """Start Python speaker to connect to ODL."""
            self.bgp_speaker_process = bgp.start_bgp_speaker(
                ammount=count_prefix_count_single,
                my_ip=TOOLS_IP,
                my_port=BGP_TOOL_PORT,
                peer_ip=ODL_IP,
                peer_port=ODL_BGP_PORT,
                insert=insert,
                withdraw=withdraw,
                prefill=prefill,
                update=UPDATE,
                log_level=BGP_TOOL_LOG_LEVEL,
            )

        with allure_step_with_separate_logging(
            "step_wait_for_stable_talking_ip_topology"
        ):
            """Wait until example-ipv4-topology becomes stable. This is done by
            checking stability of prefix count."""
            prefix_counting.wait_for_ipv4_topology_prefixes_to_become_stable(
                excluded_value=0,
                timeout=bgp_filling_timeout,
                wait_period=CHECK_PERIOD_PREFIX_COUNT_SINGLE,
                consecutive_times_stable_value=REPETITIONS_PREFIX_COUNT_SINGLE,
            )

        with allure_step_with_separate_logging("step_check_talking_ip_topology_count"):
            """Count the routes in example-ipv4-topology and fail if the count is
            not correct."""
            prefix_counting.check_ipv4_topology_prefixes_count(
                count_prefix_count_single
            )

        with allure_step_with_separate_logging("step_kill_talking_bgp_speaker"):
            """Abort the Python speaker."""
            bgp.stop_bgp_speaker(self.bgp_speaker_process)

        with allure_step_with_separate_logging(
            "step_store_result_for_talking_bgp_speaker"
        ):
            """Store results for debugging."""
            infra.backup_file(
                f"totals-{RESULTS_FILE_NAME}",
                target_file_name=f"prefixcount-talking-totals-${RESULTS_FILE_NAME}",
                src_dir=".",
            )
            infra.backup_file(
                f"performance-{RESULTS_FILE_NAME}",
                target_file_name=f"prefixcount-talking-performance-{RESULTS_FILE_NAME}",
                src_dir=".",
            )

        with allure_step_with_separate_logging(
            "step_wait_for_stable_ip_topology_after_talking"
        ):
            """Wait until example-ipv4-topology becomes stable again."""
            # TODO: Is is possible to have failed at Check_Talking_Ipv4_Topology_Count and still have initial period of constant count?
            # FIXME: If yes, do count here to get the initial value and use it (if nonzero).
            # TODO: If yes, decide whether access to the FailFast state should have keyword or just variable name.
            prefix_counting.wait_for_ipv4_topology_prefixes_to_become_stable(
                excluded_value=count_prefix_count_single,
                timeout=bgp_emptying_timeout,
                wait_period=CHECK_PERIOD_PREFIX_COUNT_SINGLE,
                consecutive_times_stable_value=REPETITIONS_PREFIX_COUNT_SINGLE,
            )

        with allure_step_with_separate_logging(
            "step_check_for_empty_ip_topology_after_talking"
        ):
            prefix_counting.check_ipv4_topology_is_empty()

        with allure_step_with_separate_logging("step_start_listening_bgp_speaker"):
            """Start Python speaker in listening mode."""
            self.bgp_speaker_process = bgp.start_bgp_speaker(
                ammount=count_prefix_count_single,
                my_ip=TOOLS_IP,
                my_port=BGP_TOOL_PORT,
                peer_ip=ODL_IP,
                peer_port=ODL_BGP_PORT,
                insert=insert,
                withdraw=withdraw,
                prefill=prefill,
                update=UPDATE,
                listen=True,
                log_level=BGP_TOOL_LOG_LEVEL,
            )

        with allure_step_with_separate_logging(
            "step_reconfigure_odl_to_initiate_connection"
        ):
            """Replace BGP peer config module, now with initiate-connection set to
            true."""
            bgp.set_bgp_neighbour(
                ip=TOOLS_IP,
                holdtime=HOLDTIME,
                peer_port=BGP_TOOL_PORT,
                rib_instance=RIB_INSTANCE,
                passive_mode=False,
            )

        with allure_step_with_separate_logging(
            "step_wait_for_stable_listening_ip_topology"
        ):
            """Wait until example-ipv4-topology becomes stable."""
            prefix_counting.wait_for_ipv4_topology_prefixes_to_become_stable(
                excluded_value=0,
                timeout=bgp_filling_timeout,
                wait_period=CHECK_PERIOD_PREFIX_COUNT_SINGLE,
                consecutive_times_stable_value=REPETITIONS_PREFIX_COUNT_SINGLE,
            )

        with allure_step_with_separate_logging(
            "step_check_listening_ip_topology_count"
        ):
            """Count the routes in example-ipv4-topology and fail if the count is
            not correct."""
            prefix_counting.check_ipv4_topology_prefixes_count(
                count_prefix_count_single
            )

        with allure_step_with_separate_logging("step_kill_listening_bgp_speaker"):
            """Abort the Python speaker."""
            bgp.stop_bgp_speaker(self.bgp_speaker_process)

        with allure_step_with_separate_logging(
            "step_store_result_for_listening_bgp_speaker"
        ):
            """Store results for debugging."""
            infra.backup_file(
                f"totals-{RESULTS_FILE_NAME}",
                target_file_name=f"prefixcount-listening-totals-${RESULTS_FILE_NAME}",
                src_dir=".",
            )
            infra.backup_file(
                f"performance-{RESULTS_FILE_NAME}",
                target_file_name=f"prefixcount-listening-performance-{RESULTS_FILE_NAME}",
                src_dir=".",
            )

        with allure_step_with_separate_logging(
            "step_wait_for_stable_ip_topology_after_listening"
        ):
            """Wait until example-ipv4-topology becomes stable again."""
            prefix_counting.wait_for_ipv4_topology_prefixes_to_become_stable(
                excluded_value=count_prefix_count_single,
                timeout=bgp_emptying_timeout,
                wait_period=CHECK_PERIOD_PREFIX_COUNT_SINGLE,
                consecutive_times_stable_value=REPETITIONS_PREFIX_COUNT_SINGLE,
            )

        with allure_step_with_separate_logging(
            "step_check_for_empty_ip_topology_after_listening"
        ):
            """Example-ipv4-topology should be empty now."""
            prefix_counting.check_ipv4_topology_is_empty()

        with allure_step_with_separate_logging("step_delete_bgp_peer_configuration"):
            """Revert the BGP configuration to the original state: without any
            configured peers."""
            bgp.delete_bgp_neighbour(ip=TOOLS_IP, rib_instance=RIB_INSTANCE)
