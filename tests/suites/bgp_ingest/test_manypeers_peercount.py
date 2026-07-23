#
# Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# Based on the original Robot Framework integration test:
# https://github.com/opendaylight/integration-test/blob/901c7e139945b436d95a44b3b592904c3d7a4f9f/csit/suites/bgpcep/bgpingest/manypeers_peercount.robot
#

import textwrap

import pytest

from suites.base_test_manypeers_peercount import BaseTestManyPeerPeerCount
from suites.suite_order import SuiteOrder


# NOTE: Tame placeholder scale values for local iteration. To be revisited
# (together with the Jenkins sandbox findings) before the suite is finalised.
# COUNT_PEER_COUNT_MANY must stay divisible by BGP_PEERS_COUNT so that the
# expected per-peer received-prefix count is exact.
COUNT_PEER_COUNT_MANY = 30_000
BGP_PEERS_COUNT = 2
INSERT = None
WITHDRAW = None
PREFILL = None


@pytest.mark.bgp
@pytest.mark.ibgp
@pytest.mark.performance
@pytest.mark.multi_device
@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.parametrize(
    "bgp_peers_count, count_peer_count_many, insert, withdraw, prefill",
    [(BGP_PEERS_COUNT, COUNT_PEER_COUNT_MANY, INSERT, WITHDRAW, PREFILL)],
)
@pytest.mark.run(order=SuiteOrder.BGP_INGEST_MANYPEERS_PEER_COUNT)
class TestManyPeersPeerCount(BaseTestManyPeerPeerCount):
    test_description = textwrap.dedent(
        """
            **BGP performance of ingesting from many iBGP rrc peers, iBGPs \
            receive updates.**

            This suite uses play.py processes as iBGP rrc peers. This is \
            analogue of single peer performance suite, which uses many peers. \
            Each peer is of ibgp rrc type, and they contribute to the same \
            example-bgp-rib, and thus to the same single example-ipv4-topology. \
            The suite looks at example-ipv4-topology and checks BGP peers log \
            for received updates.

            ODL distinguishes peers by their IP addresses. \
            Currently, this suite requires python utils to be started on ODL \
            System, to guarantee IP address block is available for them to \
            bind to.

            Brief description how to configure BGP peer can be found here:
            *https://wiki.opendaylight.org/view/BGP_LS_PCEP:User_Guide#BGP_Peer*
            *http://docs.opendaylight.org/en/stable-boron/user-guide/\
            bgp-user-guide.html#bgp-peering*
        """
    )
