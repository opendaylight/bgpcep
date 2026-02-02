#
# Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import random
import logging
import textwrap

import allure
import pytest
import time

from libraries import pcep
from libraries import utils
from libraries.variables import variables


DEFAULT_PCEP_STATS_UPDATE_INTERVAL = variables.DEFAULT_PCEP_STATS_UPDATE_INTERVAL
PCC_MIN_COUNT = 1
PCC_MAX_COUNT = 10
LSP_MIN_COUNT = 1
LSP_MAX_COUNT = 10
ODL_IP = variables.ODL_IP
TOOLS_IP = variables.TOOLS_IP

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_pcep_pcc_mock_processes")
@pytest.mark.run(order=88)
class TestPcepUser:
    pcc_mock_process = None

    @allure.description(
        textwrap.dedent("""
            **Ensure system properly working with randomly changing topologies**

            Tests that module for PCEP statistics is able to correctly handle random \
            changes in number of connected PCC devices and reported LSPs.
        """)
    )
    def test_randomly_changing_topology(self, allure_step_with_separate_logging):

        with allure_step_with_separate_logging("step_set_timer_value_to_5_second"):
            """Update timer value value."""
            pcep.set_stat_timer_value(5)

        for iteration in range(1, 11):

            with allure_step_with_separate_logging(
                f"generate_rundom_numbers_for_pcc_and_lsp_count"
            ):
                """Choose number of pcc nodes and lsps for this iteration."""
                pcc_count = random.randint(PCC_MIN_COUNT, PCC_MAX_COUNT)
                lsp_count = random.randint(LSP_MIN_COUNT, LSP_MAX_COUNT)

            with allure_step_with_separate_logging(
                f"step_start_pcc_mock_{pcc_count}_pcc_{lsp_count}_lsps_"
                f"iteration_{iteration}"
            ):
                """Starts PCC mocks simulator with {pcc_count} simulated PCC device
                and {lsp_count} reported LSPs."""
                self.pcc_mock_process = pcep.start_pcc_mock(
                    pcc=pcc_count,
                    lsp=lsp_count,
                    local_address=TOOLS_IP,
                    remote_address=ODL_IP,
                    verify_introduced_lsps=True,
                    verify_interval=0.1,
                )

            with allure_step_with_separate_logging(
                f"step_wait_10_second_iteration_{iteration}"
            ):
                """Wait one second until the next pcep stat update."""
                time.sleep(10)

            with allure_step_with_separate_logging(
                f"step_verify_{pcc_count}_pcc_{lsp_count}_lsps_"
                f"stats_are_present_iteration_{iteration}"
            ):
                """Verifies that get-stat RPC does return correct statistics containing
                {pcc_count} PCC devices and {lsp_count} reported LSPs."""
                utils.wait_until_function_pass(
                    5,
                    0.1,
                    pcep.verify_global_pcep_statistics,
                    expected_pcc_count=pcc_count,
                    expected_lsps_per_pcc_count=lsp_count,
                )

            with allure_step_with_separate_logging(
                f"step_stop_pcc_mock_with_{pcc_count}_pcc_{lsp_count}_lsps_"
                f"iteration_{iteration}"
            ):
                """Stop PCC mocks simulator."""
                pcep.stop_pcc_mock_process(self.pcc_mock_process)

        with allure_step_with_separate_logging("step_set_timer_value_back_to_default"):
            """Update timer value back to the original default value."""
            pcep.set_stat_timer_value(DEFAULT_PCEP_STATS_UPDATE_INTERVAL)
