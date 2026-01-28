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
from libraries import utils
from libraries.variables import variables


DEFAULT_PCEP_STATS_UPDATE_INTERVAL = variables.DEFAULT_PCEP_STATS_UPDATE_INTERVAL
ODL_IP = variables.ODL_IP
TOOLS_IP = variables.TOOLS_IP

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_pcep_pcc_mock_processes")
@pytest.mark.run(order=85)
class TestPcepUser:
    pcc_mock_process = None

    @allure.description(
        "**Ensure system works properly with rapidly changing topologies**\n"
        "\n"
        "Tests that module for PCEP statistics is able to correctly handle rapid "
        "changes in number of connected PCC devices and reported LSPs."
    )
    def test_rapidly_changing_topology(self, allure_step_with_separate_logging):

        with allure_step_with_separate_logging("step_set_timer_value_to_1_second"):
            """Update timer value to lowest possible value."""
            pcep.set_stat_timer_value(1)

        for current_count in range(1, 11):

            with allure_step_with_separate_logging(
                f"step_start_pcc_mock_{current_count}_pcc_{current_count}_lsps"
            ):
                """Starts PCC mocks simulator with {current_count} simulated PCC device
                and {current_count} reported LSPs."""
                self.pcc_mock_process = pcep.start_pcc_mock(
                    pcc=current_count,
                    lsp=current_count,
                    local_address=TOOLS_IP,
                    remote_address=ODL_IP,
                    verify_introduced_lsps=True,
                    verify_interval=0.1,
                )

            with allure_step_with_separate_logging(
                f"step_wait_1_second_iteration_{current_count}"
            ):
                """Wait one second until the next pcep stat update."""
                time.sleep(1)

            with allure_step_with_separate_logging(
                f"step_verify_{current_count}_pcc_{current_count}_lsps_"
                "stats_are_present"
            ):
                """Verifies that get-stat RPC does return correct statistics containing
                {current_count} PCC devices and {current_count} reported LSPs."""
                utils.wait_until_function_pass(
                    5,
                    0.1,
                    pcep.verify_global_pcep_statistics,
                    expected_pcc_count=current_count,
                    expected_lsps_per_pcc_count=current_count,
                )

            with allure_step_with_separate_logging(
                f"step_stop_pcc_mock_with_{current_count}_pcc_{current_count}_lsps"
            ):
                """Stop PCC mocks simulator."""
                pcep.stop_pcc_mock_process(self.pcc_mock_process)

        with allure_step_with_separate_logging("step_set_timer_value_back_to_default"):
            """Update timer value back to the original default value."""
            pcep.set_stat_timer_value(DEFAULT_PCEP_STATS_UPDATE_INTERVAL)
