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
# Functional test suite for bgp - l3vpn-mutlicast
#
# This suite tests advertising l3vpn_mcast routes to odl. For advertising
# play.py is used, and particular files are stored as *.hex files.There are
# L3vpn-ipv4-multicast routes and L3vpn-ipv6-multicast routes tested.

import logging
import pytest

from libraries import bgp
from libraries import infra
from libraries import templated_requests
from libraries import utils
from libraries.variables import variables


ODL_IP = variables.ODL_IP
RESTCONF_PORT = variables.RESTCONF_PORT
TOOLS_IP = variables.TOOLS_IP
BGP_TOOL_PORT = variables.BGP_TOOL_PORT
BGP_RPC_CLIENT = bgp.BgpRpcClient(TOOLS_IP)
HOLDTIME = 180
L3VPN_MCAST_DIR = "variables/bgpfunctional/l3vpn_mcast"
RIB_NAME = "example-bgp-rib"
L3VPN_MCAST_APP_PEER_MAPPING = {"IP": ODL_IP, "BGP_RIB": RIB_NAME}
L3VPN_MCAST_ODL_CONFIG_MAPPING = {
    "IP": TOOLS_IP,
    "HOLDTIME": HOLDTIME,
    "PEER_PORT": BGP_TOOL_PORT,
    "INITIATE": "false",
    "BGP_RIB": RIB_NAME,
    "PASSIVE_MODE": "true",
}


log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=40)
class TestBgpfunctionalL3vpnMcast:

    def test_bgp_functional_l3vpn_mcast(self, allure_step_with_separate_logging):
        with allure_step_with_separate_logging("step_configure_app_peer"):
            """Configures bgp application peer."""
            templated_requests.put_templated_request(
                f"{L3VPN_MCAST_DIR}/app_peer", L3VPN_MCAST_APP_PEER_MAPPING, json=False
            )

        with allure_step_with_separate_logging(
            "step_reconfigure_odl_to_accept_connection"
        ):
            """Configure BGP peer module with initiate-connection set to false."""
            templated_requests.put_templated_request(
                f"{L3VPN_MCAST_DIR}/bgp_peer",
                L3VPN_MCAST_ODL_CONFIG_MAPPING,
                json=False,
            )

        with allure_step_with_separate_logging("step_start_bgp_peer"):
            """Start Python speaker to connect to ODL. We need to wait until
            odl really starts to accept incomming bgp connection.
            The failure happens if the incomming connection comes
            too quickly after configuring the peer in the previous
            test case."""
            self.bgp_speaker_process = bgp.start_bgp_speaker_with_verify_and_retry(
                retries=3,
                ammount=0,
                my_ip=TOOLS_IP,
                peer_ip=ODL_IP,
                log_level="debug",
                l3vpn_mcast=True,
                wfr=1,
            )
            utils.verify_process_did_not_stop_immediately(self.bgp_speaker_process.pid)

        with allure_step_with_separate_logging("step_odl_to_play_l3vpn_mcast"):
            bgp.odl_to_play_template("l3vpn_mcast", L3VPN_MCAST_DIR)

        with allure_step_with_separate_logging("step_play_to_odl_l3vpn_mcast"):
            bgp.play_to_odl_template("l3vpn_mcast", L3VPN_MCAST_DIR)

        with allure_step_with_separate_logging("step_odl_to_play_l3vpn_mcast_ipv6"):
            bgp.odl_to_play_template("l3vpn_mcast_ipv6", L3VPN_MCAST_DIR)

        with allure_step_with_separate_logging("step_play_to_odl_l3vpn_mcast_ipv6"):
            bgp.play_to_odl_template("l3vpn_mcast_ipv6", L3VPN_MCAST_DIR, "ipv6")

        with allure_step_with_separate_logging("step_kill_talking_bgp_speaker"):
            """Abort the Python speaker."""
            bgp.stop_bgp_speaker(self.bgp_speaker_process)
            infra.backup_file(
                src_file_name="play.py.out", target_file_name="l3vpn_mcast_play.log"
            )

        with allure_step_with_separate_logging("step_delete_bgp_peer_configuration"):
            """Revert the BGP configuration to the original state: without any configured peers."""
            templated_requests.delete_templated_request(
                f"{L3VPN_MCAST_DIR}/bgp_peer", L3VPN_MCAST_ODL_CONFIG_MAPPING
            )

        with allure_step_with_separate_logging("step_deconfigure_app_peer"):
            """Revert the BGP configuration to the original state: without application peer"""
            templated_requests.delete_templated_request(
                f"{L3VPN_MCAST_DIR}/app_peer", L3VPN_MCAST_APP_PEER_MAPPING
            )
