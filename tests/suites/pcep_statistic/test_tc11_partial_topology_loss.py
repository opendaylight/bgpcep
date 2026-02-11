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

from libraries import pcep
from libraries import utils
from libraries.variables import variables


DEFAULT_PCEP_STATS_UPDATE_INTERVAL = variables.DEFAULT_PCEP_STATS_UPDATE_INTERVAL
SIMULATOR_1_PCC_COUNT = 10
SIMULATOR_1_LSP_PER_PCC_COUNT = 10
SIMULATOR_1_IP = "127.0.1.0"
SIMULATOR_2_PCC_COUNT = 5
SIMULATOR_2_LSP_PER_PCC_COUNT = 5
SIMULATOR_2_IP = "127.0.2.0"
ODL_IP = variables.ODL_IP
TOOLS_IP = variables.TOOLS_IP

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_pcep_pcc_mock_processes")
@pytest.mark.run(order=87)
class TestPcepUser:
    pcc_mock_process_1 = None
    pcc_mock_process_2 = None

    @allure.description(
        textwrap.dedent("""
            **Ensure system properly functioning with lost partial topology**

            Verifies that PCEP statistics update correctly when a portion \
            of the topology (Simulator 2) is disconnected.
        """)
    )
    def test_partial_topology_loss(self, allure_step_with_separate_logging):
        with allure_step_with_separate_logging("step_set_timer_value_to_5_second"):
            """Update timer value value."""
            pcep.set_stat_timer_value(5)

        with allure_step_with_separate_logging(
            f"step_start_first_pcc_mock_{SIMULATOR_1_PCC_COUNT}_pcc_"
            f"{SIMULATOR_1_LSP_PER_PCC_COUNT}_lsps"
        ):
            """Starts first PCC mocks simulator."""
            self.pcc_mock_process_1 = pcep.start_pcc_mock(
                pcc=SIMULATOR_1_PCC_COUNT,
                lsp=SIMULATOR_1_LSP_PER_PCC_COUNT,
                local_address=SIMULATOR_1_IP,
                remote_address=ODL_IP,
                verify_introduced_lsps=False,
            )

        with allure_step_with_separate_logging(
            f"step_start_second_pcc_mock_{SIMULATOR_2_PCC_COUNT}_pcc_"
            f"{SIMULATOR_2_LSP_PER_PCC_COUNT}_lsps"
        ):
            """Starts second PCC mocks simulator."""
            self.pcc_mock_process_2 = pcep.start_pcc_mock(
                pcc=SIMULATOR_2_PCC_COUNT,
                lsp=SIMULATOR_2_LSP_PER_PCC_COUNT,
                local_address=SIMULATOR_2_IP,
                remote_address=ODL_IP,
                verify_introduced_lsps=False,
            )

        with allure_step_with_separate_logging(f"step_topology_show_all_pcc_and_lsp"):
            """Wait until all PCC devices and reported LSPs are shown."""
            pcep.wait_until_concrete_number_of_lsps_reported(
                SIMULATOR_1_PCC_COUNT * SIMULATOR_1_LSP_PER_PCC_COUNT
                + SIMULATOR_2_PCC_COUNT * SIMULATOR_2_LSP_PER_PCC_COUNT,
                interval=0.1,
            )

        with allure_step_with_separate_logging(f"step_wait_5_seconds_for_inital_stats"):
            """Wait one second until the next pcep stat update."""
            time.sleep(5)

        with allure_step_with_separate_logging(
            f"step_verify_stats_conains_devices_from_both_simulators"
        ):
            """Verifies that get-stat RPC does return correct statistics containing
            all PCC devices and all reported LSPs from both simulators and no extra
            nodes."""
            utils.wait_until_function_pass(
                5,
                0.1,
                pcep.verify_global_pcep_statistics,
                expected_pcc_count=SIMULATOR_1_PCC_COUNT + SIMULATOR_2_PCC_COUNT,
            )
            pcep.verify_statsistics_contains_pcc_mock_data(
                expected_pcc_count=SIMULATOR_1_PCC_COUNT,
                expected_lsps_per_pcc_count=SIMULATOR_1_LSP_PER_PCC_COUNT,
                first_pcc_ip=SIMULATOR_1_IP,
            )
            pcep.verify_statsistics_contains_pcc_mock_data(
                expected_pcc_count=SIMULATOR_2_PCC_COUNT,
                expected_lsps_per_pcc_count=SIMULATOR_2_LSP_PER_PCC_COUNT,
                first_pcc_ip=SIMULATOR_2_IP,
            )

        with allure_step_with_separate_logging(
            f"step_stop_second_pcc_mock_{SIMULATOR_2_PCC_COUNT}_pcc_"
            f"{SIMULATOR_2_LSP_PER_PCC_COUNT}_lsps"
        ):
            """Stop second PCC mocks simulator."""
            pcep.stop_pcc_mock_process(self.pcc_mock_process_2)

        with allure_step_with_separate_logging(
            f"step_topology_show_only_first_simulator_pcc_and_lsp"
        ):
            """Wait until only first simulator PCC devices and reported LSPs
            are shown."""
            pcep.wait_until_concrete_number_of_lsps_reported(
                SIMULATOR_1_PCC_COUNT * SIMULATOR_1_LSP_PER_PCC_COUNT, interval=0.1
            )

        with allure_step_with_separate_logging(f"step_wait_1_second"):
            """Wait one second, before the next statistics update occure."""
            time.sleep(1)

        with allure_step_with_separate_logging(
            f"step_verify_stats_not_yet_synced_after_stopping_simulator"
        ):
            """Verifies that PCEP statistics does not yet reflect changes in
            PCEP topology, lost connection to the second simulator."""
            utils.verify_function_never_passes_within_timeout(
                5,
                0.1,
                pcep.verify_global_pcep_statistics,
                expected_pcc_count=SIMULATOR_1_PCC_COUNT,
                expected_lsps_per_pcc_count=1,
            )

        with allure_step_with_separate_logging(
            f"step_topology_show_first_simulator_pcc_and_lsp"
        ):
            """Wait until only the first simulator PCC devices and reported LSPs
            are shown."""
            pcep.wait_until_concrete_number_of_lsps_reported(
                SIMULATOR_1_PCC_COUNT * SIMULATOR_1_LSP_PER_PCC_COUNT, interval=0.1
            )

        with allure_step_with_separate_logging(
            f"step_wait_5_seconds_for_updated_stats"
        ):
            """Wait five second until the next PCEP stat update."""
            time.sleep(5)

        with allure_step_with_separate_logging(
            f"step_verify_stats_conains_devices_from_first_simulator_only"
        ):
            """Verifies that get-stat RPC does return correct statistics containing
            only PCC devices and LSPs from first simulator."""
            utils.wait_until_function_pass(
                5,
                0.1,
                pcep.verify_global_pcep_statistics,
                expected_pcc_count=SIMULATOR_1_PCC_COUNT,
                expected_lsps_per_pcc_count=SIMULATOR_1_LSP_PER_PCC_COUNT,
            )

        with allure_step_with_separate_logging(
            f"step_stop_first_pcc_mock_{SIMULATOR_2_PCC_COUNT}_pcc_"
            f"{SIMULATOR_2_LSP_PER_PCC_COUNT}_lsps"
        ):
            """Stop first PCC mocks simulator."""
            pcep.stop_pcc_mock_process(self.pcc_mock_process_1)

        with allure_step_with_separate_logging("step_set_timer_value_back_to_default"):
            """Update timer value back to the original default value."""
            pcep.set_stat_timer_value(DEFAULT_PCEP_STATS_UPDATE_INTERVAL)
