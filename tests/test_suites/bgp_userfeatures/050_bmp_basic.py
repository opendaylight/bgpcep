#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
#
# This is a basic test for bgp monitoring protocol feature. After the feature
# odl-bgpcep-bmp installation, the port 12345 should be bound for listening. 
# To test this feature bgp-bmp-mock tool is used. It is a part of the bgpcep project.
# It is a java tool which simulates more peers and more routers. In this particular
# test suite it simulates 1 peer with 1 router, which means it advertizes one peer
# ipv4 address towards odl. As a result one route should appear in the
# rests/data/bmp-monitor:bmp-monitor?content=nonconfig.

import logging
import pytest

from lib import bmp
from lib import infra
from lib import utils


BMP_LOG_FILE = "bmpmock.log"

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=14)
class TestBmpBasic:
    bmp_mock_process = None

    def test_bmp_basic(self, allure_step_with_separate_logging):

        with allure_step_with_separate_logging("step_verify_BMP_feature"):
            """Verifies if feature is up."""
            utils.wait_until_function_pass(
                36, 5, bmp.check_empty_bmp_monitor
            )

        with allure_step_with_separate_logging("step_start_bmp_mock"):
            """Starts bmp-mock."""
            self.bmp_mock_process = bmp.start_bmp_mock(routers_count=1, peers_count=1)

        with allure_step_with_separate_logging("step_verify_data_reported"):
            """Verifies if the tool reported expected data."""
            utils.wait_until_function_pass(
                3, 2, bmp.check_bmp_monitor_filled
            )

        with allure_step_with_separate_logging("step_stop_bmp_mock"):
            """Send ctrl+c to bmp-mock to stop it."""
            bmp.stop_bmp_mock_process(self.bmp_mock_process)

        with allure_step_with_separate_logging("step_achive_bgp_bmp_mock_logs"):
            """Archives bgp bmp mock tool log ouput."""
            infra.shell(f"mv tmp/{BMP_LOG_FILE} results/{BMP_LOG_FILE}")
