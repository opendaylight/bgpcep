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
# This suite requires odl-bgpcep-data-change-counter to be installed so
# make sure it is added to "install-features" of any jobs that are going
# to invoke it.
#
# The suite consists of two halves, differing on which side initiates BGP
# connection. Data change counter is a lightweight way to detect
# "work is being done". Utils library provide a nice function to wait for
# stability, but it needs initial value, that is why last_change_count_single
# appears just before work-inducing action.
# The time for wait_for_*to_become_stable cases to finish is the main
# performance metric.After waiting for stability is done, full check on
# number of prefixes present is performed.
#
# Brief description how to configure BGP peer can be found here:
# https://wiki.opendaylight.org/view/BGP_LS_PCEP:User_Guide#BGP_Peer
# http://docs.opendaylight.org/en/stable-boron/user-guide/bgp-user-guide.html#bgp-peering
#
# TODO: Currently, if a bug causes zero increase of data changes,
# affected test cases will wait for max time. Reconsider.
# if zero increase is allowed as stable, higher number of repetitions
# should be required.


import logging

from libraries import bgp
from libraries import change_counter
from libraries import infra
from libraries import prefix_counting
from libraries import utils
from libraries.variables import variables


TEST_DURATION_MULTIPLIER = variables.TEST_DURATION_MULTIPLIER
CHECK_PERIOD_CHANGE_COUNT_SINGLE = 10
REPETITIONS_CHANGE_COUNT_SINGLE = 4
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


class BaseTestSinglePeerChangeCount:
    bgp_speaker_process = None
    last_change_count_single = 1

    def store_change_count(self):
        """Get the count of changes from BGP change counter."""
        self.last_change_count_single = change_counter.get_change_count()

    def test_single_peer_change_count(
        self,
        allure_step_with_separate_logging,
        count_change_count_single,
        insert,
        withdraw,
        prefill,
    ):

        bgp_filling_timeout = TEST_DURATION_MULTIPLIER * (
            count_change_count_single * 9.0 / 10000 + 20
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

        with allure_step_with_separate_logging("step_recofigure_data_change_counter"):
            """Configure data change counter to count transactions in
            example-ipv4-topology instead of example-linkstate-topology."""
            change_counter.set_change_counter(topology_name="example-ipv4-topology")

        with allure_step_with_separate_logging("step_check_data_change_counter_ready"):
            """Data change counter might have been slower to start than ipv4
            topology, wait for it."""
            utils.wait_until_function_pass(5, 1, change_counter.get_change_count)

        with allure_step_with_separate_logging("step_start_talking_bgp_speaker"):
            """Start Python speaker to connect to ODL."""
            self.store_change_count()
            self.bgp_speaker_process = bgp.start_bgp_speaker(
                ammount=count_change_count_single,
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
            checking the change counter."""
            change_counter.wait_for_change_count_to_become_stable(
                minimum_value=self.last_change_count_single + 1,
                timeout=bgp_filling_timeout,
                wait_period=CHECK_PERIOD_CHANGE_COUNT_SINGLE,
                consecutive_times_stable_value=REPETITIONS_CHANGE_COUNT_SINGLE,
            )

        with allure_step_with_separate_logging("step_check_talking_ip_topology_count"):
            """Count the routes in example-ipv4-topology and fail if the count is
            not correct."""
            prefix_counting.check_ipv4_topology_prefixes_count(
                count_change_count_single
            )

        with allure_step_with_separate_logging("step_kill_talking_bgp_speaker"):
            """Abort the Python speaker."""
            self.store_change_count()
            bgp.stop_bgp_speaker(self.bgp_speaker_process)

        with allure_step_with_separate_logging(
            "step_store_result_for_talking_bgp_speaker"
        ):
            """Store results for plotting."""
            infra.backup_file(
                f"totals-{RESULTS_FILE_NAME}",
                target_file_name=f"changecount-talking-totals-${RESULTS_FILE_NAME}",
                src_dir=".",
            )
            infra.backup_file(
                f"performance-{RESULTS_FILE_NAME}",
                target_file_name=f"changecount-talking-performance-{RESULTS_FILE_NAME}",
                src_dir=".",
            )

        with allure_step_with_separate_logging(
            "step_wait_for_stable_ip_topology_after_talking"
        ):
            """Wait until example-ipv4-topology becomes stable again."""
            change_counter.wait_for_change_count_to_become_stable(
                minimum_value=self.last_change_count_single + 1,
                timeout=bgp_emptying_timeout,
                wait_period=CHECK_PERIOD_CHANGE_COUNT_SINGLE,
                consecutive_times_stable_value=REPETITIONS_CHANGE_COUNT_SINGLE,
            )

        with allure_step_with_separate_logging(
            "step_check_for_empty_ip_topology_after_talking"
        ):
            """Example-ipv4-topology should be empty now."""
            prefix_counting.check_ipv4_topology_is_empty()

        with allure_step_with_separate_logging("step_start_listening_bgp_speaker"):
            """Start Python speaker in listening mode."""
            self.bgp_speaker_process = bgp.start_bgp_speaker(
                ammount=count_change_count_single,
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
            self.store_change_count()
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
            change_counter.wait_for_change_count_to_become_stable(
                minimum_value=self.last_change_count_single + 1,
                timeout=bgp_filling_timeout,
                wait_period=CHECK_PERIOD_CHANGE_COUNT_SINGLE,
                consecutive_times_stable_value=REPETITIONS_CHANGE_COUNT_SINGLE,
            )

        with allure_step_with_separate_logging("step_listening_ip_topology_count"):
            """Count the routes in example-ipv4-topology and fail if the count is
            not correct."""
            prefix_counting.check_ipv4_topology_prefixes_count(
                count_change_count_single
            )

        with allure_step_with_separate_logging("step_kill_listening_bgp_speaker"):
            """Abort the Python speaker. Also, attempt to stop failing fast."""
            self.store_change_count()
            bgp.stop_bgp_speaker(self.bgp_speaker_process)

        with allure_step_with_separate_logging(
            "step_store_result_for_listening_bgp_speaker"
        ):
            """Store results for plotting."""
            infra.backup_file(
                f"totals-{RESULTS_FILE_NAME}",
                target_file_name=f"changecount-listening-totals-${RESULTS_FILE_NAME}",
                src_dir=".",
            )
            infra.backup_file(
                f"performance-{RESULTS_FILE_NAME}",
                target_file_name=f"changecount-listening-performance-{RESULTS_FILE_NAME}",
                src_dir=".",
            )

        with allure_step_with_separate_logging(
            "step_wait_for_stable_ip_topology_after_listening"
        ):
            """Wait until example-ipv4-topology becomes stable again."""
            change_counter.wait_for_change_count_to_become_stable(
                minimum_value=self.last_change_count_single + 1,
                timeout=bgp_emptying_timeout,
                wait_period=CHECK_PERIOD_CHANGE_COUNT_SINGLE,
                consecutive_times_stable_value=REPETITIONS_CHANGE_COUNT_SINGLE,
            )

        with allure_step_with_separate_logging(
            "step_check_for_empty_ip_topology_after_listening"
        ):
            """Example-ipv4-topology should be empty now."""
            prefix_counting.check_ipv4_topology_is_empty()

        with allure_step_with_separate_logging(
            "step_restore_data_change_counter_configuration"
        ):
            """Configure data change counter back to count transactions affecting
            example-linkstate-topology."""
            change_counter.set_change_counter(
                topology_name="example-linkstate-topology"
            )

        with allure_step_with_separate_logging("step_delete_bgp_peer_configuration"):
            """Revert the BGP configuration to the original state: without any
            configured peers."""
            bgp.delete_bgp_neighbour(ip=TOOLS_IP, rib_instance=RIB_INSTANCE)
