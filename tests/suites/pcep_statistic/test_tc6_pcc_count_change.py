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
INITAL_PCC_COUNT = 1
UPDATED_PCC_COUNT = 5
INITAL_LSP_PER_PCC_COUNT = 5
UPDATED_LSP_PER_PCC_COUNT = 1
ODL_IP = variables.ODL_IP
TOOLS_IP = variables.TOOLS_IP

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_pcep_pcc_mock_processes")
@pytest.mark.run(order=82)
class TestPcepUser:
    pcc_mock_process = None

    @allure.description(
        "**Ensure PCC is being updated after topology change**\n"
        "\n"
        "Tests that PCEP statistics are correctly updated after changing "
        "number of connected PCCs, reflecting the current state."
    )
    def test_stats_updated_after_topology_change(
        self, allure_step_with_separate_logging
    ):

        with allure_step_with_separate_logging("step_set_timer_value_to_1_second"):
            """Update timer value to lowest possible value."""
            pcep.set_stat_timer_value(1)

        with allure_step_with_separate_logging(
            f"step_start_pcc_mock_{INITAL_PCC_COUNT}_pcc_"
            f"{INITAL_LSP_PER_PCC_COUNT}_lsps"):
            """Starts PCC mocks simulator with initial count of simulated PCC device
            and initial count of reported LSPs."""
            self.pcc_mock_process = pcep.start_pcc_mock(
                pcc=INITAL_PCC_COUNT,
                lsp=INITAL_LSP_PER_PCC_COUNT,
                local_address=TOOLS_IP,
                remote_address=ODL_IP,
                verify_introduced_lsps=True,
                verify_interval=0.1,
            )

        with allure_step_with_separate_logging(f"step_wait_1_second"):
            """Wait one second until the next pcep stat update."""
            time.sleep(1)

        with allure_step_with_separate_logging("step_verfiy_correct_stats_are_present"):
            """Verifies that get-stat RPC does return statistics."""
            pcep.verify_stats_pcc_count(
                expected_pcc_count=INITAL_PCC_COUNT,
                expected_lsps_per_pcc_count=INITAL_LSP_PER_PCC_COUNT
            )

        with allure_step_with_separate_logging("step_stop_pcc_mock"):
            """Stop PCC mocks simulator."""
            pcep.stop_pcc_mock_process(self.pcc_mock_process)

        with allure_step_with_separate_logging(
            f"step_start_pcc_mock_{UPDATED_PCC_COUNT}_pcc_"
            f"{UPDATED_LSP_PER_PCC_COUNT}_lsps"):
            """Starts PCC mocks simulator with updated count of simulated PCC device
            and updated count of reported LSPs."""
            self.pcc_mock_process = pcep.start_pcc_mock(
                pcc=UPDATED_PCC_COUNT,
                lsp=UPDATED_LSP_PER_PCC_COUNT,
                local_address=TOOLS_IP,
                remote_address=ODL_IP,
                verify_introduced_lsps=True,
                verify_interval=0.1,
            )

        with allure_step_with_separate_logging(f"step_wait_1_second"):
            """Wait one second until the next pcep stat update."""
            time.sleep(1)

        with allure_step_with_separate_logging("step_verfiy_updated_stats_are_present"):
            """Verifies that get-stat RPC does return statistics."""
            pcep.verify_stats_pcc_count(
                expected_pcc_count=UPDATED_PCC_COUNT,
                expected_lsps_per_pcc_count=UPDATED_LSP_PER_PCC_COUNT
            )

        with allure_step_with_separate_logging("step_stop_pcc_mock"):
            """Stop PCC mocks simulator."""
            pcep.stop_pcc_mock_process(self.pcc_mock_process)

        with allure_step_with_separate_logging("step_set_timer_value_back_to_default"):
            """Update timer value back to the original default value."""
            pcep.set_stat_timer_value(DEFAULT_PCEP_STATS_UPDATE_INTERVAL)
