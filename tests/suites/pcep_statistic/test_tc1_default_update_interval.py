#
# Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import logging
import textwrap

import allure
import pytest
import time

from libraries import infra
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
@pytest.mark.run(order=77)
class TestPcepUser:
    pcc_mock_process = None

    @allure.description(
        textwrap.dedent("""
            **Ensure statistics update interval is set to 5 seconds by default**

            Tests that PCEP statistics are updated strictly after the default \
            timer elapses. It verifies statistics are not updated before \
            the timer expires, but right after.
        """)
    )
    def test_default_update_interval(self, allure_step_with_separate_logging):

        with allure_step_with_separate_logging(
            "step_change_karaf_logging_levels_to_debug"
        ):
            """To be able to track time of the last pcep stats update we
            need to check it in logs with at least DEBUG level."""
            infra.execute_karaf_command(f"log:set DEBUG org.opendaylight.bgpcep")
            infra.execute_karaf_command(f"log:set DEBUG org.opendaylight.protocol")

        with allure_step_with_separate_logging("step_start_pcc_mock"):
            """Starts PCC mocks simulator."""
            self.pcc_mock_process = pcep.start_pcc_mock(
                pcc=1,
                lsp=1,
                local_address=TOOLS_IP,
                remote_address=ODL_IP,
                verify_introduced_lsps=True,
                verify_interval=0.1,
            )

        with allure_step_with_separate_logging(
            f"step_wait_{DEFAULT_PCEP_STATS_UPDATE_INTERVAL - 1}_seconds"
        ):
            """Wait until one second before the next pcep stat update."""
            time.sleep(DEFAULT_PCEP_STATS_UPDATE_INTERVAL - 1)

        with allure_step_with_separate_logging("step_verfiy_stats_are_not_present"):
            """Verifies that get-stat RPC does not return any statistics."""
            pcep.verify_odl_does_not_return_statistics_for_pcc(pcc_ip=TOOLS_IP)

        with allure_step_with_separate_logging("step_wait_1_second"):
            """Wait for 1 second."""
            time.sleep(1)

        with allure_step_with_separate_logging("step_verfiy_stats_are_present"):
            """Verifies that get-stat RPC does return statistics."""
            utils.wait_until_function_pass(
                5, 0.1, pcep.get_statistics, pcc_ip=TOOLS_IP, verify_response=True
            )

        with allure_step_with_separate_logging("step_stop_pcc_mock"):
            """Stop PCC mocks simulator."""
            pcep.stop_pcc_mock_process(self.pcc_mock_process)

        with allure_step_with_separate_logging(
            "step_change_karaf_logging_levels_to_default"
        ):
            """Return logging level back to default INFO value."""
            infra.execute_karaf_command(f"log:set INFO org.opendaylight.bgpcep")
            infra.execute_karaf_command(f"log:set INFO org.opendaylight.protocol")
