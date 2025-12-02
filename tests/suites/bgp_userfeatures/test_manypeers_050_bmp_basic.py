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

from libraries import bmp
from libraries import infra
from libraries import templated_requests
from libraries import utils
from libraries.variables import variables


BMP_ROUTERS_COUNT = 1
REPORTED_PEERS_COUNT = 20

TOOLS_IP = variables.TOOLS_IP
BGP_BMP_DIR = "variables/bgpfunctional/bmp_basic/filled_structure_manypeers"
BGP_BMP_FEAT_DIR = "variables/bgpfunctional/bmp_basic/empty_structure"
BMP_LOG_FILE = "bmpmock.log"

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=38)
class TestBmpBasic:
    bmp_mock_process = None

    def test_bmp_basic(self, allure_step_with_separate_logging):

        with allure_step_with_separate_logging("step_verify_BMP_feature"):
            """Verifies if feature is up."""
            utils.wait_until_function_pass(
                180,
                5,
                templated_requests.get_templated_request,
                BGP_BMP_FEAT_DIR,
                mapping=None,
                json=True,
                verify=True,
            )

        with allure_step_with_separate_logging("step_start_bmp_mock"):
            """Starts bmp-mock."""
            self.bmp_mock_process = bmp.start_bmp_mock(
                routers_count=BMP_ROUTERS_COUNT, peers_count=REPORTED_PEERS_COUNT
            )

        with allure_step_with_separate_logging("step_verify_data_reported"):
            """Verifies if the tool reported expected data."""
            mapping = {"TOOL_IP": TOOLS_IP}
            utils.wait_until_function_pass(
                3,
                2,
                templated_requests.get_templated_request,
                BGP_BMP_DIR,
                mapping=mapping,
                json=True,
                verify=True,
            )

        with allure_step_with_separate_logging("step_stop_bmp_mock"):
            """Send ctrl+c to bmp-mock to stop it."""
            bmp.stop_bmp_mock_process(self.bmp_mock_process)

        with allure_step_with_separate_logging("step_archive_bgp_bmp_mock_logs"):
            """Archives bgp bmp mock tool log ouput."""
            infra.shell(f"mv tmp/{BMP_LOG_FILE} results/{BMP_LOG_FILE}")
