#
# Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# Based on the original Robot Framework integration test:
# https://github.com/opendaylight/integration-test/blob/901c7e139945b436d95a44b3b592904c3d7a4f9f/csit/suites/bgpcep/bgpclustering/030_singlepeer_prefixcount_1Mroutes_nonreplicated_rib.robot
#

import textwrap

import pytest

from suites.base_test_singlepeer_prefixcount_nonreplicated_rib import (
    BaseTestSinglePeerPrefixCountNonreplicatedRib,
)
from suites.suite_order import SuiteOrder


PREFIXES_COUNT = 1_000_000
INSERT = 1
WITHDRAW = 0
PREFILL = 0


@pytest.mark.bgp
@pytest.mark.cluster
@pytest.mark.ibgp
@pytest.mark.performance
@pytest.mark.multi_device
@pytest.mark.usefixtures("odl_three_node_cluster_nonreplicated_rib")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.parametrize(
    "prefixes_count, insert, withdraw, prefill",
    [(PREFIXES_COUNT, INSERT, WITHDRAW, PREFILL)],
)
@pytest.mark.run(
    order=SuiteOrder.BGP_INGEST_SINGLEPEER_PREFIXCOUNT_1MROUTES_NONREPLICATED_RIB
)
class TestSinglePeerPrefixCount1MRoutesNonreplicatedRib(
    BaseTestSinglePeerPrefixCountNonreplicatedRib
):
    """The 1Mroutes flow against a cluster whose BGP RIB is not replicated.

    The largest of the unreplicated prefix counting suites, and otherwise the
    same run as the rest of the family: the prefix count is the only thing
    that differs, so neither the flow nor the node resolution is repeated
    here. The timeouts the base class derives from the count grow with it, so
    this suite needs none of its own.
    """

    test_description = textwrap.dedent(
        """
            **BGP performance of ingesting 1000000 routes from 1 iBGP peer, \
            unreplicated RIB**

            Data change counter is NOT used. This suite uses play.py as single \
            iBGP peer which talks to a three-node controller cluster whose \
            bgp_rib shard each member keeps to itself. Test suite checks \
            changes of the example-ipv4-topology on the leader of the \
            topology operational shard. RIB is not examined.

            Less stress on the cluster is expected than when the followers \
            are triggered as well, which is the point of the unreplicated \
            variant.
        """
    )
