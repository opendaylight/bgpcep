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

import json
import logging
import os
import pytest
import time

from lib import bgp
from lib import flowspec
from lib import infra
from lib import pcep
from lib import templated_requests
from lib import ip_topology
from lib import utils


ODL_IP = os.environ["ODL_IP"]
RESTCONF_PORT = os.environ["RESTCONF_PORT"]
TOOLS_IP = os.environ["TOOLS_IP"]
BGP_RPC_CLIENT = bgp.BgpRpcClient(TOOLS_IP)
L3VPN_MCAST_DIR = "data/bgpfunctional/l3vpn_mcast"
log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=15)
class TestBgpfunctionalL3vpnMcast:

    def set_app_peer(self):
        """???
        """
        mapping = {
            "IP": ODL_IP,
            "BGP_RIB": "example-bgp-rib",
        }
        templated_requests.put_templated_request(
            f"{L3VPN_MCAST_DIR}/app_peer", mapping, json=False
        )

    def delete_app_peer(self):
        """???
        """
        mapping = {
            "IP": ODL_IP,
            "BGP_RIB": "example-bgp-rib",
        }
        templated_requests.delete_templated_request(
            f"{L3VPN_MCAST_DIR}/app_peer", mapping
        )

    def set_bgp_peer_configuration(self):
        """???
        """
        mapping = {
            "IP": TOOLS_IP,
            "BGP_RIB": "example-bgp-rib",
            "HOLDTIME": 180,
            "PEER_PORT": 17900,
            "PASSIVE_MODE":"true"
        }
        templated_requests.put_templated_request(
            f"{L3VPN_MCAST_DIR}/bgp_peer", mapping, json=False
        )

    def delete_bgp_peer_configuration(self):
        """???
        """
        mapping = {
            "IP": TOOLS_IP,
            "BGP_RIB": "example-bgp-rib",
        }
        templated_requests.delete_templated_request(
            f"{L3VPN_MCAST_DIR}/bgp_peer", mapping
        )
    
    def test_bgp_functional_l3vpn_mcast(self, allure_step_with_separate_logging):
        with allure_step_with_separate_logging("step_configure_app_peer"):
            """Configures bgp application peer."""
            self.set_app_peer()

        with allure_step_with_separate_logging("step_reconfigure_odl_to_accept_connection"):
            """Configure BGP peer module with initiate-connection set to false."""
            self.set_bgp_peer_configuration()

        with allure_step_with_separate_logging("step_start_bgp_peer"):
            """Start Python speaker to connect to ODL. We need to wait until
            odl really starts to accept incomming bgp connection.
            The failure happens if the incomming connection comes
            too quickly after configuring the peer in the previous
            test case."""
            self.bgp_speaker_process = bgp.start_bgp_speaker_with_verify_and_retry(retries=3, ammount=0, my_ip=TOOLS_IP, peer_ip=ODL_IP, log_level="debug", l3vpn_mcast=True, wfr=1)
            utils.verify_process_did_not_stop_immediatelp(self.bgp_speaker_process.pid)

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
            infra.shell("cp play.py.out results/l3vpn_mcast_play.log")

        with allure_step_with_separate_logging("step_delete_bgp_peer_configuration"):
            """Revert the BGP configuration to the original state: without any configured peers."""
            self.delete_bgp_peer_configuration()

        with allure_step_with_separate_logging("step_deconfigure_app_peer"):
            """	Revert the BGP configuration to the original state: without application peer"""
            self.delete_app_peer()
