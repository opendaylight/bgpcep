#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# TCPMD5 user-facing feature system tests, using PCEP.
#
# Test suite performs basic pcep md5 password authorization test cases:
# (Run entire basic PCEP suite without passwords.)
# Start pcc-mock (reconnecting mode): 1 pcc, 1 lsp, password set,
# check pcep-topology stays empty.
# Use restconf to change PCEP configuration to use a wrong password,
# check pcep-topology stays empty.
# Change ODL PCEP configuration to use the correct password,
# check pcep-topology shows the lsp.
# Stop pcc-mock, check pcep-topology stays empty.
# Start pcc-mock with new password, check pcep-topology stays empty.
# Change ODL PCEP configuration to use the correct password,
# check pcep-topology shows the lsp.
# Update the lsp, check a change in pcep-topology.
# Change ODL PCEP configuration to not use password, pcep-topology empties,
# kill pcep-pcc-mock.
#
# Test cases no longer need netconf-connector-ssh, and they include comparison of
# pcep-session-state.

import logging
import pytest

from libraries import infra
from libraries import pcep
from libraries import utils
from libraries import templated_requests
from variables.tcpmd5user.titanium import variables as tcpmd5_variables
from libraries.variables import variables


LOG_NAME = "pccmock.log"
ODL_IP = variables.ODL_IP
TOOLS_IP = variables.TOOLS_IP
TCP_MD5_VARIABLES = tcpmd5_variables.get_variables(TOOLS_IP)
DIR_WITH_TEMPLATES = "variables/tcpmd5user/titanium"
ERROR_ARGS = ""

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=15)
class TestTcpMd5User:
    pcep_mock_process = None

    def tear_down(self):
        rc, output = infra.shell("cat tmp/pccmock.log")
        log.info(output)

    def check_unathorized(self):
        """Try to access pcep topology with wrong password, should get
        empty topology."""
        templated_requests.get_templated_request(
            f"{DIR_WITH_TEMPLATES}/default_off", None, verify=True
        )

    def replace_password_on_pcep_node(self, password):
        """Send restconf PUT to replace the config module specifying PCEP password element."""
        mapping = {"IP": TOOLS_IP, "PASSWD": password}
        templated_requests.put_templated_request(
            f"{DIR_WITH_TEMPLATES}/pcep_topology_node", mapping, json=False
        )

    def unset_password_on_pcep_node(self):
        """Send restconf PUT to unset the config module."""
        mapping = {"IP": TOOLS_IP}
        templated_requests.put_templated_request(
            f"{DIR_WITH_TEMPLATES}/pcep_topology_node_empty", mapping, json=False
        )

    def start_pcc_mock_tool_with_password(self, password):
        """Starts pcc-mock with password argument."""
        self.pcep_mock_process = infra.shell(
            f"java -jar build_tools/pcep-pcc-mock.jar --log-level debug --password {password} --reconnect 1 --local-address {TOOLS_IP} --remote-address {ODL_IP} 2>&1 | tee pccmock.log",
            run_in_background=True,
        )

    def stop_pcc_mock(self):
        output = self.pcep_mock_process.stdout
        log.debug(f"Pcc mock process output: {output=}")
        pid = infra.get_children_processes_pids(self.pcep_mock_process, "java")[0]
        log.info(f"Killing pcc mock process with PID {pid}")
        infra.stop_process_by_pid(pid, gracefully=True)

    def test_tcp_md5_user(self, allure_step_with_separate_logging):

        with allure_step_with_separate_logging("step_topology_precondition"):
            """Compare current pcep-topology to empty pcep tology. Timeout is long
            enough to ODL boot, to see that pcep is ready, with no PCC is connected."""
            utils.wait_until_function_pass(
                300,
                1,
                templated_requests.get_templated_request,
                f"{DIR_WITH_TEMPLATES}/default_off",
                None,
                verify=True,
            )

        with allure_step_with_separate_logging("step_start_secure_pcc_mock"):
            """Execute pcc-mock with password set, fail if pcc-mock promptly exits.
            Keep pcc-mock running for next test cases."""
            self.start_pcc_mock_tool_with_password(password="topsecret")

        with allure_step_with_separate_logging("step_topology_unauthorized_1"):
            """Try to catch a glimpse of pcc-mock in pcep-topology.
            Pass if no change from Precondition is detected over 10 seconds."""
            utils.wait_until_function_pass(10, 1, self.check_unathorized)

        with allure_step_with_separate_logging("step_set_wrong_password"):
            """The same logic as step_topology_unauthorized_1 as incorrect password was provided to ODL."""
            self.replace_password_on_pcep_node(password="changeme")

        with allure_step_with_separate_logging("step_topology_unauthorized_2"):
            """The same logic as topology_unauthorized_1 as incorrect password was provided to ODL."""
            utils.wait_until_function_pass(10, 1, self.check_unathorized)

        with allure_step_with_separate_logging("step_set_correct_password"):
            """Configure password in pcep dispatcher for client with Mininet IP address.
            This password finally matches what pcc-mock uses."""
            self.replace_password_on_pcep_node(password="topsecret")

        with allure_step_with_separate_logging("step_topology_intercondition"):
            """Compare pcep-topology/path-computation-client to filled one,
            which includes a tunnel from pcc-mock."""
            mapping = {
                "IP": TOOLS_IP,
                "CODE": TCP_MD5_VARIABLES["pcc_name_code"],
                "NAME": TCP_MD5_VARIABLES["pcc_name"],
                "IP_ODL": ODL_IP,
                "ERRORS": ERROR_ARGS,
            }
            utils.wait_until_function_pass(
                30,
                1,
                templated_requests.get_templated_request,
                f"{DIR_WITH_TEMPLATES}/default_on_state",
                mapping,
                verify=True,
            )

        with allure_step_with_separate_logging("step_stop_pcc_mock_1"):
            """Stops First instance of pcc-mock."""
            self.stop_pcc_mock()

        with allure_step_with_separate_logging("step_topology_unauthorized_3"):
            """The same logic as Topology_Unauthorized_1, with no pcc-mock running."""
            utils.wait_until_function_pass(10, 1, self.check_unathorized)

        with allure_step_with_separate_logging("step_start_secure_pcc_mock_2"):
            """Execute pcc-mock on Mininet with new password set, fail if pcc-mock promptly exits.
            Keep pcc-mock running for next test cases."""
            self.start_pcc_mock_tool_with_password(password="newtopsecret")

        with allure_step_with_separate_logging("step_topology_unauthorized_4"):
            """The same logic as Topology_Unauthorized_1, but ODL password became incorrect with new pcc-mock running."""
            utils.wait_until_function_pass(10, 1, self.check_unathorized)

        with allure_step_with_separate_logging("step_correct_password_2"):
            """Configure password in pcep dispatcher.
            This password again matches what second pcc-mock instance uses."""
            self.replace_password_on_pcep_node(password="changeme")

        with allure_step_with_separate_logging("step_topology_intercondition_2"):
            """Compare pcep-topology/path-computation-client to filled one,
            which includes a tunnel from pcc-mock."""
            mapping = {
                "IP": TOOLS_IP,
                "CODE": TCP_MD5_VARIABLES["pcc_name_code"],
                "NAME": TCP_MD5_VARIABLES["pcc_name"],
                "IP_ODL": ODL_IP,
                "ERRORS": ERROR_ARGS,
            }
            utils.wait_until_function_pass(
                30,
                1,
                templated_requests.get_templated_request,
                f"{DIR_WITH_TEMPLATES}/default_on_state",
                mapping,
                verify=True,
            )

        with allure_step_with_separate_logging("step_update_delegated"):
            """Perform update-lsp on the mocked tunnel, check response is success."""
            mapping = {"IP": TOOLS_IP, "NAME": TCP_MD5_VARIABLES["pcc_name"]}
            response = templated_requests.post_templated_request(
                f"{DIR_WITH_TEMPLATES}/update_delegated", mapping, json=False
            )
            log.info(response.text)

        with allure_step_with_separate_logging("step_topology_updated"):
            """Compare pcep-topology/path-computation-client to default_on_updated,
            which includes the updated tunnel."""
            mapping = {
                "IP": TOOLS_IP,
                "CODE": TCP_MD5_VARIABLES["pcc_name_code"],
                "NAME": TCP_MD5_VARIABLES["pcc_name"],
                "IP_ODL": ODL_IP,
                "ERRORS": ERROR_ARGS,
            }
            utils.wait_until_function_pass(
                30,
                1,
                templated_requests.get_templated_request,
                f"{DIR_WITH_TEMPLATES}/default_on_updated_state",
                mapping,
                verify=True,
            )

        with allure_step_with_separate_logging("step_Unset_Password"):
            """De-configure password for pcep dispatcher."""
            self.unset_password_on_pcep_node()

        with allure_step_with_separate_logging("step_stop_pcc_mock_2"):
            self.stop_pcc_mock()

        with allure_step_with_separate_logging("step_topology_postcondition"):
            """Verify that pcep-topology stays empty."""
            utils.verify_function_does_not_fail_within_timeout(
                10, 1, self.check_unathorized
            )

        with allure_step_with_separate_logging("step_delete_pcep_client_module"):
            """Delete Pcep client module."""
            mapping = {"IP": TOOLS_IP}
            templated_requests.delete_templated_request(
                f" {DIR_WITH_TEMPLATES}/pcep_topology_node", mapping, json=False
            )
