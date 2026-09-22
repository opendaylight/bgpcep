#
# Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# Based on the original Robot Framework integration test:
# https://github.com/opendaylight/integration-test/blob/901c7e139945b436d95a44b3b592904c3d7a4f9f/csit/suites/bgpcep/bgpclustering/singlepeer_pc_shm_300kroutes_longevity.robot
#

import logging
import textwrap

import pytest

from libraries import utils
from libraries.variables import variables
from suites.base_test_singlepeer_prefixcount_clustering import (
    BaseTestSinglePeerPrefixCountClustering,
)
from suites.suite_order import SuiteOrder


PREFIXES_COUNT = 300_000
INSERT = 1
WITHDRAW = 0
PREFILL = 0
LONGEVITY_TEST_DURATION_IN_SECS = variables.LONGEVITY_TEST_DURATION_IN_SECS
# Robot waits a second between scenario repetitions.
SCENARIO_INTERVAL = 1

log = logging.getLogger(__name__)


@pytest.mark.bgp
@pytest.mark.ibgp
@pytest.mark.longevity
@pytest.mark.performance
@pytest.mark.single_device
@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.parametrize(
    "prefixes_count, insert, withdraw, prefill",
    [(PREFIXES_COUNT, INSERT, WITHDRAW, PREFILL)],
)
@pytest.mark.run(order=SuiteOrder.BGP_INGEST_PC_SHM_300K_LONGEVITY)
class TestSinglePeer300KRoutesLongevity(BaseTestSinglePeerPrefixCountClustering):
    """Soak variant of the single peer prefix count flow.

    Reuses the base flow unchanged and only replaces run_scenario, so the peer
    is configured once, the advertise and withdraw cycle is repeated for the
    configured duration, and the peer is removed at the end even if a
    repetition fails.
    """

    test_description = textwrap.dedent(
        """
            **BGP longevity soak of ingesting from 1 iBGP peer**

            Data change counter is NOT used. This suite uses play.py as single \
            iBGP peer which talks to single controller. Test suite checks \
            changes of the example-ipv4-topology. RIB is not examined.

            The BGP peer is configured once, then the advertise and withdraw \
            scenario is repeated for the whole configured duration, failing as \
            soon as any repetition does. The peer configuration is removed at \
            the end.

            singlepeer_pc_shm_300kroutes_longevity: pc - prefix counting, \
            shm - shard monitoring.
        """
    )

    @pytest.fixture(autouse=True)
    def _captured_logs(self, caplog):
        """Keeps pytest's log capture reachable from run_scenario.

        Args:
            caplog (pytest.LogCaptureFixture): Captured records of this test.

        Returns:
            None
        """
        self._caplog = caplog

    def run_scenario(
        self,
        allure_step_with_separate_logging,
        prefixes_count,
        insert,
        withdraw,
        prefill,
    ):
        """Repeats the ingest cycle for the whole configured duration.

        The records of an iteration that passed are dropped before the next one
        starts. A soak running for the default 23 hours would otherwise keep
        every record of every iteration in memory, since pytest captures the
        whole call phase at the DEBUG level configured in tox.ini. A failing
        iteration raises before its records are dropped, so the report still
        shows what went wrong.

        Args:
            allure_step_with_separate_logging: Allure step context manager.
            prefixes_count (int): Number of prefixes the speaker advertises.
            insert (int): Prefixes added per update message.
            withdraw (int): Prefixes withdrawn per update message.
            prefill (int): Prefixes advertised before the measured part starts.

        Returns:
            None
        """

        def run_ingest_cycle_and_drop_passed_logs():
            """Runs one cycle, keeping its records only if it failed."""
            self.run_ingest_cycle(
                allure_step_with_separate_logging,
                prefixes_count,
                insert,
                withdraw,
                prefill,
            )
            self._caplog.clear()

        log.info(
            f"Repeating the ingest scenario for {LONGEVITY_TEST_DURATION_IN_SECS}s"
        )
        utils.verify_function_does_not_fail_for_duration(
            LONGEVITY_TEST_DURATION_IN_SECS,
            SCENARIO_INTERVAL,
            run_ingest_cycle_and_drop_passed_logs,
        )
