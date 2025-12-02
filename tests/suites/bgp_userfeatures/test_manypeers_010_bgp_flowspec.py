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
# Functional test for bgp flowspec.

from jinja2 import Environment, FileSystemLoader
import logging
import pytest

from libraries import bgp
from libraries import flowspec
from libraries import infra
from libraries import templated_requests
from libraries import utils
from libraries.variables import variables


BGP_PEERS_COUNT = 20
ODL_IP = variables.ODL_IP
TOOLS_IP = variables.TOOLS_IP
BGP_TOOL_PORT = variables.BGP_TOOL_PORT
BGP_VARIABLES_FOLDER = "variables/bgpflowspec/"
CMD = "env exabgp.tcp.port=1790 exabgp --debug"
HOLDTIME = 180
CFG1 = "bgp-flowspec-manypeers.cfg"
EXP1 = "bgp_flowspec_manypeers"
CFG2 = "bgp-flowspec-redirect-manypeers.cfg"
EXP2 = "bgp_flowspec_redirect_manypeers"
FLOWSPEC_URL = "/rests/data/bgp-rib:bgp-rib/rib=example-bgp-rib/loc-rib/tables=bgp-types:ipv4-address-family,bgp-flowspec:flowspec-subsequent-address-family/bgp-flowspec:flowspec-routes?content=nonconfig"
RIB_INSTANCE = "example-bgp-rib"
PROTOCOL_OPENCONFIG = RIB_INSTANCE
EMPTY_LIST = tuple()

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=35)
class TestBgpFlowspec:

    def prepare_config_files(self):
        env = Environment(loader=FileSystemLoader(BGP_VARIABLES_FOLDER))
        # generate config file for bgp-flowspec-manypeers.cfg
        template = env.get_template("bgp-flowspec-manypeers.j2")
        config = template.render({"ODL_IP": ODL_IP, "PEER_COUNT": BGP_PEERS_COUNT})
        infra.save_to_a_file(f"tmp/{CFG1}", config)
        # generate config file for bgp-flowspec-redirect-manypeers.cfg
        template = env.get_template("bgp-flowspec-redirect-manypeers.j2")
        config = template.render({"ODL_IP": ODL_IP, "PEER_COUNT": BGP_PEERS_COUNT})
        infra.save_to_a_file(f"tmp/{CFG2}", config)

    def setup_test_case(self, cfg_file: str):
        flowspec.verify_flowspec_data_is_empty()
        peers_ips = [f"127.0.1.{i}" for i in range(BGP_PEERS_COUNT)]
        self.exabgp_process = bgp.start_exabgp_and_verify_connected(
            f"tmp/{cfg_file}", peers_ips, log_file="exabgp.log"
        )

    def verify_flowspec_data(self, exprspdir: str):
        """Verify expected response"""
        templated_requests.get_templated_request(
            f"{BGP_VARIABLES_FOLDER}/{exprspdir}", mapping=None, verify=True
        )

    def test_bgp_flowspec(self, allure_step_with_separate_logging):
        with allure_step_with_separate_logging("step_prepare_config_files"):
            self.prepare_config_files()

        with allure_step_with_separate_logging(
            "step_check_for_rmpty_topology_before_talking"
        ):
            """Sanity check bgp-flowspec:flowspec-routes is up but empty."""
            flowspec.wait_until_flowspec_data_is_empty(20, 3)

        with allure_step_with_separate_logging(
            "step_reconfigure_odl_to_accept_connection"
        ):
            """Configure BGP peer module with initiate-connection set to false."""
            for i in range(BGP_PEERS_COUNT):
                mapping = {
                    "IP": f"127.0.1.{i}",
                    "HOLDTIME": HOLDTIME,
                    "PEER_PORT": BGP_TOOL_PORT,
                    "PASSIVE_MODE": "true",
                    "BGP_RIB_OPENCONFIG": PROTOCOL_OPENCONFIG,
                }
                templated_requests.put_templated_request(
                    f"{BGP_VARIABLES_FOLDER}/bgp_peer", mapping, json=False
                )

        with allure_step_with_separate_logging("step_flowspec_test_1"):
            """Testing flowspec values for bgp-flowspec.cfg."""
            self.setup_test_case(CFG1)
            utils.wait_until_function_pass(15, 1, self.verify_flowspec_data, EXP1)
            bgp.stop_exabgp(self.exabgp_process)

        with allure_step_with_separate_logging("step_flowspec_test_2"):
            """Testing flowspec values for bgp-flowspec-redirect.cfg."""
            self.setup_test_case(CFG2)
            utils.wait_until_function_pass(15, 1, self.verify_flowspec_data, EXP2)
            bgp.stop_exabgp(self.exabgp_process)

        with allure_step_with_separate_logging(
            "step_deconfigure_odl_to_accept_connection"
        ):
            """Deconfigure BGP peer."""
            for i in range(BGP_PEERS_COUNT):
                mapping = {"BGP_RIB_OPENCONFIG": PROTOCOL_OPENCONFIG, "IP": f"127.0.1.{i}"}
                templated_requests.delete_templated_request(
                    f"{BGP_VARIABLES_FOLDER}/bgp_peer", mapping
                )
