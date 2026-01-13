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
        "**Ensure PCC is being updated after topology change**\n"
        "\n"
        "Tests that PCEP statistics are correctly updated after changing "
        "number of connected PCCs and reported LSPs, reflecting "
        "the current state."
    )
    def test_stats_updated_after_topology_change(self, allure_step_with_separate_logging):

        with allure_step_with_separate_logging(
            "step_set_timer_value_to_1_second"
        ):
            """Update timer value to lowest possible value."""
            pcep.set_stat_timer_value(1)

        with allure_step_with_separate_logging("step_start_pcc_mock_1_pcc_5_lsps"):
            """Starts PCC mocks simulator with 1 simulated PCC device
            and 5 reported LSPs."""
            self.pcc_mock_process = pcep.start_pcc_mock(
                pcc=1,
                lsp=5,
                local_address=TOOLS_IP,
                remote_address=ODL_IP,
                verify_introduced_lsps=True,
                verify_interval=0.1,
            )

        with allure_step_with_separate_logging(
            f"step_wait_1_second"
        ):
            """Wait one second until the next pcep stat update."""
            time.sleep(1)

        with allure_step_with_separate_logging("step_verfiy_stats_are_present"):
            """Verifies that get-stat RPC does return statistics."""
            pcep.verify_stats_pcc_count(expected_pcc_count=1, expected_lsps_per_pcc_count=5)

        with allure_step_with_separate_logging("step_stop_pcc_mock"):
            """Stop PCC mocks simulator."""
            pcep.stop_pcc_mock_process(self.pcc_mock_process)

        with allure_step_with_separate_logging("step_start_pcc_mock_5_pcc_5_lsps"):
            """Starts PCC mocks simulator with 5 simulated PCC device
            and 5 reported LSPs. Each PCC report one LSP."""
            self.pcc_mock_process = pcep.start_pcc_mock(
                pcc=5,
                lsp=1,
                local_address=TOOLS_IP,
                remote_address=ODL_IP,
                verify_introduced_lsps=True,
                verify_interval=0.1,
            )

        with allure_step_with_separate_logging(
            f"step_wait_1_second"
        ):
            """Wait one second until the next pcep stat update."""
            time.sleep(1)

        with allure_step_with_separate_logging("step_verfiy_stats_are_present"):
            """Verifies that get-stat RPC does return statistics."""
            pcep.verify_stats_pcc_count(expected_pcc_count=5, expected_lsps_per_pcc_count=1)

        with allure_step_with_separate_logging("step_stop_pcc_mock"):
            """Stop PCC mocks simulator."""
            pcep.stop_pcc_mock_process(self.pcc_mock_process)

        with allure_step_with_separate_logging("step_set_timer_value_back_to_default"):
            """Update timer value back to the original default value."""
            pcep.set_stat_timer_value(DEFAULT_PCEP_STATS_UPDATE_INTERVAL)
