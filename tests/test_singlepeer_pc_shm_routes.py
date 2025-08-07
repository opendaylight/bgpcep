#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
#
# BGP performance of ingesting from 1 iBGP peer, data change
# counter is NOT used.This suite uses play.py as single iBGP peer
# which talks to single controller.Test suite checks changes
# of the the example-ipv4-topology on all nodes. RIB is not examined.
# test_singlepeer_pc_300kroutes: pc - prefix counting

import logging
import pytest
import signal

from lib import bgp
from lib import infra
from lib import ip_topology
from lib import utils


PREFIXES_COUNT = 300_000

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.run(order=1)
class TestSinglePeer300KRoutes:
    bgp_speaker_process = None

    def test_for_empty_topology_before_talking(self):
        topology_count = utils.retry_function_and_expect_value(
            120, 1, 0, ip_topology.get_ipv4_topology_prefixes_count
        )
        assert (
            topology_count == 0
        ), f"Ipv4 topology is not empty as expected, there are {topology_count} prefixes present."
        # assert False, "This test case should fail"

    @pytest.mark.skip_if_fails("test_for_empty_topology_before_talking")
    def test_reconfigure_odl_to_accept_connection(self):
        bgp.set_bgp_neighbour(ip="127.0.0.1", passive_mode=True)

    @pytest.mark.skip_if_fails("test_reconfigure_odl_to_accept_connection")
    def test_start_talking_bgp_speaker(self):
        TestSinglePeer300KRoutes.bgp_speaker_process = (
            bgp.start_bgp_speaker_with_verify_and_retry(
                ammount=PREFIXES_COUNT,
                insert=1,
                withdraw=0,
                prefill=0,
                update="single",
                listen=False,
                info=True,
            )
        )

    @pytest.mark.skip_if_fails(
        ["test_reconfigure_odl_to_accept_connection", "test_start_talking_bgp_speaker"]
    )
    def test_wait_for_stable_talking_ip_topology(self):
        ip_topology.wait_for_ipv4_topology_prefixes_to_become_stable(excluded_value=0)

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

    @pytest.mark.skip_if_fails("test_kill_talking_bgp_speaker")
    def test_wait_for_stable_ip_topology_after_listening(self):
        ip_topology.wait_for_ipv4_topology_prefixes_to_become_stable(
            excluded_value=PREFIXES_COUNT
        )

    def test_for_empty_ip_topology_after_listening(self):
        topology_count = ip_topology.get_ipv4_topology_prefixes_count()
        assert (
            topology_count == 0
        ), f"Ipv4 topology is not empty as expected, there are {topology_count} prefixes present."

    def test_delete_bgp_peer_configuartion(self):
        bgp.delete_bgp_neighbour(ip="127.0.0.1")
