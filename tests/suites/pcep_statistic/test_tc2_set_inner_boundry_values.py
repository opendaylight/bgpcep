#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# Tests that PCEP statistics timer can be updated to valid boundary values.
# It tries to set the minimal and maximal allowed values and expects a success
# response from ODL.

import logging

import pytest

from libraries import pcep
from libraries.variables import variables


MIN_TIMER_VALUE = 1
MAX_TIMER_VALUE = 65535
DEFAULT_PCEP_STATS_UPDATE_INTERVAL = variables.DEFAULT_PCEP_STATS_UPDATE_INTERVAL
ODL_IP = variables.ODL_IP
TOOLS_IP = variables.TOOLS_IP

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.run(order=78)
class TestPcepUser:
    pcc_mock_process = None

    def test_set_inner_boundry_values(self, allure_step_with_separate_logging):

        with allure_step_with_separate_logging("step_set_timer_value_to_lowest_possible"):
            """Update timer value to lowest possible value."""
            pcep.set_stat_timer_value(MIN_TIMER_VALUE)

        with allure_step_with_separate_logging("step_set_timer_value_to_highest_possible"):
            """Update timer value to highest possible value."""
            pcep.set_stat_timer_value(MAX_TIMER_VALUE)

        with allure_step_with_separate_logging("step_set_timer_value_back_to_default"):
            """Update timer value back to the original default value."""
            pcep.set_stat_timer_value(DEFAULT_PCEP_STATS_UPDATE_INTERVAL)
