#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# BGP performance of ingesting from 1 BGP application peer
# Test suite performs basic BGP performance test cases for 
# BGP application peer. BGP application peer introduces routes
# using restconf - in two steps:
# 1. introduces the 100000 number of routes in one POST request
# 2. POSTs the rest of routes (up to the 180000 number) one by one
# Test suite checks that the prefixes are propagated to IPv4 topology
# and announced to BGP peer via updates. Test case where the BGP peer
# is disconnected and reconnected and all routes are deleted by
# BGP application peer are performed as well. Brief description
# how to configure BGP application peer and how to use
# restconf application peer interface:
# https://wiki.opendaylight.org/view/BGP_LS_PCEP:User_Guide#BGP_Application_Peer
# https://wiki.opendaylight.org/view/BGP_LS_PCEP:Programmer_Guide#BGP
# http://docs.opendaylight.org/en/stable-boron/user-guide/bgp-user-guide.html#bgp-peering
# http://docs.opendaylight.org/en/stable-boron/user-guide/bgp-user-guide.html#application-peer-configuration
# Reported bugs:
# Bug 4689 - Not a reasonable duration of 1M prefix introduction
# from BGP application peer via restconf
# Bug 4791 - BGPSessionImpl: Failed to send message Update logged
# even all UPDATE mesages received by iBGP peer


import logging
import pytest
import signal
import time

from lib import bgp
from lib import change_counter
from lib import infra
from lib import ip_topology
from lib import utils


PREFILL_COUNT = 100
ADDITIONAL_COUNT = 80
TOTAL_COUNT = PREFILL_COUNT + ADDITIONAL_COUNT

log = logging.getLogger(__name__)

@pytest.mark.usefixtures("preconditions")
@pytest.mark.run(order=4)
class TestBgpAppPeerPrefixCount:
    bgp_speaker_process = None
    last_change_count_single = 1

    def test_for_empty_topology_before_starting(self):
        topology_count = utils.retry_function_and_expect_value(120, 1, 0, ip_topology.get_ipv4_topology_prefixes_count)
        assert topology_count == 0, f"Ipv4 topology is not empty as expected, there are {topology_count} prefixes present."
        #assert False, "This test case should fail"

    @pytest.mark.skip_if_fails("test_for_empty_topology_before_starting")
    def test_reconfigure_odl_to_accept_connection(self):
        bgp.set_bgp_neighbour_user(ip="127.0.0.1", passive_mode=True)

    @pytest.mark.skip_if_fails("test_for_empty_topology_before_starting")
    def test_reconfigure_odl_to_accept_bgp_application_peer(self):
        bgp.set_bgp_application_peer(ip="10.0.0.10")

    @pytest.mark.skip_if_fails("test_reconfigure_odl_to_accept_connection")
    def test_connect_bgp_peer(self):
        TestBgpAppPeerPrefixCount.bgp_speaker_process = bgp.start_bgp_speaker(ammount=0, listen=False)

    @pytest.mark.skip_if_fails(["test_reconfigure_odl_to_accept_bgp_application_peer",
                                "test_connect_bgp_peer"])
    def test_bgp_application_peer_prefill_routes(self):
        bgp.start_bgp_app_peer(count=PREFILL_COUNT, log_level="info", timeout=3000)
        infra.shell("mv bgp_app_peer.log results/bgp_app_peer_prefill.log")

    @pytest.mark.skip_if_fails("test_bgp_application_peer_prefill_routes")
    def test_wait_for_ip_topology_is_prefilled(self):
        count = utils.retry_function_and_expect_value(retry_count=20, interval=5, expected_value=PREFILL_COUNT, function=ip_topology.get_ipv4_topology_prefixes_count)
        assert count == PREFILL_COUNT, "Not all expected prefixes were found in ipv4 topology"

    @pytest.mark.skip_if_fails("test_bgp_application_peer_prefill_routes")
    def test_bgp_peer_updates_for_prefilled_routes(self):
        count = infra.wait_for_string_in_file(5, 3, f"total_received_nlri_prefix_counter: {PREFILL_COUNT}", "bgp_peer.log")
        assert 1 <= count, "Did not find expected received prefixes in bgp_peer.log file"

    @pytest.mark.skip_if_fails(["test_reconfigure_odl_to_accept_bgp_application_peer",
                                "test_connect_bgp_peer"])
    def test_bgp_application_peer_intorduce_single_routes(self):
        bgp.start_bgp_app_peer(count=ADDITIONAL_COUNT, command="add", prefix="12.0.0.0", log_level="info", timeout=3000)
        infra.shell("mv bgp_app_peer.log results/bgp_app_peer_singles.log")

    @pytest.mark.skip_if_fails("test_bgp_application_peer_intorduce_single_routes")
    def test_wait_for_ip_topology_is_filled(self):
        count = utils.retry_function_and_expect_value(20, 5, TOTAL_COUNT, ip_topology.get_ipv4_topology_prefixes_count)
        assert count == TOTAL_COUNT, "Not all expected prefixes were found in ipv4 topology"

    @pytest.mark.skip_if_fails("test_bgp_application_peer_intorduce_single_routes")
    def test_bgp_peer_updates_for_all_routes(self):
        count = infra.wait_for_string_in_file(20, 5, f"total_received_nlri_prefix_counter: {TOTAL_COUNT}", "bgp_peer.log")
        assert 1 <= count, "Did not find expected received prefixes in bgp_peer.log file"

    @pytest.mark.skip_if_fails("test_connect_bgp_peer")
    def test_disconnect_bgp_peer(self):
        bgp.stop_bgp_speaker(self.bgp_speaker_process)
        infra.shell("mv bgp_peer.log results/bgp_peer_disconnect.log")

    @pytest.mark.skip_if_fails("test_disconnect_bgp_peer")
    def test_reconnect_bgp_peer(self):
        TestBgpAppPeerPrefixCount.bgp_speaker_process = bgp.start_bgp_speaker(ammount=0, listen=False)

    @pytest.mark.skip_if_fails("test_reconnect_bgp_peer")
    def test_bgp_peer_updates_for_reintroduced_routes(self):
        count = infra.wait_for_string_in_file(20, 5, f"total_received_nlri_prefix_counter: {TOTAL_COUNT}", "bgp_peer.log")
        assert 1 <= count, "Did not find expected received prefixes in bgp_peer.log file"

    @pytest.mark.skip_if_fails("test_bgp_peer_updates_for_reintroduced_routes")
    def test_bgp_application_peer_delete_all_routes(self):
        bgp.start_bgp_app_peer(command="delete-all", log_level="info", timeout=3000)
        infra.shell("mv bgp_app_peer.log results/bgp_app_peer_delete_all.log")

    @pytest.mark.skip_if_fails("test_bgp_application_peer_delete_all_routes")
    def test_wait_for_stable_topology_after_deletion(self):
        ip_topology.wait_for_ipv4_topology_prefixes_to_become_stable(excluded_value=TOTAL_COUNT)

    @pytest.mark.skip_if_fails("test_bgp_application_peer_delete_all_routes")
    def test_for_empty_topology_after_deletion(self):
        count = ip_topology.get_ipv4_topology_prefixes_count()
        assert 0 == count, "There are still some prefixes in ipv4 topology after deletion, expected empty"

    @pytest.mark.skip_if_fails("test_bgp_application_peer_delete_all_routes")
    def test_bgp_peer_updates_for_prefix_withdrawals(self):
        count = infra.wait_for_string_in_file(20, 5, f"total_received_withdrawn_prefix_counter: {TOTAL_COUNT}", "bgp_peer.log")
        assert 1 <= count, "Did not find expected received prefixes in bgp_peer.log file"

    @pytest.mark.skip_if_fails("test_reconnect_bgp_peer")
    def test_stop_bgp_peer(self):
        bgp.stop_bgp_speaker(self.bgp_speaker_process)
        infra.shell("mv bgp_peer.log results/bgp_peer_reconnect.log")

    def test_delete_bgp_peer_configuration(self):
        bgp.delete_bgp_neighbour("127.0.0.1")

    def test_delete_bgp_application_peer_configuration(self):
         bgp.delete_bgp_application_peer(ip="10.0.0.10")

    

    