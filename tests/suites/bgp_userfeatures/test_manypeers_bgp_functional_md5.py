#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import logging

import allure
from jinja2 import Environment, FileSystemLoader
import pytest

from libraries import bgp
from libraries import infra
from libraries import templated_requests
from libraries.variables import variables


BGP_PEERS_COUNT = 20
ODL_IP = variables.ODL_IP
TOOLS_IP = variables.TOOLS_IP
BGP_TOOL_PORT = variables.BGP_TOOL_PORT
HOLDTIME = 180
BGP_VAR_FOLDER = "variables/bgpfunctional/bgp_md5"
BGP_PEER_FOLDER = f"{BGP_VAR_FOLDER}/bgp_peer"
BGP_EXAMD5_CFG = "tmp/exa-md5.cfg"
MD5_SAME_PASSWD = "topsecret"
MD5_DIFF_PASSWD = "different"
PROTOCOL_OPENCONFIG = "example-bgp-rib"

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_exabgp_processes")
@pytest.mark.run(order=65)
class TestBgpFunctionalMd5:

    def reconfigure_odl_to_accept_connections(self, password: str):
        for i in range(BGP_PEERS_COUNT):
            mapping = {
                "IP": f"127.0.1.{i}",
                "BGP_RIB_OPENCONFIG": PROTOCOL_OPENCONFIG,
                "HOLDTIME": HOLDTIME,
                "PEER_PORT": BGP_TOOL_PORT,
                "PASSWORD": f"{password}",
                "PASSIVE_MODE": "true",
            }
            templated_requests.put_templated_request(
                BGP_PEER_FOLDER, mapping, json=False
            )

    def delete_bgp_peer_configuration(self):
        for i in range(BGP_PEERS_COUNT):
            mapping = {"IP": f"127.0.1.{i}", "BGP_RIB_OPENCONFIG": PROTOCOL_OPENCONFIG}
            templated_requests.delete_templated_request(BGP_PEER_FOLDER, mapping)

    def prepare_exabgp_config_file(self):
        env = Environment(loader=FileSystemLoader("variables/bgpfunctional/bgp_md5/"))
        template = env.get_template("manypeers-exa-md5.j2")
        config = template.render(
            {
                "PEER_COUNT": BGP_PEERS_COUNT,
                "ODLIP": ODL_IP,
                "ROUTEREFRESH": "disable",
                "ADDPATH": "disable",
                "PASSWORD": MD5_SAME_PASSWD,
            }
        )
        infra.save_to_a_file(f"tmp/exa-md5.cfg", config)

    @allure.description(
        "**This suite tests tcpmd5 connections of bgp peers.**\n"
        "\n"
        "It uses odl and exabgp as bgp peers. No routes are advertized, "
        "simple peer presence in the datastore is tested.")
    def test_bgp_functional_md5(self, allure_step_with_separate_logging):

        with allure_step_with_separate_logging("step_prepare_exabgp_config_file"):
            self.prepare_exabgp_config_file()

        with allure_step_with_separate_logging("step_verify_exabgp_connected"):
            """Verifies exabgp connected with md5 settings."""
            self.reconfigure_odl_to_accept_connections(MD5_SAME_PASSWD)
            exabgp_ips = [f"127.0.1.{i}" for i in range(BGP_PEERS_COUNT)]
            exabgp_process = bgp.start_exabgp_and_verify_connected(
                BGP_EXAMD5_CFG, exabgp_ips, log_file="exabgp.log"
            )
            bgp.stop_exabgp(exabgp_process)
            self.delete_bgp_peer_configuration()

        with allure_step_with_separate_logging("step_verify_exabgp_not_connected"):
            """Verifies exabgp not connected with md5 settings."""
            self.reconfigure_odl_to_accept_connections(MD5_DIFF_PASSWD)
            exabgp_ips = [f"127.0.1.{i}" for i in range(BGP_PEERS_COUNT)]
            exabgp_process = bgp.start_exabgp_and_verify_connected(
                BGP_EXAMD5_CFG,
                exabgp_ips,
                expect_connected=False,
                log_file="exabgp.log",
            )
            bgp.stop_exabgp(exabgp_process)
            self.delete_bgp_peer_configuration()
