#
# Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# Based on the original Robot Framework integration test:
# https://github.com/opendaylight/integration-test/blob/901c7e139945b436d95a44b3b592904c3d7a4f9f/csit/suites/bgpcep/bgpclustering/010_singlepeer_prefixcount_1route.robot
#

import textwrap

import pytest

from suites.base_test_singlepeer_prefixcount_clustering import (
    BaseTestSinglePeerPrefixCountClustering,
)
from suites.suite_order import SuiteOrder


PREFIXES_COUNT = 1
INSERT = 1
WITHDRAW = 0
PREFILL = 0


@pytest.mark.bgp
@pytest.mark.cluster
@pytest.mark.ibgp
@pytest.mark.functional
@pytest.mark.multi_device
@pytest.mark.usefixtures("odl_three_node_cluster_bgp")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.parametrize(
    "prefixes_count, insert, withdraw, prefill",
    [(PREFIXES_COUNT, INSERT, WITHDRAW, PREFILL)],
)
@pytest.mark.run(order=SuiteOrder.BGP_INGEST_SINGLEPEER_PREFIXCOUNT_1ROUTE_CLUSTER)
class TestSinglePeerPrefixCount1RouteCluster(BaseTestSinglePeerPrefixCountClustering):
    """Three-node form of the 1route suite.

    The Robot original is a clustering suite, so the migrated flow is run in
    both topologies: the standalone sibling keeps it in the smoke selection,
    while this one restores what only a cluster can show, namely that the
    route reaches every member's operational datastore and not just the one
    holding the BGP session.

    The flow itself is the shared base class, unchanged. Only the fixture
    differs, so a failure here is about the cluster and nothing else.
    """

    test_description = textwrap.dedent(
        """
            **BGP ingesting of a single route from 1 iBGP peer, on a cluster**

            Data change counter is NOT used. This suite uses play.py as single \
            iBGP peer which talks to a three-node controller cluster. Test \
            suite checks changes of the example-ipv4-topology on every member. \
            RIB is not examined.

            Only one member accepts the session, since the peer registry lives \
            in the BGP singleton, but the resulting route is replicated, so \
            the prefix count is asserted on all three members.
        """
    )
