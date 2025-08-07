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

    def test_for_empty_topology_before_talking(self):
        topology_count = utils.wait_until_function_returns_value(
            120, 1, 0, ip_topology.get_ipv4_topology_prefixes_count
        )
        assert (
            topology_count == 0
        ), f"Ipv4 topology is not empty as expected, there are {topology_count} prefixes present."

    @pytest.mark.skip_if_fails("test_for_empty_topology_before_talking")
    def test_reconfigure_odl_to_accept_connection(self):
        bgp.set_bgp_neighbours(
            first_neigbout_ip=FIRST_PEER_IP, count=BGP_PEERS_COUNT, passive_mode=True
        )

    @pytest.mark.skip_if_fails("test_reconfigure_odl_to_accept_connection")
    def test_change_karaf_logging_levels(self):
        infra.execute_karaf_command(
            f"log:set {KARAF_BGPCEP_LOG_LEVEL} org.opendaylight.bgpcep"
        )
        infra.execute_karaf_command(
            f"log:set {KARAF_BGPCEP_LOG_LEVEL} org.opendaylight.protocol"
        )

    @pytest.mark.skip_if_fails("test_reconfigure_odl_to_accept_connection")
    def test_start_talking_bgp_manager(self):
        TestManyPeerPrefixCount.bgp_speaker_process = bgp.start_bgp_speaker(
            ammount=PREFIXES_COUNT, multiplicity=BGP_PEERS_COUNT, listen=False
        )

    @pytest.mark.skip_if_fails(
        ["test_reconfigure_odl_to_accept_connection", "test_start_talking_bgp_manager"]
    )
    def test_wait_for_stable_talking_ip_topology(self):
        ip_topology.wait_for_ipv4_topology_prefixes_to_become_stable(
            excluded_value=0, timeout=BGP_FILLING_TIMEOUT
        )

    @pytest.mark.skip_if_fails(
        ["test_reconfigure_odl_to_accept_connection", "test_start_talking_bgp_manager"]
    )
    def test_talking_ip_topology_count(self):
        topology_count = ip_topology.get_ipv4_topology_prefixes_count()
        assert (
            topology_count == PREFIXES_COUNT
        ), f"Ipv4 topology does not contain all {PREFIXES_COUNT} expected advertised prefixes, but only {topology_count}"

    @pytest.mark.skip_if_fails("test_start_talking_bgp_manager")
    def test_kill_talking_bgp_speaker(self):
        bgp.stop_bgp_speaker(self.bgp_speaker_process)

    @pytest.mark.skip_if_fails("test_kill_talking_bgp_speaker")
    def test_wait_for_stable_ip_topology_after_talking(self):
        ip_topology.wait_for_ipv4_topology_prefixes_to_become_stable(
            excluded_value=PREFIXES_COUNT, timeout=BGP_EMPTYING_TIMEOUT
        )

    def test_for_empty_ip_topology_after_talking(self):
        topology_count = ip_topology.get_ipv4_topology_prefixes_count()
        assert (
            topology_count == 0
        ), f"Ipv4 topology is not empty as expected, there are {topology_count} prefixes present."

    def test_restore_karaf_logging_levels(self):
        infra.execute_karaf_command(
            f"log:set {KARAF_LOG_LEVEL} org.opendaylight.bgpcep"
        )
        infra.execute_karaf_command(
            f"log:set {KARAF_LOG_LEVEL} org.opendaylight.protocol"
        )

    def test_delete_bgp_peer_configuartion(self):
        bgp.delete_bgp_neighbours(
            first_neigbout_ip=FIRST_PEER_IP, count=BGP_PEERS_COUNT
        )
