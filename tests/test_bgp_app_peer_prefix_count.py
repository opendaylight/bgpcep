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
from lib import utils


PREFILL_COUNT = 5_000
ADDITIONAL_COUNT = 200
TOTAL_COUNT = PREFILL_COUNT + ADDITIONAL_COUNT

log = logging.getLogger(__name__)

@pytest.mark.usefixtures("preconditions")
class TestBgpAppPeerPrefixCount:
    bgp_speaker_process = None
    last_change_count_single = 1

    @pytest.mark.run(order=1)
    def test_for_empty_topology_before_starting(self):
        topology_count = ip_topology.get_ipv4_topology_count()
        assert topology_count == 0, f"Ipv4 topology is not empty as expected, there are {topology_count} prefixes present."

    @pytest.mark.run(order=2)
    def test_reconfigure_odl_to_accept_connection(self):
        bgp.set_bgp_neighbour_user(ip="127.0.0.1", passive_mode=True)

    @pytest.mark.run(order=3)
    def test_reconfigure_odl_to_accept_bgp_application_peer(self):
        bgp.set_bgp_application_peer(ip="10.0.0.10")

    @pytest.mark.run(order=4)
    def test_connect_bgp_peer(self):
        TestBgpAppPeerPrefixCount.bgp_speaker_process = bgp.start_bgp_speaker(ammount=0, listen=False)

    @pytest.mark.run(order=5)
    def test_bgp_application_peer_prefill_routes(self):
        bgp.start_bgp_app_peer(count=PREFILL_COUNT, log_level="debug")
        infra.shell("mv bgp_app_peer.log results/bgp_app_peer_prefill.log")

    @pytest.mark.run(order=6)
    def test_wait_for_ip_topology_is_prefilled(self):
        count = utils.retry_function_and_expect_value(retry_count=20, interval=5, expected_value=PREFILL_COUNT, function=ip_topology.get_ipv4_topology_count)
        assert count == PREFILL_COUNT, "Not all expected prefixes were found in ipv4 topology"

    @pytest.mark.run(order=7)
    def test_bgp_peer_updates_for_prefilled_routes(self):
        count = utils.wait_for_string_in_file(5, 3, f"total_received_nlri_prefix_counter: {PREFILL_COUNT}", "bgp_peer.log")
        assert 1 <= count, "Did not find expected received prefixes in bgp_peer.log file"

    @pytest.mark.run(order=8)
    def test_bgp_application_peer_intorduce_single_routes(self):
        bgp.start_bgp_app_peer(count=ADDITIONAL_COUNT, command="add", prefix="12.0.0.0", log_level="debug")
        infra.shell("mv bgp_app_peer.log results/bgp_app_peer_singles.log")

    @pytest.mark.run(order=9)
    def test_wait_for_ip_topology_is_filled(self):
        count = ip_topology.get_ipv4_topology_count()
        assert count == TOTAL_COUNT, "Not all expected prefixes were found in ipv4 topology"

    @pytest.mark.run(order=10)
    def test_bgp_peer_updates_for_all_routes(self):
        count = utils.wait_for_string_in_file(5, 3, f"total_received_nlri_prefix_counter: {TOTAL_COUNT}", "bgp_peer.log")
        assert 1 <= count, "Did not find expected received prefixes in bgp_peer.log file"

    @pytest.mark.run(order=11)
    def test_disconnect_bgp_peer(self):
        bgp.stop_bgp_speaker(self.bgp_speaker_process)
        infra.shell("mv bgp_peer.log results/bgp_peer_disconnect.log")

    @pytest.mark.run(order=12)
    def test_reconnect_bgp_peer(self):
        TestBgpAppPeerPrefixCount.bgp_speaker_process = bgp.start_bgp_speaker(ammount=0, listen=False)

    @pytest.mark.run(order=13)
    def test_bgp_peer_updates_for_reintroduced_routes(self):
        count = utils.wait_for_string_in_file(5, 3, f"total_received_nlri_prefix_counter: {TOTAL_COUNT}", "bgp_peer.log")
        assert 1 <= count, "Did not find expected received prefixes in bgp_peer.log file"

    @pytest.mark.run(order=14)
    def test_bgp_application_peer_delete_all_routes(self):
        bgp.start_bgp_app_peer(command="delete-all", log_level="debug")
        infra.shell("mv bgp_app_peer.log results/bgp_app_peer_delete_all.log")

    @pytest.mark.run(order=15)
    def test_wait_for_stable_topology_after_deletion(self):
        ip_topology.wait_for_ipv4_topology_prefixes_to_become_stable(excluded_value=TOTAL_COUNT)

    @pytest.mark.run(order=16)
    def test_for_empty_topology_after_deletion(self):
        count = ip_topology.get_ipv4_topology_count()
        assert 0 == count, "There are still some prefixes in ipv4 topology after deletion, expected empty"

    @pytest.mark.run(order=17)
    def test_bgp_peer_updates_for_prefix_withdrawals(self):
        count = utils.wait_for_string_in_file(5, 3, f"total_received_withdrawn_prefix_counter: {TOTAL_COUNT}", "bgp_peer.log")
        assert 1 <= count, "Did not find expected received prefixes in bgp_peer.log file"

    @pytest.mark.run(order=18)
    def test_stop_bgp_peer(self):
        bgp.stop_bgp_speaker(self.bgp_speaker_process)
        infra.shell("mv bgp_peer.log results/bgp_peer_reconnect.log")

    @pytest.mark.run(order=19)
    def test_delete_bgp_peer_configuration(self):
        bgp.delete_bgp_neighbour_user("127.0.0.1")

    @pytest.mark.run(order=20)
    def test_delete_bgp_application_peer_configuration(self):
         bgp.set_bgp_application_peer(ip="10.0.0.10")

    

    