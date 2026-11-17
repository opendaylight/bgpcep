#
# Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# Based on the original Robot Framework integration tests:
# https://github.com/opendaylight/integration-test/blob/901c7e139945b436d95a44b3b592904c3d7a4f9f/csit/suites/bgpcep/bgpclustering/010_singlepeer_prefixcount_1route_nonreplicated_rib.robot
# https://github.com/opendaylight/integration-test/blob/901c7e139945b436d95a44b3b592904c3d7a4f9f/csit/suites/bgpcep/bgpclustering/020_singlepeer_prefixcount_100kroutes_nonreplicated_rib.robot
#

from libraries import cluster
from suites.base_test_singlepeer_prefixcount_clustering import (
    BaseTestSinglePeerPrefixCountClustering,
)


class BaseTestSinglePeerPrefixCountNonreplicatedRib(
    BaseTestSinglePeerPrefixCountClustering
):
    """Shared part of the suites run against a cluster with its own RIB.

    These suites differ from their replicated siblings only in where the
    requests go. The routes live on a single member, so the session check
    has to be aimed at that one; the topology they are exported into is
    replicated as usual, and the counts are read from its shard leader
    because the Robot originals do, not because the followers have nothing
    to show. The flow itself stays the clustering base class unchanged, so
    the prefix count is the only thing separating the suites of this family
    from each other.
    """

    _topology_leader: str | None = None
    _rib_host: str | None = None

    @property
    def hosts_to_check(self) -> list[str]:
        """The topology shard leader, which is what the Robot originals read.

        The topology stays replicated even though the RIB does not, so the
        followers can answer as well; this keeps to the one node those
        originals ask.

        Looked up once and kept, the way they resolve it into a suite
        variable before the flow starts. Re-reading it between steps would
        quietly follow a leader that moved mid-test, turning a failed
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
