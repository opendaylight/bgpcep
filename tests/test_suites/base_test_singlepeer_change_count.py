#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# BGP performance of ingesting from 1 iBGP peer, data change counter
# is used. This suite uses play.py as single iBGP peer. The suite
# only looks at example-ipv4-topology, so RIB is not examined.
# This suite requires odl-bgpcep-data-change-counter to be installed
# so make sure it is added to "install-features" of any jobs
# that are going to invoke it.The suite consists of two halves,
# differing on which side initiates BGP connection.
# Data change counter is a lightweight way to detect
# "work is being done". change_counter.py provide a nice function
# to wait for stability, but it needs initial value, that is why
# "TestSinglePeerChangeCount.last_change_count_single = change_counter.get_change_count()"
# appears just before work-inducing action. The time for
# test_wait_for_stable_* cases to finish is the main performance
# metric.After waiting for stability is done, full check on
# number of prefixes present is performed.
# Brief description how to configure BGP peer can be found here:
# https://wiki.opendaylight.org/view/BGP_LS_PCEP:User_Guide#BGP_Peer
# http://docs.opendaylight.org/en/stable-boron/user-guide/bgp-user-guide.html#bgp-peering


import logging
import os

from lib import bgp
from lib import change_counter
from lib import infra
from lib import ip_topology
from lib import utils


TEST_DURATION_MULTIPLIER = int(os.environ["TEST_DURATION_MULTIPLIER"])

log = logging.getLogger(__name__)


class BaseTestSinglePeerChangeCount:
    bgp_speaker_process = None
    last_change_count_single = 1

    def test_single_peer_change_count(
        self,
        allure_step_with_separate_logging,
        prefixes_count,
        insert,
        withdraw,
        prefill,
    ):

        bgp_filling_timeout = TEST_DURATION_MULTIPLIER * (
            prefixes_count * 9.0 / 10000 + 20
        )
        bgp_emptying_timeout = bgp_filling_timeout * 3 / 4

        with allure_step_with_separate_logging(
            "step_check_for_empty_topology_before_talking"
        ):
            """Wait for example-ipv4-topology to come up and empty. Give large
            timeout for case when BGP boots slower than restconf."""
            topology_count = utils.wait_until_function_returns_value(
                120, 1, 0, ip_topology.get_ipv4_topology_prefixes_count
            )
            assert (
                topology_count == 0
            ), f"Ipv4 topology is not empty as expected, there are {topology_count} prefixes present."

        with allure_step_with_separate_logging(
            "step_reconfigure_odl_to_accept_connection"
        ):
            """Configure BGP peer module with initiate-connection set to false."""
            bgp.set_bgp_neighbour(ip="127.0.0.1", passive_mode=True)

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
            self.last_change_count_single = change_counter.get_change_count()
            self.bgp_speaker_process = bgp.start_bgp_speaker(
                ammount=prefixes_count,
                insert=insert,
                withdraw=withdraw,
                prefill=prefill,
                update="single",
                listen=False,
                log_level="info",
            )
            assert infra.is_process_still_running(
                self.bgp_speaker_process
            ), "Bgp speaker process is not running"

        with allure_step_with_separate_logging(
            "step_wait_for_stable_talking_ip_topology"
        ):
            """Wait until example-ipv4-topology becomes stable. This is done by
            checking the change counter."""
            change_counter.wait_for_change_count_to_become_stable(
                minimum_value=self.last_change_count_single + 1,
                timeout=bgp_filling_timeout,
            )

        with allure_step_with_separate_logging("step_check_talking_ip_topology_count"):
            """Count the routes in example-ipv4-topology and fail if the count is
            not correct."""
            topology_count = ip_topology.get_ipv4_topology_prefixes_count()
            assert (
                topology_count == prefixes_count
            ), f"Ipv4 topology does not contain all {prefixes_count} expected advertised prefixes, but only {topology_count}"

        with allure_step_with_separate_logging("step_kill_talking_bgp_speaker"):
            """Abort the Python speaker."""
            self.last_change_count_single = change_counter.get_change_count()
            bgp.stop_bgp_speaker(self.bgp_speaker_process)

        with allure_step_with_separate_logging(
            "step_store_result_for_talking_bgp_speaker"
        ):
            """Store results for plotting."""
            infra.shell(
                "mv totals-bgp.csv results/changecount-talking-totals-bgp.csv",
                check_rc=True,
            )
            infra.shell(
                "mv performance-bgp.csv results/changecount-talking-performance-bgp.csv",
                check_rc=True,
            )

        with allure_step_with_separate_logging(
            "step_wait_for_stable_ip_topology_after_talking"
        ):
            """Wait until example-ipv4-topology becomes stable again."""
            change_counter.wait_for_change_count_to_become_stable(
                minimum_value=self.last_change_count_single + 1,
                timeout=bgp_emptying_timeout,
            )

        with allure_step_with_separate_logging(
            "step_check_for_empty_ip_topology_after_talking"
        ):
            """Example-ipv4-topology should be empty now."""
            topology_count = ip_topology.get_ipv4_topology_prefixes_count()
            assert (
                topology_count == 0
            ), f"Ipv4 topology is not empty as expected, there are {topology_count} prefixes present."

        with allure_step_with_separate_logging("step_start_listening_bgp_speaker"):
            """Start Python speaker in listening mode."""
            self.bgp_speaker_process = bgp.start_bgp_speaker(
                ammount=prefixes_count,
                insert=insert,
                withdraw=withdraw,
                prefill=prefill,
                update="single",
                listen=True,
                log_level="info",
            )
            assert infra.is_process_still_running(
                self.bgp_speaker_process
            ), "Bgp speaker process is not running"

        with allure_step_with_separate_logging(
            "step_reconfigure_odl_to_initiate_connection"
        ):
            """Replace BGP peer config module, now with initiate-connection set to
            true."""
            self.last_change_count_single = change_counter.get_change_count()
            bgp.set_bgp_neighbour(ip="127.0.0.1", passive_mode=False)

        with allure_step_with_separate_logging(
            "step_wait_for_stable_listening_ip_topology"
        ):
            """Wait until example-ipv4-topology becomes stable."""
            change_counter.wait_for_change_count_to_become_stable(
                minimum_value=self.last_change_count_single + 1,
                timeout=bgp_filling_timeout,
            )

        with allure_step_with_separate_logging("step_listening_ip_topology_count"):
            """Count the routes in example-ipv4-topology and fail if the count is
            not correct."""
            topology_count = ip_topology.get_ipv4_topology_prefixes_count()
            assert (
                topology_count == prefixes_count
            ), f"Ipv4 topology does not contain all {prefixes_count} expected advertised prefixes, but only {topology_count}"

        with allure_step_with_separate_logging("step_kill_listening_bgp_speaker"):
            """Abort the Python speaker. Also, attempt to stop failing fast."""
            self.last_change_count_single = change_counter.get_change_count()
            bgp.stop_bgp_speaker(self.bgp_speaker_process)

        with allure_step_with_separate_logging(
            "step_store_result_for_listening_bgp_speaker"
        ):
            """Store results for plotting."""
            infra.shell(
                "mv totals-bgp.csv results/changecount-listening-totals-bgp.csv",
                check_rc=True,
            )
            infra.shell(
                "mv performance-bgp.csv results/changecount-listening-performance-bgp.csv",
                check_rc=True,
            )

        with allure_step_with_separate_logging(
            "step_wait_for_stable_ip_topology_after_listening"
        ):
            """Wait until example-ipv4-topology becomes stable again."""
            change_counter.wait_for_change_count_to_become_stable(
                minimum_value=self.last_change_count_single + 1,
                timeout=bgp_emptying_timeout,
            )

        with allure_step_with_separate_logging(
            "step_check_for_empty_ip_topology_after_listening"
        ):
            """Example-ipv4-topology should be empty now."""
            topology_count = ip_topology.get_ipv4_topology_prefixes_count()
            assert (
                topology_count == 0
            ), f"Ipv4 topology is not empty as expected, there are {topology_count} prefixes present."

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
            bgp.delete_bgp_neighbour(ip="127.0.0.1")
