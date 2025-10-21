#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# Functional test for bgp - route-target-constrain safi
#
# This suite tests advertising rt-constrain routes to odl. For advertising 
# from peer play.py is used, sending hex messages to odl.
# There are 3 peers: ebgp and two ibgps. First peer sends l3vpn route with
# specific RT to odl, second peer sends RT route and third peer only
# establishes connection. Then it is checked that odl advertizes l3vpn route
# to second peer. Third peer sends wildcard RT route and it is checked that
# odl doesn't advertize l3vpn route to it. Then second peer removes RT and it
# is checked that second peer withdrew RT route and that odl withdrew
# l3vpn route from it.

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
BGP_RPC_CLIENT2 = bgp.BgpRpcClient(ODL_IP, 8002)
BGP_RPC_CLIENT3 = bgp.BgpRpcClient(ODL_IP, 8003)
BGP_RPC_CLIENT4 = bgp.BgpRpcClient(ODL_IP, 8004)
BGP_RPC_CLIENTS = (BGP_RPC_CLIENT2, BGP_RPC_CLIENT3, BGP_RPC_CLIENT4)
HOLDTIME = 180
RT_CONSTRAIN_DIR = "data/bgpfunctional/rt_constrain"
EBGP_DIR = "data/bgpfunctional/ebgp_peer"
PLAY_SCRIPT = "tools/play.py"
RIB_NAME = "example-bgp-rib"
ODL_2_IP = "127.0.0.2"
ODL_3_IP = "127.0.0.3"
ODL_4_IP = "127.0.0.4"
ODL_IPS = (ODL_2_IP, ODL_3_IP, ODL_4_IP)
BGP_PEER_TYPES = ("external", "internal", "internal")
BGP_PEER_AS_NUMBERS = (65000, 64496, 64496)
ODL_IP_INDICES_ALL =  (2, 3, 4)
L3VPN_RT_CHECK = ("false", "true", "false")
RT_CONSTRAIN_APP_PEER = {"IP": ODL_IP, "BGP_RIB": RIB_NAME}
ADJ_RIB_OUT = {"PATH": f"peer=bgp:%2F%2F{ODL_3_IP}/adj-rib-out", "BGP_RIB": RIB_NAME}

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=15)
class TestBgpfunctionalRtConstrainValidation:

    def start_bgp_peer(self, ip, as_number, port, filename):
        """Starts bgp peer."""
        bgp.start_bgp_speaker(ammount=0, my_ip=ip, peer_ip=ODL_IP, as_number=as_number, port=port, usepeerip=True, log_level="debug", allf=True, wfr=1, log_file=filename)

    def play_to_odl_non_removal_bgprpcclient(self, bgp_rpc_client, to_test, dir, ipv="ipv4"):
        """Read contents of file {dir}/${totest}/announce_{totest}.hex and send it to odl."""
        announce_hex = infra.get_file_content(f"{dir}/{to_test}/announce_{to_test}.hex")
        bgp_rpc_client.play_send(announce_hex)

    def play_to_odl_routes_removal_template_bgprpcclient(self, bgp_rpc_client, to_test, dir, ipv="ipv4"):
        """Read contents of file {dir}/{totest}/withdraw_{totest}.hex and send it to odl to remove rt argument from odl."""
        withdraw_hex = infra.get_file_content(f"{dir}/{to_test}/withdraw_{to_test}.hex")
        bgp_rpc_client.play_clean()
        bgp_rpc_client.play_send(withdraw_hex)

    def get_update_message_and_compare_with_hex_bgprpcclient(self, bgp_rpc_client, hex, option):
        """Returns hex update message and compares it to hex."""
        update = bgp_rpc_client.play_get()
        if option:
            assert update == hex
        else:
            assert update != hex

    def check_for_l3vpn_odl_advertisement(self, announce_hex):
        """Checks that each node received or did not receive update message containing given hex message."""
        for bgp_rpc_client, option in zip(BGP_RPC_CLIENTS, L3VPN_RT_CHECK):
                self.get_update_message_and_compare_with_hex_bgprpcclient(bgp_rpc_client, announce_hex, option)

    def verify_reported_data(self, url, exprspfile):
        """Verifies expected response"""
        expresponse = infra.get_file_content(exprspfile)
        log.info(f"expected_responseL {expresponse}")
        rsp = templated_requests.get_request(url)
        log.info(f"actual_response: {rsp}")
        log.info(f"actual_response_content: {rsp.content}")
        utils.verify_jsons_matach(expresponse, rsp.content)

    def verify_empty_reproted_data(self):
        """Verify empty data response"""
        templated_requests.get_templated_request(f"{RT_CONSTRAIN_DIR}/empty_l3vpn", mapping=ADJ_RIB_OUT, verify=True)
    
    def test_bgp_functional_rt_constrain_validation(self, allure_step_with_separate_logging):
        with allure_step_with_separate_logging("step_reconfigure_odl_to_accept_connection"):
            """Configures BGP peer module with initiate-connection set to false."""
            for odl_ip, type in zip(ODL_IPS, BGP_PEER_TYPES):
                mapping = {
                    "IP": odl_ip,
                    "TYPE": type,
                    "BGP_RIB": RIB_NAME,
                    "HOLDTIME": HOLDTIME,
                    "PEER_PORT": 17900,
                    "PASSIVE_MODE":"true"
                }
                templated_requests.put_templated_request(
                    EBGP_DIR, mapping, json=False
                )
        
        with allure_step_with_separate_logging("step_start_bgp_peers"):
            """Start Python speaker to connect to ODL. We give each speaker time until odl really starts to accept incoming
            bgp connection. The failure happens if the incoming connection comes too quickly after configuring the peer."""
            for i in range(3):
                odl_ip = ODL_IPS[i]
                as_number = BGP_PEER_AS_NUMBERS[i]
                log.info(f"IP: {odl_ip} as_number: ${as_number}")
                self.start_bgp_peer(odl_ip, as_number, 8002+i, f"play.py.090.{2+i}")

        with allure_step_with_separate_logging("step_play_to_odl_ext_l3vpn_rt_arg"):
            """This test step sends route-target route containing route-target argument from node 1 to odl
            so odl can identify this peer as appropriate for advertizement when it recieves such route."""
            self.play_to_odl_non_removal_bgprpcclient(BGP_RPC_CLIENT2, "ext_l3vpn_rt_arg", RT_CONSTRAIN_DIR)
            mapping = {"PATH": f"peer=bgp:%2F%2F{ODL_2_IP}/effective-rib-in", "BGP_RIB": RIB_NAME}
            utils.wait_until_function_pass(3, 2, templated_requests.get_templated_request, f"{RT_CONSTRAIN_DIR}/ext_l3vpn_rt_arg/rib", mapping, verify=True)

        with allure_step_with_separate_logging("step_play_to_odl_rt_constrain_type_0"):
            """This test step sends route-target route containing route-target argument from node 1 to odl
            so odl can identify this peer as appropriate for advertizement when it recieves such route."""
            self.play_to_odl_non_removal_bgprpcclient(BGP_RPC_CLIENT3, "rt_constrain_type_0", RT_CONSTRAIN_DIR)
            mapping = {"PATH": "loc-rib", "BGP_RIB": RIB_NAME}
            utils.wait_until_function_pass(3, 2, templated_requests.get_templated_request, f"{RT_CONSTRAIN_DIR}/rt_constrain_type_0/rib", mapping, verify=True)

        with allure_step_with_separate_logging("step_check_presence_of_l3vpn_route_in_node_2_effective_rib_in_table"):
            """Checks l3vpn route is present in node 2 effective-rib-in table."""
            utils.wait_until_function_pass(3, 2, templated_requests.get_templated_request, f"{RT_CONSTRAIN_DIR}/ext_l3vpn_rt_arg/rib", mapping=ADJ_RIB_OUT, verify=True)

        with allure_step_with_separate_logging("step_check_l3vpn_route_advertisement_on_each_node"):
            """Checks that each node received or did not receive update message containing given hex message."""
            announce = infra.get_file_content(f"{RT_CONSTRAIN_DIR}/ext_l3vpn_rt_arg/announce_ext_l3vpn_rt_arg.hex")
            announce_hex = announce.strip()
            self.check_for_l3vpn_odl_advertisement(announce_hex)

        with allure_step_with_separate_logging("step_play_to_odl_rt_constrain_type_1"):
            """Sends RT route from node 3 to odl and then checks that odl does not advertize l3vpn route from previous test step,
            that is that update message is empty."""
            self.play_to_odl_non_removal_bgprpcclient(BGP_RPC_CLIENT4, "rt_constrain_type_1", RT_CONSTRAIN_DIR)
            mapping = {"PATH": f"peer=bgp:%2F%2F{ODL_4_IP}/effective-rib-in", "BGP_RIB": {RIB_NAME}}
            utils.wait_until_function_pass(3, 2, templated_requests.get_templated_request, f"{RT_CONSTRAIN_DIR}/rt_constrain_type_1/rib", mapping, verify=True)
            update = BGP_RPC_CLIENT4.play_get()

        with allure_step_with_separate_logging("step_play_to_odl_remove_rt"):
            """Removes RT from odl and then checks that second node withdrew l3vpn route and third node did not receive any message."""
            BGP_RPC_CLIENT3.play_clean()
            self.play_to_odl_routes_removal_template_bgprpcclient(BGP_RPC_CLIENT3, "rt_constrain_type_0", RT_CONSTRAIN_DIR)

        with allure_step_with_separate_logging("step_play_to_odl_remove_routes"):
            """Removes rt arguments from odl."""
            self.play_to_odl_routes_removal_template_bgprpcclient(BGP_RPC_CLIENT2, "ext_l3vpn_rt_arg", RT_CONSTRAIN_DIR)
            self.play_to_odl_routes_removal_template_bgprpcclient(BGP_RPC_CLIENT4, "rt_constrain_type_1", RT_CONSTRAIN_DIR)

        with allure_step_with_separate_logging("step_kill_talking_bgp_speakers"):
            """Abort all Python speakers."""
            for i in range(3):
                infra.shell(f"cp tmp/play.py.090.{2+i} results/play.py.090.{2+i}")
            bgp.kill_all_bgp_speakers()

        with allure_step_with_separate_logging("step_delete_bgp_peers_configuration"):
            """Revert the BGP configuration to the original state: without any configured peers."""
            for odl_ip, type in zip(ODL_IP_INDICES_ALL, BGP_PEER_TYPES):
                mapping = {"IP": odl_ip, "TYPE": type, "BGP_RIB": RIB_NAME, "HOLDTIME": HOLDTIME, "PEER_PORT": 17900, "PASSIVE_MODE":"true"}
                templated_requests.delete_templated_request(EBGP_DIR, mapping)
