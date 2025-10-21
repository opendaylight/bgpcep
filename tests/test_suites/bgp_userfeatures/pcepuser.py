#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import json
import logging
import os
import pytest
import time

from lib import bmp
from lib import infra
from lib import pcep
from lib import utils

from data.variables.default_pcep_topology import variables


LOG_NAME = "pccmock.log"
ODL_IP = os.environ["ODL_IP"]
TOOLS_IP = os.environ["TOOLS_IP"]
VARIABLES = variables.get_variables(TOOLS_IP)

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=15)
class TestPcepUser:
    pcep_mock_process = None

    @classmethod
    def verify_path_computation_client_data(cls, expected_topology: dict):
        def check_data():
            resp = pcep.get_path_computation_client()
            utils.verify_jsons_matach(resp.text,expected_topology)
        utils.wait_until_function_pass(10, 1, check_data)


    def test_pcep_user(self, allure_step_with_separate_logging):

        with allure_step_with_separate_logging("step_topology_precondition"):
            """Compare current pcep-topology to empty pcep tology. Timeout is long
            enough to ODL boot, to see that pcep is ready, with no PCC is connected."""
            utils.wait_until_function_pass(
                300, 1, pcep.check_empty_pcep_topology
            )

        with allure_step_with_separate_logging("strep_start_pcc_mock"):
            """Execute pcc-mock, fail is Open is not sent, keep it running for next tests."""
            self.pcep_mock_process = infra.shell(f"java -jar build_tools/pcep-pcc-mock.jar --reconnect 1 --local-address {TOOLS_IP} --remote-address {ODL_IP} 2>&1 | tee {LOG_NAME}", run_in_background=True)
            infra.read_until(self.pcep_mock_process, "started, sent proposal Open")
            time.sleep(1)

        with allure_step_with_separate_logging("step_configure_speaker_entity_identifier"):
            """Additional PCEP Speaker configuration. Allows PCEP speaker to determine if state synchronization can be skipped when a PCEP session is restarted."""
            pcep.configure_speaker_entitiy_identifier()

        with allure_step_with_separate_logging("step_topology_default"):
            """Compare pcep-topology to default_json, which includes a tunnel from pcc-mock. Timeout is lower than in Precondition, as state from pcc-mock should be updated quickly."""
            default_json = VARIABLES["default_json"]
            self.verify_path_computation_client_data(default_json)

        with allure_step_with_separate_logging("step_update_delegated"):
            """Perform update-lsp on the mocked tunnel, check response is success."""
            lsp_name = f"pcc_{TOOLS_IP}_tunnel_1"
            pcep.update_lsp(pcc_ip=TOOLS_IP, lsp_name=lsp_name)

        with allure_step_with_separate_logging("step_topology_updated"):
            """Compare pcep-topology to default_json, which includes the updated tunnel."""
            updated_json = VARIABLES["updated_json"]
            self.verify_path_computation_client_data(updated_json)

        with allure_step_with_separate_logging("step_refuse_remove_delegated"):
            """Perform remove-lsp on the mocked tunnel, check that mock-pcc has refused to remove it."""
            lsp_name = f"pcc_{TOOLS_IP}_tunnel_1"
            resp = pcep.remove_lsp(pcc_ip=TOOLS_IP, lsp_name=lsp_name)
            expected_response_raw = '{"network-topology-pcep:output":{"error":[{"error-object":{"ignore":false,"processing-rule":false,"type":19,"value":9}}],"failure":"failed"}}'
            utils.verify_jsons_matach(resp.text,expected_response_raw)

        with allure_step_with_separate_logging("step_topology_still_updated"):
            """Compare pcep-topology to default_json, which includes the updated tunnel, to verify that refusal did not break topology."""
            updated_json = VARIABLES["updated_json"]
            self.verify_path_computation_client_data(updated_json)

        with allure_step_with_separate_logging("step_add_instantiated"):
            """Perform add-lsp to create new tunnel, check that response is success."""
            lsp_name = "Instantiated tunnel"
            pcep.add_lsp(pcc_ip=TOOLS_IP, lsp_name=lsp_name)

        with allure_step_with_separate_logging("step_topology_second_default"):
            """Compare pcep-topology to default_json, which includes the updated delegated and default instantiated tunnel."""
            updated_json = VARIABLES["updated_default_json"]
            self.verify_path_computation_client_data(updated_json)

        with allure_step_with_separate_logging("step_update_instantiated"):
            """Perform update-lsp on the newly instantiated tunnel, check that response is success."""
            lsp_name = "Instantiated tunnel"
            pcep.update_lsp(pcc_ip=TOOLS_IP, lsp_name=lsp_name)

        with allure_step_with_separate_logging("step_topology_second_updated"):
            """Compare pcep-topology to default_json, which includes the updated delegated and updated instantiated tunnel."""
            updated_json = VARIABLES["updated_updated_json"]
            self.verify_path_computation_client_data(updated_json)

        with allure_step_with_separate_logging("step_remove_instantiated"):
            """Perform remove-lsp on the instantiated tunnel, check that response is success."""
            lsp_name = "Instantiated tunnel"
            pcep.remove_lsp(pcc_ip=TOOLS_IP, lsp_name=lsp_name)

        with allure_step_with_separate_logging("step_topology_again_updated"):
            """Compare pcep-topology to default_json, which includes the updated tunnel, to verify that instantiated tunnel was removed."""
            updated_json = VARIABLES["updated_json"]
            self.verify_path_computation_client_data(updated_json)

        with allure_step_with_separate_logging("step_stop_pcc_mock"):
            """Send SIGINT to pcc-mock, fails if does not stop within 3 seconds."""
            pcep.stop_pcc_mock_process(self.pcep_mock_process, timeout=3)

        with allure_step_with_separate_logging("step_topology_postcondition"):
            """Compare curent pcep-topology to "off_json" again."""
            pcep.check_empty_pcep_topology()
