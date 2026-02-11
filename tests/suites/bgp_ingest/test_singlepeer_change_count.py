#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import textwrap

import pytest

from suites.base_test_singlepeer_change_count import BaseTestSinglePeerChangeCount


COUNT_CHANGE_COUNT_SINGLE = 600_000
INSERT = 1
PREFILL = 0
WITHDRAW = 0


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.parametrize(
    "count_change_count_single, insert, withdraw, prefill",
    [(COUNT_CHANGE_COUNT_SINGLE, INSERT, WITHDRAW, PREFILL)],
)
@pytest.mark.run(order=3)
class TestSinglePeerChangeCountMixed(BaseTestSinglePeerChangeCount):
    test_description = (
        textwrap.dedent("""
            **This suite uses play.py as single iBGP peer.**

            The suite only looks at example-ipv4-topology, so RIB is not \
            examined. This suite requires odl-bgpcep-data-change-counter to be \
            installed so make sure it is added to "install-features" of any \
            jobs that are going to invoke it.

            The suite consists of two halves, differing on which side \
            initiates BGP connection. Data change counter is a lightweight \
            way to detect "work is being done". Utils library provide a nice \
            function to wait for stability, but it needs initial value, \
            that is why last_change_count_single appears just before \
            work-inducing action.
            The time for *wait_for_\*to_become_stable* cases to finish is \
            the main performance metric.After waiting for stability is done, \
            full check on number of prefixes present is performed.

            Brief description how to configure BGP peer can be found here:
            *https://wiki.opendaylight.org/view/BGP_LS_PCEP:User_Guide#BGP_Peer*
            *http://docs.opendaylight.org/en/stable-boron/user-guide/\
            bgp-user-guide.html#bgp-peering*

            **TODO:** Currently, if a bug causes zero increase of data changes, \
            affected test cases will wait for max time. Reconsider \
            if zero increase is allowed as stable, higher number of repetitions \
            should be required.
        """)
    )
