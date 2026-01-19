#
# Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import logging

import allure
import pytest
import time

from libraries import pcep
from libraries.variables import variables


DEFAULT_PCEP_STATS_UPDATE_INTERVAL = variables.DEFAULT_PCEP_STATS_UPDATE_INTERVAL
PCC_COUNT = 70
LSP_PER_PCC_COUNT = 145
ODL_IP = variables.ODL_IP
TOOLS_IP = variables.TOOLS_IP

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=77)
class TestPcepUser:
    pcc_mock_process = None

    @allure.description(
        "**Ensure system works properly with big topologies (70 sessions/10kLSPs)**\n"
        "\n"
        "Tests that module for PCEP statistics is able to correctly handle large "
        "number of connected PCC devices and reported LSPs."
    )
    def test_stats_updated_after_topology_change(
        self, allure_step_with_separate_logging
    ):

        with allure_step_with_separate_logging("step_set_timer_value_to_1_second"):
            """Update timer value to lowest possible value."""
            pcep.set_stat_timer_value(1)

        with allure_step_with_separate_logging("step_start_pcc_mock_1_pcc_5_lsps"):
            """Starts PCC mocks simulator with 1 simulated PCC device
            and 5 reported LSPs."""
            self.pcc_mock_process = pcep.start_pcc_mock(
                pcc=PCC_COUNT,
                lsp=LSP_PER_PCC_COUNT,
                local_address=TOOLS_IP,
                remote_address=ODL_IP,
                verify_introduced_lsps=True,
                verify_interval=0.1,
            )

        with allure_step_with_separate_logging(f"step_wait_1_second"):
            """Wait one second until the next pcep stat update."""
            time.sleep(1)

        with allure_step_with_separate_logging("step_verfiy_correct_stats_are_present"):
            """Verifies that get-stat RPC does return correct statistics containing 
            all PCC devices and all reported LSPs."""
            pcep.verify_stats_pcc_count(
                expected_pcc_count=PCC_COUNT, 
                expected_lsps_per_pcc_count=LSP_PER_PCC_COUNT
            )

        with allure_step_with_separate_logging("step_stop_pcc_mock"):
            """Stop PCC mocks simulator."""
            pcep.stop_pcc_mock_process(self.pcc_mock_process)


        with allure_step_with_separate_logging("step_set_timer_value_back_to_default"):
            """Update timer value back to the original default value."""
            pcep.set_stat_timer_value(DEFAULT_PCEP_STATS_UPDATE_INTERVAL)
