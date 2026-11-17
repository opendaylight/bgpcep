#
# Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# Based on the original Robot Framework integration test:
# https://github.com/opendaylight/integration-test/blob/901c7e139945b436d95a44b3b592904c3d7a4f9f/csit/suites/bgpcep/bgpclustering/010_singlepeer_prefixcount_1route_nonreplicated_rib.robot
#

import textwrap

import pytest

from libraries import cluster
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
@pytest.mark.usefixtures("odl_three_node_cluster_nonreplicated_rib")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.parametrize(
    "prefixes_count, insert, withdraw, prefill",
    [(PREFIXES_COUNT, INSERT, WITHDRAW, PREFILL)],
)
@pytest.mark.run(
    order=SuiteOrder.BGP_INGEST_SINGLEPEER_PREFIXCOUNT_1ROUTE_NONREPLICATED_RIB
)
class TestSinglePeerPrefixCount1RouteNonreplicatedRib(
    BaseTestSinglePeerPrefixCountClustering
):
    """The 1route flow against a cluster whose BGP RIB is not replicated.

    Each member keeps its own RIB, so the routes are not carried to the other
    members and only the node building the topology has them to show. The
    flow itself is the shared base class, unchanged; the fixture and the node
    the counts are read from are the whole difference.
    """

    _topology_leader: str | None = None
    _rib_host: str | None = None

    @property
    def hosts_to_check(self) -> list[str]:
        """The topology shard leader, which is the only node to see routes.

        Looked up once and kept, the way the Robot original resolves it into
        a suite variable before the flow starts. Re-reading it between steps
        would quietly follow a leader that moved mid-test, turning a failed
        hand-over into a pass.

        Args:
            None

        Returns:
            list[str]: Address of the topology operational shard leader.
        """
        if self._topology_leader is None:
            type(self)._topology_leader = cluster.shard_leader(
                "topology", "operational"
            )
        return [self._topology_leader]

    @property
    def rib_host(self) -> str:
        """The member owning the BGP singleton, the only one with the peer.

        Its RIB is not replicated to the others, so asking any other member
        whether the speaker connected answers that the data does not exist,
        whatever the session is doing.

        Args:
            None

        Returns:
            str: Address of the member owning the BGP singleton.
        """
        if self._rib_host is None:
            type(self)._rib_host = cluster.bgp_singleton_owner_host()
        return self._rib_host

    test_description = textwrap.dedent(
        """
            **BGP ingesting of a single route from 1 iBGP peer, \
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
