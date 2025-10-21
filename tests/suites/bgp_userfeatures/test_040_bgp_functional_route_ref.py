#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# Test suite performs basic BGP functional test cases for BGP application
# peer operations and checks for IP4 topology updates and updates towards
# BGP peer as follows:
#
# Functional test for bgp - route refresh
#
# This suite tests sending and receiveing route refresh message.
# It uses odl and exabgp as bgp peers. Sending route refresh message
# from odl is initiated via restconf. If route refresh is received by odl,
# correct advertising of routes is verified. Receiving of route refresh
# by odl is verified by checking appropriate message counter via
# odl-bgpcep-bgp-cli and restconf using BGP neighbor operational state.

import logging
import pytest

from libraries import bgp
from libraries import infra
from libraries import templated_requests
from libraries import utils
from libraries.variables import variables


ODL_IP = variables.ODL_IP
TOOLS_IP = variables.TOOLS_IP
BGP_RPC_CLIENT = bgp.BgpRpcClient(TOOLS_IP)
APP_PEER_NAME = "example-bgp-peer-app"
BGP_CFG_NAME = "exa.cfg"
BGP_PEER_NAME = "example-bgp-peer"
BGP_VAR_FOLDER = "variables/bgpfunctional"
BGP_RR_VAR_FOLDER = f"{BGP_VAR_FOLDER}/route_refresh"
DEVICE_NAME = f"controller-config"
EXARPCSCRIPT = "tools/exabgp_files/exarpc.py"
HOLDTIME = 180
RIB_INSTANCE = "example-bgp-rib"
PROTOCOL_OPENCONFIG = RIB_INSTANCE
MSG_STATE_OFFSET = 24

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=51)
class TestBgpfunctionalRouteRef:
    exabgp_process = None

    def setup_config_files(self):
        """Copies exabgp config files."""
        rc, output = infra.shell("which python")
        log.warn(output)
        infra.shell(f"cp {BGP_VAR_FOLDER}/{BGP_CFG_NAME} tmp/")
        infra.shell(f"cp {EXARPCSCRIPT} tmp/")
        infra.shell(f"sed -i -e 's/EXABGPIP/{TOOLS_IP}/g' tmp/{BGP_CFG_NAME}")
        infra.shell(f"sed -i -e 's/ODLIP/{ODL_IP}/g' tmp/{BGP_CFG_NAME}")
        infra.shell(f"sed -i -e 's/ROUTEREFRESH/enable/g' tmp/{BGP_CFG_NAME}")
        infra.shell(f"sed -i -e 's/ADDPATH/disable/g' tmp/{BGP_CFG_NAME}")
        rc, stdout = infra.shell(f"cat tmp/{BGP_CFG_NAME}")
        log.info(stdout)

    def verify_exaBgp_received_updates(self, exp_count: int):
        """Gets number of received update requests and compares with given expected count"""
        count_recv = int(BGP_RPC_CLIENT.exa_get_received_update_count())
        assert count_recv == exp_count

    def verify_exaBgp_received_route_refresh(self, exp_count: int):
        """Compares expected count of route request messages on exabgp side"""
        count_recv = int(BGP_RPC_CLIENT.exa_get_received_route_refresh_count())
        assert count_recv == exp_count

    def verify_cli_output_count(self, notification_count, update_count, receive_count):
        """Checks notification and update count from odl-bgpcep-bgp-cli.
        odl-bgpcep-bgp-cli is only avaiable on versions oxygen and above."""
        stdout, stderror = infra.execute_karaf_command(
            f"bgp:operational-state -rib example-bgp-rib -neighbor ${TOOLS_IP}"
        )
        log.info(f"Karaf stdout: {stdout}")
        mapping = {
            "IP": TOOLS_IP,
            "NOT_COUNT": notification_count,
            "SEND_COUNT": update_count,
            "RECV_COUNT": receive_count,
            "DIVIDER": "|",
        }
        exp_state = templated_requests.resolve_templated_text(
            f"{BGP_RR_VAR_FOLDER}/operational_cli/update.txt", mapping
        )
        log.info(f"Expected state: {exp_state}")
        line_count = exp_state.find("\n") + 1
        real_state = "\n".join(
            stdout.splitlines()[MSG_STATE_OFFSET : MSG_STATE_OFFSET + line_count]
        )
        utils.verify_multiline_text_match(exp_state, real_state)

    def verify_odl_operational_state_count(
        self, notification_count: int, update_count: int, receive_count: int
    ):
        """Check notification and update count gained from operatial neighbor state
        It verifies these counts also against cli output."""
        mapping = {
            "IP": TOOLS_IP,
            "RIB_INSTANCE_NAME": RIB_INSTANCE,
            "NOT_COUNT": notification_count,
            "SEND_COUNT": update_count,
            "RECV_COUNT": receive_count,
        }
        utils.wait_until_function_pass(
            3,
            5,
            templated_requests.get_templated_request,
            f"{BGP_RR_VAR_FOLDER}/operational_state",
            mapping,
            verify=True,
        )
        utils.wait_until_function_pass(
            3,
            5,
            self.verify_cli_output_count,
            notification_count,
            update_count,
            receive_count,
        )

    def configure_routes_and_start_exabgp(self, cfg_file: str):
        """Setup function for exa to odl test case."""
        for prefix in ("1.1.1.1/32", "2.2.2.2/32"):
            mapping = {"PREFIX": prefix, "APP_RIB": ODL_IP}
            templated_requests.post_templated_request(
                f"{BGP_RR_VAR_FOLDER}/route", mapping, json=False
            )
        self.exabgp_process = bgp.start_exabgp_and_verify_connected(
            cfg_file, TOOLS_IP, "exabgp.log"
        )
        utils.wait_until_function_pass(3, 3, self.verify_exaBgp_received_updates, 4)

    def deconfigure_routes_and_stop_exabgp(self, cfg_file: str):
        """Teardown keyword for exa to odl test case."""
        bgp.stop_bgp_speaker(self.exabgp_process)
        mapping = {"PREFIX": prefix, "APP_RIB": ODL_IP}
        templated_requests.delete_templated_request(
            f"{BGP_RR_VAR_FOLDER}/route", mapping
        )

    def test_bgp_functional_route_ref(self, allure_step_with_separate_logging):

        with allure_step_with_separate_logging("step_test_suite_setup"):
            self.setup_config_files()

        with allure_step_with_separate_logging("step_configure_app_peer"):
            """Configures bgp application peer. Openconfig is used for carbon and above."""
            mapping = {"IP": ODL_IP, "BGP_RIB_OPENCONFIG": "example-bgp-rib"}
            templated_requests.put_templated_request(
                f"{BGP_VAR_FOLDER}/app_peer", mapping, json=False
            )

        with allure_step_with_separate_logging(
            "step_reconfigure_odl_to_accept_connection"
        ):
            """Configures BGP peer module with initiate-connection set to false."""
            mapping = {
                "IP": TOOLS_IP,
                "BGP_RIB_OPENCONFIG": "example-bgp-rib",
                "HOLDTIME": 180,
                "PEER_PORT": 17900,
                "PASSIVE_MODE": "true",
            }
            templated_requests.put_templated_request(
                f"{BGP_VAR_FOLDER}/bgp_peer", mapping, json=False
            )

        with allure_step_with_separate_logging("step_exa_to_send_route_refresh"):
            """Exabgp sends route refresh and count received updates."""
            self.configure_routes_and_start_exabgp(f"tmp/{BGP_CFG_NAME}")
            try:
                BGP_RPC_CLIENT.exa_clean_received_update_count()
                BGP_RPC_CLIENT.exa_announce("announce route-refresh ipv4 unicast")
                # From neon onwards there are extra BGP End-Of-RIB message
                update_count = 3
                utils.wait_until_function_pass(
                    5, 2, self.verify_exaBgp_received_updates, update_count
                )
                # From neon onwards there are extra BGP End-Of-RIB message per address family
                update_count = 7
                utils.wait_until_function_pass(
                    3,
                    5,
                    self.verify_odl_operational_state_count,
                    notification_count=0,
                    update_count=update_count,
                    receive_count=2,
                )
            finally:
                self.deconfigure_routes_and_stop_exabgp()

        with allure_step_with_separate_logging("step_odl_to_send_route_refresh"):
            """Sends route refresh request and checks if exabgp receives it."""
            bgp.start_exabgp_and_verify_connected(BGP_CFG_NAME, TOOLS_IP)
            try:
                BGP_RPC_CLIENT.exa_clean_received_route_refresh_count()
                mapping = {"BGP_PEER_IP": TOOLS_IP}
                templated_requests.post_templated_request(
                    f"{BGP_VAR_FOLDER}/route_refresh", mapping, json=False
                )
                utils.wait_until_function_pass(
                    5, 2, self.verify_exaBgp_received_route_refresh, 1
                )
                # From neon onwards there are extra BGP End-Of-RIB message per address family
                update_count = 9
                utils.wait_until_function_pass(
                    3,
                    5,
                    self.verify_odl_operational_state_count,
                    notification_count=1,
                    update_count=update_count,
                    receive_count=4,
                )
            finally:
                bgp.stop_exabgp(self.exabgp_process)

        with allure_step_with_separate_logging("step_delete_bgp_peer_configuration"):
            """Revert the BGP configuration to the original state: without any configured peers."""
            mapping = {
                "IP": TOOLS_IP,
                "BGP_RIB_OPENCONFIG": "example-bgp-rib",
            }
            templated_requests.delete_templated_request(
                f"{BGP_VAR_FOLDER}/bgp_peer", mapping
            )

        with allure_step_with_separate_logging("step_deconfigure_app_peer"):
            """Revert the BGP configuration to the original state: without application peer."""
            mapping = {
                "IP": ODL_IP,
                "BGP_RIB_OPENCONFIG": "example-bgp-rib",
            }
            templated_requests.delete_templated_request(
                f"{BGP_VAR_FOLDER}/app_peer", mapping
            )
