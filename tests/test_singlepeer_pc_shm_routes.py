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

from lib import bgp
from lib import infra
from lib import ip_topology


PREFIXES_COUNT = 300_000

log = logging.getLogger(__name__)

@pytest.mark.usefixtures("preconditions")
class TestSinglePeer300KRoutes:
    bgp_speaker_process = None

    def test_empty_topology_before_talking(self):
        topology_count = ip_topology.get_ipv4_topology_count()
        assert topology_count == 0, f"Ipv4 topology is not empty as expected, there are {topology_count} prefixes present."

    def test_reconfigure_odl_to_accept_connection(self):
        bgp.set_bgp_neighbour(ip="127.0.0.1", passive_mode=True)

    def test_start_talking_bgp_speaker(self):
        TestSinglePeer300KRoutes.bgp_speaker_process = bgp.start_bgp_speaker(ammount=PREFIXES_COUNT,
                          insert=1,
                          withdraw=0,
                          prefill=0,
                          update="single",
                          listen=False)

    def test_wait_for_stable_talking_ip_topology(self):
        ip_topology.wait_for_ipv4_topology_prefixes_to_become_stable(excluded_value=0)

    def test_talking_ip_topology_count(self):
        topology_count = ip_topology.get_ipv4_topology_count()
        assert topology_count == PREFIXES_COUNT, f"Ipv4 topology does not contain all {PREFIXES_COUNT} expected advertised prefixes, but only {topology_count}"

    def test_kill_talking_bgp_speaker(self):
        self.bgp_speaker_process.send_signal(signal.SIGINT)

    def test_wait_for_stable_ip_topology_after_listening(self):
        ip_topology.wait_for_ipv4_topology_prefixes_to_become_stable(excluded_value=PREFIXES_COUNT)

    def test_for_empty_ip_topology_after_listening(self):
        topology_count = ip_topology.get_ipv4_topology_count()
        assert topology_count == 0, f"Ipv4 topology is not empty as expected, there are {topology_count} prefixes present."

    def test_delete_bgp_peer_configuartion(self):
        bgp.delete_bgp_neighbour(ip="127.0.0.1")