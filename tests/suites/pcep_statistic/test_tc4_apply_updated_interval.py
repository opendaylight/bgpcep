#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
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


CUSTOM_TIMER_VALUE = 1
DEFAULT_PCEP_STATS_UPDATE_INTERVAL = variables.DEFAULT_PCEP_STATS_UPDATE_INTERVAL
ODL_IP = variables.ODL_IP
TOOLS_IP = variables.TOOLS_IP

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_pcep_pcc_mock_processes")
@pytest.mark.run(order=80)
class TestPcepUser:
    pcc_mock_process = None

    @allure.description(
        "**Ensure interval parameters are applied when PCC is connected**\n"
        "\n"
        "Tests dynamic reconfiguration of the PCEP statistics timer value. "
        "Verifies that when the timer value is updated, the new interval "
        "between statistics updates is applied immediately. It validates "
        "that the next statistics update occurs successfully after "
        "the new timer value elapses."
    )
    def test_apply_updated_interval(self, allure_step_with_separate_logging):

        with allure_step_with_separate_logging(
            f"step_set_timer_to_custom_value_{CUSTOM_TIMER_VALUE}"
        ):
            """Update timer value to the new custom value."""
            pcep.set_stat_timer_value(CUSTOM_TIMER_VALUE)

        with allure_step_with_separate_logging("step_start_pcc_mock"):
            """Starts PCC mocks simulator and verify connected."""
            self.pcc_mock_process = pcep.start_pcc_mock(
                pcc=1,
                lsp=1,
                local_address=TOOLS_IP,
                remote_address=ODL_IP,
                verify_introduced_lsps=True,
                verify_interval=0.1,
            )

        with allure_step_with_separate_logging(
            f"step_wait_{CUSTOM_TIMER_VALUE}_second"
        ):
            """Wait for custom timer value to elapse."""
            time.sleep(CUSTOM_TIMER_VALUE)

        with allure_step_with_separate_logging("step_verfiy_stats_are_present"):
            """Verifies that get-stat RPC does return statistics."""
            pcep.get_stats(pcc_ip=TOOLS_IP, verify_response=True)

        with allure_step_with_separate_logging("step_stop_pcc_mock"):
            """Stop PCC mocks simulator."""
            pcep.stop_pcc_mock_process(self.pcc_mock_process)

        with allure_step_with_separate_logging("step_set_timer_value_back_to_default"):
            """Update timer value back to the original default value."""
            pcep.set_stat_timer_value(DEFAULT_PCEP_STATS_UPDATE_INTERVAL)
