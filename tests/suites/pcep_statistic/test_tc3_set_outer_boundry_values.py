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

from libraries import pcep
from libraries.variables import variables


BELOW_MIN_TIMER_VALUE = 0
ABOVE_MAX_TIMER_VALUE = 65536
DEFAULT_PCEP_STATS_UPDATE_INTERVAL = variables.DEFAULT_PCEP_STATS_UPDATE_INTERVAL
ODL_IP = variables.ODL_IP
TOOLS_IP = variables.TOOLS_IP

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.run(order=79)
class TestPcepUser:
    pcc_mock_process = None

    @allure.description(
        """Tests that PCEP statistics timer can not be updated to values outside
        allowed range. It tries to set the timer value below min and above max
        values and expects a failure response from ODL.""")
    def test_default_update_interval(self, allure_step_with_separate_logging):

        with allure_step_with_separate_logging("step_set_timer_value_below_min"):
            """Update timer to a value below lowest allowed value."""
            pcep.set_stat_timer_value(BELOW_MIN_TIMER_VALUE, expected_response_code=400)

        with allure_step_with_separate_logging("step_set_timer_value_above_max"):
            """Update timer to a value above highest allowed value."""
            pcep.set_stat_timer_value(ABOVE_MAX_TIMER_VALUE, expected_response_code=400)

        with allure_step_with_separate_logging("step_set_timer_value_back_to_default"):
            """Update timer value back to the original default value."""
            pcep.set_stat_timer_value(DEFAULT_PCEP_STATS_UPDATE_INTERVAL)
