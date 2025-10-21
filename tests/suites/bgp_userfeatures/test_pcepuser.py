#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import logging
import pytest
import time

from libraries import infra
from libraries import pcep
from libraries import templated_requests
from libraries import utils
from variables.pcepuser.titanium import variables as pcep_variables
from libraries.variables import variables


LOG_NAME = "pccmock.log"
ODL_IP = variables.ODL_IP
REST_API = variables.REST_API
TOOLS_IP = variables.TOOLS_IP
TOPOLOGY_URL = variables.TOPOLOGY_URL
PCEP_VARIABLES = pcep_variables.get_variables(TOOLS_IP)
PATH_SESSION_URI = (
    f"node=pcc:%2F%2F{TOOLS_IP}/network-topology-pcep:path-computation-client"
)
PCEP_VARIABLES_FOLDER = f"variables/pcepuser/titanium"

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=48)
class TestPcepUser:
    pcep_mock_process = None

    def compare_topology(self, exp, uri=None):
        """Get current pcep-topology as json, compare both expected and actual json.
        Error codes and normalized jsons should match exactly."""
        if uri:
            topology_uri = f"{TOPOLOGY_URL}=pcep-topology/{uri}?content=nonconfig"
        else:
            topology_uri = f"{TOPOLOGY_URL}=pcep-topology?content=nonconfig"
        resp = templated_requests.get_from_uri(topology_uri, expected_code=200)
        utils.verify_jsons_matach(resp.text, exp)

    def test_pcep_user(self, allure_step_with_separate_logging):

        with allure_step_with_separate_logging("step_topology_precondition"):
            """Compare current pcep-topology to empty pcep tology. Timeout is long
            enough to ODL boot, to see that pcep is ready, with no PCC is connected."""
            utils.wait_until_function_pass(300, 1, pcep.check_empty_pcep_topology)

        with allure_step_with_separate_logging("strep_start_pcc_mock"):
            """Execute pcc-mock, fail is Open is not sent, keep it running for next tests."""
            self.pcep_mock_process = infra.shell(
                f"java -jar build_tools/pcep-pcc-mock.jar --reconnect 1 --local-address {TOOLS_IP} --remote-address {ODL_IP} 2>&1 | tee tmp/{LOG_NAME}",
                run_in_background=True,
            )
            infra.read_until(self.pcep_mock_process, "started, sent proposal Open")
            time.sleep(1)

        with allure_step_with_separate_logging(
            "step_configure_speaker_entity_identifier"
        ):
            """Additional PCEP Speaker configuration. Allows PCEP speaker to determine if state synchronization can be skipped when a PCEP session is restarted."""
            mapping = {"IP": ODL_IP}
            templated_requests.put_templated_request(
                "variables/pcepuser/titanium/node_speaker_entity_identifier",
                mapping,
                json=False,
            )

        with allure_step_with_separate_logging("step_topology_default"):
            """Compare pcep-topology to default_json, which includes a tunnel from pcc-mock. Timeout is lower than in Precondition, as state from pcc-mock should be updated quickly."""
            default_json = PCEP_VARIABLES["default_json"]
            self.compare_topology(default_json, PATH_SESSION_URI)

        with allure_step_with_separate_logging("step_update_delegated"):
            """Perform update-lsp on the mocked tunnel, check response is success."""
            pcep.update_lsp(PCEP_VARIABLES["update_delegated_xml"])

        with allure_step_with_separate_logging("step_topology_updated"):
            """Compare pcep-topology to default_json, which includes the updated tunnel."""
            updated_json = PCEP_VARIABLES["updated_json"]
            self.compare_topology(updated_json, PATH_SESSION_URI)

        with allure_step_with_separate_logging("step_refuse_remove_delegated"):
            """Perform remove-lsp on the mocked tunnel, check that mock-pcc has refused to remove it."""
            resp = pcep.remove_lsp(PCEP_VARIABLES["remove_delegated_xml"])
            expected_response_raw = '{"network-topology-pcep:output":{"error":[{"error-object":{"ignore":false,"processing-rule":false,"type":19,"value":9}}],"failure":"failed"}}'
            utils.verify_jsons_matach(resp.text, expected_response_raw)

        with allure_step_with_separate_logging("step_topology_still_updated"):
            """Compare pcep-topology to default_json, which includes the updated tunnel, to verify that refusal did not break topology."""
            updated_json = PCEP_VARIABLES["updated_json"]
            self.compare_topology(updated_json, PATH_SESSION_URI)

        with allure_step_with_separate_logging("step_add_instantiated"):
            """Perform add-lsp to create new tunnel, check that response is success."""
            pcep.add_lsp(PCEP_VARIABLES["add_instantiated_xml"])

        with allure_step_with_separate_logging("step_topology_second_default"):
            """Compare pcep-topology to default_json, which includes the updated delegated and default instantiated tunnel."""
            updated_json = PCEP_VARIABLES["updated_default_json"]
            self.compare_topology(updated_json, PATH_SESSION_URI)

        with allure_step_with_separate_logging("step_update_instantiated"):
            """Perform update-lsp on the newly instantiated tunnel, check that response is success."""
            pcep.update_lsp(PCEP_VARIABLES["update_instantiated_xml"])

        with allure_step_with_separate_logging("step_topology_second_updated"):
            """Compare pcep-topology to default_json, which includes the updated delegated and updated instantiated tunnel."""
            updated_json = PCEP_VARIABLES["updated_updated_json"]
            self.compare_topology(updated_json, PATH_SESSION_URI)

        with allure_step_with_separate_logging("step_remove_instantiated"):
            """Perform remove-lsp on the instantiated tunnel, check that response is success."""
            pcep.remove_lsp(PCEP_VARIABLES["remove_instantiated_xml"])

        with allure_step_with_separate_logging("step_topology_again_updated"):
            """Compare pcep-topology to default_json, which includes the updated tunnel, to verify that instantiated tunnel was removed."""
            updated_json = PCEP_VARIABLES["updated_json"]
            self.compare_topology(updated_json, PATH_SESSION_URI)

        with allure_step_with_separate_logging("step_stop_pcc_mock"):
            """Send SIGINT to pcc-mock, fails if does not stop within 3 seconds."""
            pid = infra.get_children_processes_pids(self.pcep_mock_process, "java")[0]
            infra.stop_process_by_pid(pid, gracefully=True, timeout=3)

        with allure_step_with_separate_logging("step_topology_postcondition"):
            """Compare curent pcep-topology to "off_json" again."""
            pcep.check_empty_pcep_topology()
