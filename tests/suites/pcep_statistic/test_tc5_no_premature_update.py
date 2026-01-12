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


CUSTOM_TIMER_VALUE = 65535
DEFAULT_PCEP_STATS_UPDATE_INTERVAL = variables.DEFAULT_PCEP_STATS_UPDATE_INTERVAL
ODL_IP = variables.ODL_IP
TOOLS_IP = variables.TOOLS_IP

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.run(order=80)
class TestPcepUser:
    pcc_mock_process = None

    @allure.description(
        "**Ensure statistics are not updated before interval expiration**\n"
        "\n"
        "Tests the dynamic reconfiguration of PCEP statistics timer value. "
        "Verifies that when the timer value is increased, the statistics "
        "are not updated before the newly defined interval elapses."
    )
    def test_no_premature_update(self, allure_step_with_separate_logging):

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

        with allure_step_with_separate_logging(f"step_wait_5_seconds"):
            """Wait for short duration, shorter then the CUSTOM_TIMER_VALUE."""
            time.sleep(5)

        with allure_step_with_separate_logging("step_verfiy_stats_are_not_present"):
            """Verifies that get-stat RPC does return statistics."""
            pcep.verify_odl_does_not_return_stats_for_pcc(pcc_ip=TOOLS_IP)

        with allure_step_with_separate_logging("step_stop_pcc_mock"):
            """Stop PCC mocks simulator."""
            pcep.stop_pcc_mock_process(self.pcc_mock_process)

        with allure_step_with_separate_logging("step_set_timer_value_back_to_default"):
            """Update timer value back to the original default value."""
            pcep.set_stat_timer_value(DEFAULT_PCEP_STATS_UPDATE_INTERVAL)
