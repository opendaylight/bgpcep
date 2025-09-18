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

import pytest

from test_suites.base_test_manypeers_prefix_count import BaseTestManyPeerPrefixCount


PREFIXES_COUNT = 600_000
BGP_PEERS_COUNT = 20
INSERT = None
WITHDRAW = None
PREFILL = None


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.parametrize("bgp_peers_count, prefixes_count, insert, withdraw, prefill", [(BGP_PEERS_COUNT ,PREFIXES_COUNT, INSERT, WITHDRAW, PREFILL)])
@pytest.mark.run(order=5)
class TestManyPeersPrefixCount(BaseTestManyPeerPrefixCount):
    pass
