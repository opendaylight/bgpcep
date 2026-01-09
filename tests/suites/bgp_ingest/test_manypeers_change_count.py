#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import pytest

from suites.base_test_manypeers_change_count import BaseTestManyPeerChangeCount


COUNT_CHANGE_COUNT_MANY = 600_000
BGP_PEERS_COUNT = 20
INSERT = None
WITHDRAW = None
PREFILL = None


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.parametrize(
    "bgp_peers_count, count_change_count_many, insert, withdraw, prefill",
    [(BGP_PEERS_COUNT, COUNT_CHANGE_COUNT_MANY, INSERT, WITHDRAW, PREFILL)],
)
@pytest.mark.run(order=6)
class TestManyPeersChangeCountMixed(BaseTestManyPeerChangeCount):
    test_description = (
        "**BGP performance of ingesting from many iBGP peers**\n"
        "\n"
        "Data change counter is used. This suite uses play.py processes as "
        "iBGP peers. This is analogue of single peer performance suite, "
        "which uses many peers. Each peer is of ibgp type, and they "
        "contribute to the same example-bgp-rib, and thus to the same single "
        "example-ipv4-topology. The suite only looks at "
        "example-ipv4-topology, so RIB is not examined.\n"
        "\n"
        "This suite requires odl-bgpcep-data-change-counter to be installed "
        "so make sure it is added to \"install-features\" of any jobs that "
        "are going to invoke it. Use the other version of the suite "
        "(test_manypeers_prefix_count.py) if the feature does not work.\n"
        "\n"
        "The suite consists of two halves, differing on which side initiates "
        "BGP connection. Data change counter is a lightweight way to detect "
        "\"work is being done\".Utils provide a nice function to wait for "
        "stability, but it needs initial value, that is why "
        "store_change_count appears just before work-inducing action. "
        "The time for *wait_for_\*to_become_stable* cases to finish is "
        "the main performance metric. After waiting for stability "
        "is done, full check on number of prefixes present is performed.\n"
        "\n"
        "**TODO:** Currently, if a bug causes zero increase of data changes, "
        "affected test cases will wait for max time. Reconsider "
        "if zero increase is allowed as stable, higher number of repetitions "
        "should be required.\n"
        "\n"
        "ODL distinguishes peers by their IP addresses.\n"
        "**TODO:** Figure out how to use Docker and docker IP pool available "
        "in RelEng.\n"
        "\n"
        "Brief description how to configure BGP peer can be found here:\n"
        "*https://wiki.opendaylight.org/view/BGP_LS_PCEP:User_Guide#BGP_Peer*\n"
        "*http://docs.opendaylight.org/en/stable-boron/user-guide/bgp-user-guide.html#bgp-peering*\n"
        "\n"
        "**TODO:** Is there a need for version of this suite where ODL "
        "connects to pers? Note that configuring ODL is slow, which may "
        "affect measured performance singificantly.\n"
        "**Advanced TODO:** Give manager ability to start pushing on trigger "
        "long after connections are established."
    )
