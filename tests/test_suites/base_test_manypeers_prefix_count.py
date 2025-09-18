#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# BGP performance of ingesting from many iBGP peers, data change
# counter NOT used.This suite uses play.py processes as iBGP peers.
# This is analogue of single peer performance suite,
# which uses many peers. Each peer is of ibgp type, and they
# contribute to the same example-bgp-rib, and thus to the same
# single example-ipv4-topology. The suite only looks at
# example-ipv4-topology, so RIB is not examined.
# The suite consists of two halves, differing on which side initiates
# BGP connection. State of "work is being done" is detected by
# increasing value of prefixes in topology. The time for
# test_wait_for_stable_* cases to finish is the main performance
# metric. After waiting for stability is done, full check on
# number of prefixes present is performed.
# TODO: Figure out how to use Docker and docker IP pool available in
# RelEng. Currently, 127.0.0.1 is hardcoded as the first peer address
# to use.
# Brief description how to configure BGP peer can be found here:
# https://wiki.opendaylight.org/view/BGP_LS_PCEP:User_Guide#BGP_Peer
# http://docs.opendaylight.org/en/stable-boron/user-guide/bgp-user-guide.html#bgp-peering

import logging
import os

from lib import bgp
from lib import infra
from lib import ip_topology
from lib import utils


FIRST_PEER_IP = "127.0.0.1"
KARAF_BGPCEP_LOG_LEVEL = "INFO"
KARAF_LOG_LEVEL = os.environ["KARAF_LOG_LEVEL"]
TEST_DURATION_MULTIPLIER = int(os.environ["TEST_DURATION_MULTIPLIER"])

log = logging.getLogger(__name__)


class BaseTestManyPeerPrefixCount:
    bgp_speaker_process = None

    def test_many_peers_prefix_count(
        self,
        allure_step_with_separate_logging,
        bgp_peers_count,
        prefixes_count,
        insert,
        withdraw,
        prefill,
    ):
        bgp_filling_timeout = (
            TEST_DURATION_MULTIPLIER
            * (prefixes_count * bgp_peers_count * 4.0 / 10000 + 80)
            + 100
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
            "step_reconfigure_odl_to_accept_connections"
        ):
            """Configure BGP peer modules with initiate-connection set to false."""
            bgp.set_bgp_neighbours(
                first_neigbout_ip=FIRST_PEER_IP,
                count=bgp_peers_count,
                passive_mode=True,
            )

        with allure_step_with_separate_logging("step_change_karaf_logging_levels"):
            """We may want to set more verbose logging here after configuration is
            done."""
            infra.execute_karaf_command(
                f"log:set {KARAF_BGPCEP_LOG_LEVEL} org.opendaylight.bgpcep"
            )
            infra.execute_karaf_command(
                f"log:set {KARAF_BGPCEP_LOG_LEVEL} org.opendaylight.protocol"
            )

        with allure_step_with_separate_logging("step_start_talking_bgp_managers"):
            """Start Python manager to connect speakers to ODL."""
            self.bgp_speaker_process = bgp.start_bgp_speaker(
                ammount=prefixes_count,
                multiplicity=bgp_peers_count,
                insert=insert,
                withdraw=withdraw,
                prefill=prefill,
                update="single",
                log_level="info",
                listen=False,
            )
            assert infra.is_process_still_running(
                self.bgp_speaker_process
            ), "Bgp speaker process is not running"

        with allure_step_with_separate_logging(
            "step_wait_for_stable_talking_ip_topology"
        ):
            """Wait until example-ipv4-topology becomes stable. This is done by
            checking stability of prefix count."""
            ip_topology.wait_for_ipv4_topology_prefixes_to_become_stable(
                excluded_value=0, wait_period=20, timeout=bgp_filling_timeout
            )

        with allure_step_with_separate_logging("step_check_talking_ip_topology_count"):
            """Count the routes in example-ipv4-topology and fail if the count is
            not correct."""
            topology_count = ip_topology.get_ipv4_topology_prefixes_count()
            assert (
                topology_count == prefixes_count
            ), f"Ipv4 topology does not contain all {prefixes_count} expected advertised prefixes, but only {topology_count}"

        with allure_step_with_separate_logging("step_kill_talking_bgp_speakers"):
            """Abort the Python speakers. Also, attempt to stop failing fast."""
            bgp.stop_bgp_speaker(self.bgp_speaker_process)

        with allure_step_with_separate_logging(
            "step_wait_for_stable_ip_topology_after_talking"
        ):
            """Wait until example-ipv4-topology becomes stable again."""
            ip_topology.wait_for_ipv4_topology_prefixes_to_become_stable(
                excluded_value=prefixes_count, timeout=bgp_emptying_timeout
            )

        with allure_step_with_separate_logging(
            "step_check_for_empty_ip_topology_after_talking"
        ):
            """Example-ipv4-topology should be empty now."""
            topology_count = ip_topology.get_ipv4_topology_prefixes_count()
            assert (
                topology_count == 0
            ), f"Ipv4 topology is not empty as expected, there are {topology_count} prefixes present."

        with allure_step_with_separate_logging("step_restore_karaf_logging_levels"):
            """Set logging on bgpcep and protocol to the global value."""
            infra.execute_karaf_command(
                f"log:set {KARAF_LOG_LEVEL} org.opendaylight.bgpcep"
            )
            infra.execute_karaf_command(
                f"log:set {KARAF_LOG_LEVEL} org.opendaylight.protocol"
            )

        with allure_step_with_separate_logging("step_delete_bgp_peer_configuartions"):
            """Revert the BGP configuration to the original state: without any
            configured peers."""
            bgp.delete_bgp_neighbours(
                first_neigbout_ip=FIRST_PEER_IP, count=bgp_peers_count
            )
