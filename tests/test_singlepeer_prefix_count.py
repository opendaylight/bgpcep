#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# BGP performance of ingesting from 1 iBGP peer, data change counter
# is NOT used.This suite uses play.py as single iBGP peer.
# The suite only looks at example-ipv4-topology, so RIB is not
# examined. The suite consists of two halves, differing on which side
# initiates BGP connection. State of "work is being done" is detected
# by increasing value of prefixes in topology. The time for
# test_wait_for_stable_* cases to finish is the main performance
# metric.After waiting for stability is done, full check on
# number of prefixes present is performed. Brief description how to
# configure BGP peer can be found here:
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
TEST_DURATION_MULTIPLIER = int(os.environ["TEST_DURATION_MULTIPLIER"])
BGP_FILLING_TIMEOUT = TEST_DURATION_MULTIPLIER * (PREFIXES_COUNT * 9.0 / 10000 + 20)
BGP_EMPTYING_TIMEOUT = BGP_FILLING_TIMEOUT * 3 / 4

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=2)
class TestSinglePeerPrefixCount:
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
        bgp.set_bgp_neighbour(ip="127.0.0.1", passive_mode=True)

    @pytest.mark.skip_if_fails("test_reconfigure_odl_to_accept_connection")
    def test_start_talking_bgp_speaker(self):
        TestSinglePeerPrefixCount.bgp_speaker_process = bgp.start_bgp_speaker(
            ammount=PREFIXES_COUNT,
            insert=1,
            withdraw=0,
            prefill=0,
            update="single",
            listen=False,
            log_level="info",
        )

    @pytest.mark.skip_if_fails(
        ["test_reconfigure_odl_to_accept_connection", "test_start_talking_bgp_speaker"]
    )
    def test_wait_for_stable_talking_ip_topology(self):
        ip_topology.wait_for_ipv4_topology_prefixes_to_become_stable(
            excluded_value=0, timeout=BGP_FILLING_TIMEOUT
        )

    @pytest.mark.skip_if_fails(
        ["test_reconfigure_odl_to_accept_connection", "test_start_talking_bgp_speaker"]
    )
    def test_talking_ip_topology_count(self):
        topology_count = ip_topology.get_ipv4_topology_prefixes_count()
        assert (
            topology_count == PREFIXES_COUNT
        ), f"Ipv4 topology does not contain all {PREFIXES_COUNT} expected advertised prefixes, but only {topology_count}"

    @pytest.mark.skip_if_fails("test_start_talking_bgp_speaker")
    def test_kill_talking_bgp_speaker(self):
        bgp.stop_bgp_speaker(self.bgp_speaker_process)

    @pytest.mark.skip_if_fails("test_start_talking_bgp_speaker")
    def test_store_result_for_talking_bgp_speaker(self):
        infra.shell("mv totals-bgp.csv results/prefixcount-talking-totals-bgp.csv")
        infra.shell(
            "mv performance-bgp.csv results/prefixcount-talking-performance-bgp.csv"
        )

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

    @pytest.mark.skip_if_fails("test_for_empty_ip_topology_after_talking")
    def test_start_listening_bgp_speaker(self):
        TestSinglePeerPrefixCount.bgp_speaker_process = bgp.start_bgp_speaker(
            ammount=PREFIXES_COUNT,
            insert=1,
            withdraw=0,
            prefill=0,
            update="single",
            listen=True,
            log_level="info",
        )

    @pytest.mark.skip_if_fails("test_start_listening_bgp_speaker")
    def test_reconfigure_odl_to_initiate_connection(self):
        bgp.set_bgp_neighbour(ip="127.0.0.1", passive_mode=False)

    @pytest.mark.skip_if_fails(
        [
            "test_reconfigure_odl_to_initiate_connection",
            "test_start_listening_bgp_speaker",
        ]
    )
    def test_wait_for_stable_listening_ip_topology(self):
        ip_topology.wait_for_ipv4_topology_prefixes_to_become_stable(
            excluded_value=0, timeout=BGP_FILLING_TIMEOUT
        )

    @pytest.mark.skip_if_fails(
        [
            "test_reconfigure_odl_to_initiate_connection",
            "test_start_listening_bgp_speaker",
        ]
    )
    def test_listening_ip_topology_count(self):
        topology_count = ip_topology.get_ipv4_topology_prefixes_count()
        assert (
            topology_count == PREFIXES_COUNT
        ), f"Ipv4 topology does not contain all {PREFIXES_COUNT} expected advertised prefixes, but only {topology_count}"

    @pytest.mark.skip_if_fails(["test_start_listening_bgp_speaker"])
    def test_kill_listening_bgp_speaker(self):
        bgp.stop_bgp_speaker(self.bgp_speaker_process)

    @pytest.mark.skip_if_fails(["test_start_listening_bgp_speaker"])
    def test_store_result_for_listening_bgp_speaker(self):
        infra.shell("mv totals-bgp.csv results/prefixcount-listening-totals-bgp.csv")
        infra.shell(
            "mv performance-bgp.csv results/prefixcount-listening-performance-bgp.csv"
        )

    @pytest.mark.skip_if_fails("test_kill_listening_bgp_speaker")
    def test_wait_for_stable_ip_topology_after_listening(self):
        ip_topology.wait_for_ipv4_topology_prefixes_to_become_stable(
            excluded_value=PREFIXES_COUNT, timeout=BGP_EMPTYING_TIMEOUT
        )

    def test_for_empty_ip_topology_after_listening(self):
        topology_count = ip_topology.get_ipv4_topology_prefixes_count()
        assert (
            topology_count == 0
        ), f"Ipv4 topology is not empty as expected, there are {topology_count} prefixes present."

    def test_delete_bgp_peer_configuartion(self):
        bgp.delete_bgp_neighbour(ip="127.0.0.1")
