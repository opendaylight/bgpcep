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
import pytest
import signal

from lib import bgp
from lib import infra
from lib import ip_topology
from lib import utils


PREFIXES_COUNT = 600_000
BGP_PEERS_COUNT = 2
FIRST_PEER_IP = "127.0.0.1"
KARAF_BGPCEP_LOG_LEVEL = "INFO"
KARAF_LOG_LEVEL = os.environ["KARAF_LOG_LEVEL"]
TEST_DURATION_MULTIPLIER = int(os.environ["TEST_DURATION_MULTIPLIER"])
BGP_FILLING_TIMEOUT = TEST_DURATION_MULTIPLIER * (PREFIXES_COUNT * 3.0 / 10000 + 20)
BGP_EMPTYING_TIMEOUT = BGP_FILLING_TIMEOUT * 3 / 4

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.run(order=5)
class TestManyPeerPrefixCount:
    bgp_speaker_process = None

    def test_many_peers_prefix_count(self, step_logger):

        with step_logger("step_check_for_empty_topology_before_talking"):
            """Wait for example-ipv4-topology to come up and empty. Give large
            timeout for case when BGP boots slower than restconf."""
            topology_count = utils.wait_until_function_returns_value(
                120, 1, 0, ip_topology.get_ipv4_topology_prefixes_count
            )
            assert (
                topology_count == 0
            ), f"Ipv4 topology is not empty as expected, there are {topology_count} prefixes present."

        with step_logger("step_reconfigure_odl_to_accept_connections"):
            """Configure BGP peer modules with initiate-connection set to false."""
            bgp.set_bgp_neighbours(
                first_neigbout_ip=FIRST_PEER_IP, count=BGP_PEERS_COUNT, passive_mode=True
            )

        with step_logger("step_change_karaf_logging_levels"):
            """We may want to set more verbose logging here after configuration is
            done."""
            infra.execute_karaf_command(
                f"log:set {KARAF_BGPCEP_LOG_LEVEL} org.opendaylight.bgpcep"
            )
            infra.execute_karaf_command(
                f"log:set {KARAF_BGPCEP_LOG_LEVEL} org.opendaylight.protocol"
            )

        with step_logger("step_start_talking_bgp_managers"):
            """Start Python manager to connect speakers to ODL."""
            TestManyPeerPrefixCount.bgp_speaker_process = bgp.start_bgp_speaker(
                ammount=PREFIXES_COUNT, multiplicity=BGP_PEERS_COUNT, listen=False
            )
            assert infra.is_process_still_running(
                TestManyPeerPrefixCount.bgp_speaker_process
            ), "Bgp speaker process is not running"

        with step_logger("step_wait_for_stable_talking_ip_topology"):
            """Wait until example-ipv4-topology becomes stable. This is done by
            checking stability of prefix count."""
            ip_topology.wait_for_ipv4_topology_prefixes_to_become_stable(
                excluded_value=0, wait_period=20, timeout=BGP_FILLING_TIMEOUT
            )

        with step_logger("step_check_talking_ip_topology_count"):
            """Count the routes in example-ipv4-topology and fail if the count is
            not correct."""
            topology_count = ip_topology.get_ipv4_topology_prefixes_count()
            assert (
                topology_count == PREFIXES_COUNT
            ), f"Ipv4 topology does not contain all {PREFIXES_COUNT} expected advertised prefixes, but only {topology_count}"

        with step_logger("step_kill_talking_bgp_speakers"):
            """Abort the Python speakers. Also, attempt to stop failing fast."""
            bgp.stop_bgp_speaker(self.bgp_speaker_process)

        with step_logger("step_wait_for_stable_ip_topology_after_talking"):
            """Wait until example-ipv4-topology becomes stable again."""
            ip_topology.wait_for_ipv4_topology_prefixes_to_become_stable(
                excluded_value=PREFIXES_COUNT, timeout=BGP_EMPTYING_TIMEOUT
            )

        with step_logger("step_check_for_empty_ip_topology_after_talking"):
            """Example-ipv4-topology should be empty now."""
            topology_count = ip_topology.get_ipv4_topology_prefixes_count()
            assert (
                topology_count == 0
            ), f"Ipv4 topology is not empty as expected, there are {topology_count} prefixes present."

        with step_logger("step_restore_karaf_logging_levels"):
            """Set logging on bgpcep and protocol to the global value."""
            infra.execute_karaf_command(
                f"log:set {KARAF_LOG_LEVEL} org.opendaylight.bgpcep"
            )
            infra.execute_karaf_command(
                f"log:set {KARAF_LOG_LEVEL} org.opendaylight.protocol"
            )

        with step_logger("step_delete_bgp_peer_configuartions"):
            """Revert the BGP configuration to the original state: without any
            configured peers."""
            bgp.delete_bgp_neighbours(
                first_neigbout_ip=FIRST_PEER_IP, count=BGP_PEERS_COUNT
            )
