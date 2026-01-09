#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import logging

import allure
import pytest

from libraries import bgp
from libraries import infra
from libraries import templated_requests
from libraries.variables import variables


BGP_PEERS_COUNT = 20
ODL_IP = variables.ODL_IP
TOOLS_IP = variables.TOOLS_IP
BGP_TOOL_PORT = variables.BGP_TOOL_PORT
BGP_VAR_FOLDER = "variables/bgpfunctional/ipv6"
GOBGP_FOLDER = "variables/bgpfunctional/gobgp"
GOBGP_CFG = "gobgp"
GOBGP_LOG = "gobgp.log"
HOLDTIME = 180
RIB_INSTANCE = "example-bgp-rib"
GOBGP_BINARY_URL = "https://github.com/osrg/gobgp/releases/download/v2.18.0/gobgp_2.18.0_linux_386.tar.gz"
FILE_NAME = "gobgp_2.18.0_linux_386.tar.gz"

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_gobgp_processes")
@pytest.mark.run(order=67)
class TestBgpLlgrBasic:
    gobgp_processes = []

    def prepare_gobgp_config_file(self):
        for i in range(BGP_PEERS_COUNT):
            infra.shell(
                f"cp variables/bgpfunctional/gobgp/gobgp.cfg tmp/{GOBGP_CFG}-{i}.cfg"
            )
            infra.shell(f"sed -i -e 's/GOBGPIP/127.0.1.{i}/g' tmp/{GOBGP_CFG}-{i}.cfg")
            infra.shell(f"sed -i -e 's/ODLIP/{ODL_IP}/g' tmp/{GOBGP_CFG}-{i}.cfg")
            infra.shell(f"sed -i -e 's/ROUTERID/127.0.1.{i}/g' tmp/{GOBGP_CFG}-{i}.cfg")
            infra.shell(f"sed -i -e 's/ROUTEREFRESH/disable/g' tmp/{GOBGP_CFG}-{i}.cfg")
            infra.shell(f"sed -i -e 's/ADDPATH/disable/g' tmp/{GOBGP_CFG}-{i}.cfg")
            rc, stdout = infra.shell(f"cat tmp/{GOBGP_CFG}-{i}.cfg")
            log.info(f"Updated tmp/{GOBGP_CFG}-{i}.cfg config:\n{stdout}")

    def download_gobgp_binary(self):
        """Downloads gobgp binary and untar the binary zip file."""
        infra.download_file(GOBGP_BINARY_URL)
        infra.shell(f"tar -xzf tmp/{FILE_NAME} -C tmp")

    @allure.description(
        "**This suite tests simple connection between twnety ibgp peers"
        "(goabgp) and Odl.**\n"
        "\n"
        "Peers are configured with ipv6, and gobgp connectes to odl"
        "via ipv6.")
    def test_bgp_llgr_basic(self, allure_step_with_separate_logging):

        with allure_step_with_separate_logging("step_setup_gobgp"):
            self.download_gobgp_binary()
            self.prepare_gobgp_config_file()

        with allure_step_with_separate_logging(
            "step_reconfigure_odl_to_accept_connections"
        ):
            """Configure BGP peer modules with initiate-connection set to
            false with short ipv6 address."""
            for i in range(BGP_PEERS_COUNT):
                mapping = {
                    "IP": f"127.0.1.{i}",
                    "BGP_RIB_OPENCONFIG": "example-bgp-rib",
                    "HOLDTIME": HOLDTIME,
                    "PEER_PORT": BGP_TOOL_PORT,
                    "PASSIVE_MODE": "true",
                }
                templated_requests.put_templated_request(
                    f"{BGP_VAR_FOLDER}/bgp_peer", mapping, json=False
                )

        with allure_step_with_separate_logging("step_start_gobgp"):
            """Starts gobgp peer simulator."""
            for i in range(BGP_PEERS_COUNT):
                ip = f"127.0.1.{i}"
                gobgp_process = bgp.start_gobgp_and_verify_connected(
                    f"tmp/gobgpd",
                    f"tmp/{GOBGP_CFG}-{i}.cfg",
                    ip,
                    log_file=f"gobgp-{i}.log",
                    grpc_address=f"{ip}:",
                )
                self.gobgp_processes.append(gobgp_process)

        with allure_step_with_separate_logging("step_delete_bgp_peers_configuration"):
            """Revert the BGP configuration to the original state without any
            configured peer."""
            for i in range(BGP_PEERS_COUNT):
                mapping = {"IP": f"127.0.1.{i}", "BGP_RIB_OPENCONFIG": RIB_INSTANCE}
                templated_requests.delete_templated_request(
                    f"{BGP_VAR_FOLDER}/bgp_peer", mapping
                )

        with allure_step_with_separate_logging("step_stop_gobgp"):
            """Save gobgp logs as gobgp.log, and stop gobgp with SIGINT bash
            signal."""
            for i in range(BGP_PEERS_COUNT):
                infra.backup_file(src_file_name=f"{GOBGP_CFG}-{i}.cfg")
                bgp.stop_gobgp(self.gobgp_processes[i])
