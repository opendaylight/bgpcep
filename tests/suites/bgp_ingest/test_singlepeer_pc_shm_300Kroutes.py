#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# Based on the original Robot Framework integration test:
# https://github.com/opendaylight/integration-test/blob/901c7e139945b436d95a44b3b592904c3d7a4f9f/csit/suites/bgpcep/bgpclustering/singlepeer_pc_shm_300kroutes.robot
#

import textwrap

import pytest

from suites.base_test_singlepeer_prefixcount_clustering import (
    BaseTestSinglePeerPrefixCountClustering,
)
from suites.suite_order import SuiteOrder


PREFIXES_COUNT = 300_000


@pytest.mark.bgp
@pytest.mark.ibgp
@pytest.mark.performance
@pytest.mark.single_device
@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.parametrize("prefixes_count", [PREFIXES_COUNT])
@pytest.mark.run(order=SuiteOrder.BGP_INGEST_PC_SHM_300K)
class TestSinglePeer300KRoutes(BaseTestSinglePeerPrefixCountClustering):
    test_description = textwrap.dedent(
        """
            **BGP performance of ingesting from 1 iBGP peer**

            Data change counter is NOT used.This suite uses play.py \
            as single iBGP peer which talks to single controller. Test suite \
            checks changes of the the example-ipv4-topology on all nodes. \
            RIB is not examined.
            test_singlepeer_pc_300kroutes: pc - prefix counting.
        """
    )
