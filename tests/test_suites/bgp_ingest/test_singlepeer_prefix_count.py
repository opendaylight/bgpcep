#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# This suite uses play.py as single iBGP peer. The suite only looks at
# example-ipv4-topology, so RIB is not examined.
# The suite consists of two halves, differing on which side initiates BGP
# connection. State of "work is being done" is detected by increasing value of
# prefixes in topology. The time for Wait_For_Stable_* cases to finish is the
# main performance metric. After waiting for stability is done, full check on
# number of prefixes present is performed.
# Brief description how to configure BGP peer can be found here:
# https://wiki.opendaylight.org/view/BGP_LS_PCEP:User_Guide#BGP_Peer
# http://docs.opendaylight.org/en/stable-boron/user-guide/bgp-user-guide.html#bgp-peering
# TODO: Currently, if a bug causes prefix count to remain at zero, affected
# test cases will wait for max time. Reconsider. If zero is allowed as stable,
# higher period or repetitions would be required.
# The prefix counting is quite heavyweight and may induce large variation in
# time. Try the other version of the suite (test_singlepeer_change_count.py)
# to get better precision.

import pytest

from test_suites.base_test_singlepeer_prefix_count import BaseTestSinglePeerPrefixCount


PREFIXES_COUNT = 600_000
INSERT = 1
WITHDRAW = 0
PREFILL = 0


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.parametrize("prefixes_count, insert, withdraw, prefill", [(PREFIXES_COUNT, INSERT, WITHDRAW, PREFILL)])
@pytest.mark.run(order=2)
class TestSinglePeerPrefixCount(BaseTestSinglePeerPrefixCount):
    pass