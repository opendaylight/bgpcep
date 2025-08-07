#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# BGP performance of ingesting from many iBGP peers, data change
# counter is used. This suite uses play.py processes as iBGP peers.
# This is analogue of single peer performance suite, which uses
# many peers. Each peer is of ibgp type, and they contribute
# to the same example-bgp-rib, and thus to the same single
# example-ipv4-topology. The suite only looks at
# example-ipv4-topology, so RIB is not examined.
# This suite requires odl-bgpcep-data-change-counter to be installed
# so make sure it is added to "install-features" of any jobs that are
# going to invoke it. The suite consists of two halves,
# differing on which side initiates BGP connection.
# Data change counter is a lightweight way to detect
# "work is being done". change_counter.py provide a nice function
# to wait for stability, but it needs initial value, that is why
# "TestManyPeerChangeCount.last_change_count_single = change_counter.get_change_count()"
# appears just before work-inducing action. The time for
# test_wait_for_stable_* cases to finish is the main
# performance metric. After waiting for stability is done,
# full check on number of prefixes present is performed.
# TODO: Figure out how to use Docker and docker IP pool available in
# RelEng. Currently, 127.0.0.1 is hardcoded as the first
# peer address to use.
# Brief description how to configure BGP peer can be found here:
# https://wiki.opendaylight.org/view/BGP_LS_PCEP:User_Guide#BGP_Peer
# http://docs.opendaylight.org/en/stable-boron/user-guide/bgp-user-guide.html#bgp-peering

import logging
import os
import pytest
import signal
import time

from lib import bgp
from lib import change_counter
from lib import infra
from lib import ip_topology
from lib import utils


PREFIXES_COUNT = 600_000
BGP_PEERS_COUNT = 10
FIRST_PEER_IP = "127.0.0.1"
KARAF_BGPCEP_LOG_LEVEL = "INFO"
KARAF_LOG_LEVEL = os.environ["KARAF_LOG_LEVEL"]
TEST_DURATION_MULTIPLIER = int(os.environ["TEST_DURATION_MULTIPLIER"])
BGP_FILLING_TIMEOUT = TEST_DURATION_MULTIPLIER * (PREFIXES_COUNT * 3.0 / 10000 + 100)
BGP_EMPTYING_TIMEOUT = BGP_FILLING_TIMEOUT * 3 / 4

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.run(order=6)
class TestManyPeerChangeCount:
    bgp_speaker_process = None
    last_change_count = 1

    def test_many_peers_change_count(self, step_logger):

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
            """Configure BGP peer modules with initiate-connection set to false.
            It sets peer-group as template, and than sets all neighbors using
            it."""
            bgp.set_bgp_peer_group(peer_group_name="internal-neighbors", passive_mode=True)
            bgp.set_bgp_peer_group_members(
                first_neigbout_ip=FIRST_PEER_IP,
                count=BGP_PEERS_COUNT,
                peer_group_name="internal-neighbors",
            )

        with step_logger("step_recofigure_data_change_counter"):
            """Configure data change counter to count transactions in
            example-ipv4-topology instead of example-linkstate-topology."""
            change_counter.set_change_counter(topology_name="example-ipv4-topology")

        with step_logger("step_check_data_change_counter_ready"):
            """Data change counter might have been slower to start than ipv4
            topology, wait for it."""
            utils.wait_until_function_pass(5, 1, change_counter.get_change_count)

        with step_logger("step_change_karaf_logging_levels"):
            """We may want to set more verbose logging here after configuration is
            done."""
            infra.execute_karaf_command(
                f"log:set {KARAF_BGPCEP_LOG_LEVEL} org.opendaylight.bgpcep"
            )
            infra.execute_karaf_command(
                f"log:set {KARAF_BGPCEP_LOG_LEVEL} org.opendaylight.protocol"
            )

        with step_logger("step_start_talking_bgp_speakers"):
            """Start Python manager to connect speakers to ODL."""
            TestManyPeerChangeCount.last_change_count = change_counter.get_change_count()
            TestManyPeerChangeCount.bgp_speaker_process = bgp.start_bgp_speaker(
                ammount=PREFIXES_COUNT, multiplicity=BGP_PEERS_COUNT, listen=False
            )
            assert infra.is_process_still_running(
                TestManyPeerChangeCount.bgp_speaker_process
            ), "Bgp speaker process is not running"

        with step_logger("step_wait_for_stable_talking_ip_topology"):
            """Wait until example-ipv4-topology becomes stable. This is done by
            checking stability of the change counter."""
            change_counter.wait_for_change_count_to_become_stable(
                minimum_value=self.last_change_count + 1, timeout=BGP_FILLING_TIMEOUT
            )

        with step_logger("step_check_talking_ip_topology_count"):
            """Count the routes in example-ipv4-topology and fail if the count
            is not correct."""
            topology_count = ip_topology.get_ipv4_topology_prefixes_count()
            assert (
                topology_count == PREFIXES_COUNT
            ), f"Ipv4 topology does not contain all {PREFIXES_COUNT} expected advertised prefixes, but only {topology_count}"

        with step_logger("step_kill_talking_bgp_speakers"):
            """Abort the Python speakers."""
            TestManyPeerChangeCount.last_change_count = change_counter.get_change_count()
            bgp.stop_bgp_speaker(self.bgp_speaker_process)
            infra.shell("mv tmp/play.py.out results/manypeers_cc_play.log")

        with step_logger("step_wait_for_stable_ip_topology_after_talking"):
            """Wait until example-ipv4-topology becomes stable again."""
            change_counter.wait_for_change_count_to_become_stable(
                minimum_value=self.last_change_count + 1, timeout=BGP_EMPTYING_TIMEOUT
            )

        with step_logger("step_check_for_empty_ip_topology_after_talking"):
            """Example-ipv4-topology should be empty now."""
            topology_count = ip_topology.get_ipv4_topology_prefixes_count()
            assert (
                topology_count == 0
            ), f"Ipv4 topology is not empty as expected, there are {topology_count} prefixes present."

        with step_logger("step_restore_karaf_logging_levels"):
            """Set logging on bgpcep and protocol to the global value."""
            infra.execute_karaf_command("log:set INFO org.opendaylight.bgpcep")
            infra.execute_karaf_command("log:set INFO org.opendaylight.protocol")

        with step_logger("step_restore_data_change_counter_configuration"):
            """Configure data change counter back to count transactions affecting
            example-linkstate-topology."""
            change_counter.set_change_counter(topology_name="example-linkstate-topology")

        with step_logger("step_delete_bgp_peer_configuartions"):
            """Revert the BGP configuration to the original state: without any
            configured peers."""
            bgp.delete_bgp_neighbours(
                first_neigbout_ip=FIRST_PEER_IP, count=BGP_PEERS_COUNT
            )
            bgp.delete_bgp_peer_group(peer_group_name="internal-neighbors")
