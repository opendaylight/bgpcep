#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import logging
import pytest
import signal
import time

from lib import bgp
from lib import change_counter
from lib import infra
from lib import ip_topology


PREFIXES_COUNT = 600_000
BGP_PEERS_COUNT = 10
FIRST_PEER_IP = "127.0.0.1"

log = logging.getLogger(__name__)

@pytest.mark.usefixtures("preconditions")
class TestSinglePeerChangeCount:
    bgp_speaker_process = None
    last_change_count = 1

    @pytest.mark.run(order=1)
    def test_for_empty_topology_before_talking(self):
        topology_count = ip_topology.get_ipv4_topology_count()
        assert topology_count == 0, f"Ipv4 topology is not empty as expected, there are {topology_count} prefixes present."

    @pytest.mark.run(order=2)
    def test_reconfigure_odl_to_accept_connection(self):
        bgp.set_bgp_peer_group(peer_group_name="internal-neighbors", passive_mode=True)
        bgp.set_bgp_neighbours_user(first_neigbout_ip=FIRST_PEER_IP, count=BGP_PEERS_COUNT, passive_mode=True)

    @pytest.mark.run(order=3)
    def test_recofigure_data_change_counter(self):
        change_counter.set_change_counter(topology_name="example-ipv4-topology")

    @pytest.mark.run(order=4)
    def test_data_change_counter_ready(self):
        retires = 5
        for _ in range(retires):
            try:
                change_counter.get_change_count()
                break
            except Exception as e:
                log.info(f"Failed to get data chagne with the followign error: {e}")
                time.sleep(1)
        else:
            raise Exception(f"Failed to get data change counter after {retires} retries")
        
    @pytest.mark.run(order=5)
    def test_change_karaf_logging_levels(self):
        infra.execute_karaf_command("log:set INFO org.opendaylight.bgpcep")
        infra.execute_karaf_command("log:set INFO org.opendaylight.protocol")


    @pytest.mark.run(order=6)
    def test_start_talking_bgp_speaker(self):
        TestSinglePeerChangeCount.last_change_count = change_counter.get_change_count()
        TestSinglePeerChangeCount.bgp_speaker_process = bgp.start_bgp_speaker(ammount=PREFIXES_COUNT,
                          multiplicity=BGP_PEERS_COUNT,
                          listen=False)

    @pytest.mark.run(order=7)
    def test_wait_for_stable_talking_ip_topology(self):
        change_counter.wait_for_change_count_to_become_stable(minimum_value=self.last_change_count+1)

    @pytest.mark.run(order=8)
    def test_talking_ip_topology_count(self):
        topology_count = ip_topology.get_ipv4_topology_count()
        assert topology_count == PREFIXES_COUNT, f"Ipv4 topology does not contain all {PREFIXES_COUNT} expected advertised prefixes, but only {topology_count}"

    @pytest.mark.run(order=9)
    def test_kill_talking_bgp_speaker(self):
        TestSinglePeerChangeCount.last_change_count = change_counter.get_change_count()
        bgp.stop_bgp_speaker(self.bgp_speaker_process)
        infra.shell("mv play.py.out results/manypeers_cc_play.log")

    @pytest.mark.run(order=10)
    def test_wait_for_stable_ip_topology_after_talking(self):
        change_counter.wait_for_change_count_to_become_stable(minimum_value=self.last_change_count+1)

    @pytest.mark.run(order=11)
    def test_for_empty_ip_topology_after_talking(self):
        topology_count = ip_topology.get_ipv4_topology_count()
        assert topology_count == 0, f"Ipv4 topology is not empty as expected, there are {topology_count} prefixes present."

    @pytest.mark.run(order=12)
    def test_restore_karaf_logging_levels(self):
        infra.execute_karaf_command("log:set INFO org.opendaylight.bgpcep")
        infra.execute_karaf_command("log:set INFO org.opendaylight.protocol")

    @pytest.mark.run(order=13)
    def test_restore_data_change_counter_configuration(self):
        change_counter.set_change_counter(topology_name="example-linkstate-topology")

    @pytest.mark.run(order=14)
    def test_delete_bgp_peer_configuartion(self):
        bgp.delete_bgp_neighbours_user(first_neigbout_ip=FIRST_PEER_IP, count=BGP_PEERS_COUNT)
        bgp.delete_bgp_peer_group(peer_group_name="internal-neighbors")