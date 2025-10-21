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
# Functional test suite for bgp - l3vpn-ipv4
#
# This suite tests advertising and receiveing routes with l3vpn content. It
# uses odl and exabgp as bgp peers. Routes advertized from odl are configured
# via application peer. Routes advertised from exabgp is statically configured
# in exabgp config file.
# For fluorine and further, instead of exabgp, play.py is used. When sending
# routes from odl to peer, first route containg route-target argument
# have to be send from peer to odl, so odl can identify this peer. Than it
# sends l3vpn route containg this argument to odl app peer, and we check
# that app peer advertizes this route back to the peer.

import json
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
BGP_VAR_FOLDER = "variables/bgpfunctional"
BGP_L3VPN_DIR = f"{BGP_VAR_FOLDER}/l3vpn_ipv4"
DEFAULT_BGPCEP_LOG_LEVEL = "INFO"
DEFAULT_EXA_CFG = "exa.cfg"
EXARPCSCRIPT = "tools/exabgp_files/exarpc.py"
HOLDTIME = 180
L3VPN_EXA_CFG = "bgp-l3vpn-ipv4.cfg"
L3VPN_EXP = "exa_expected"
L3VPN_RSP = "bgp_l3vpn_ipv4"
L3VPN_RSPEMPTY = "bgp_l3vpn_ipv4_empty"
PLAY_SCRIPT = "tools/fastbgp/play.py"
RIB_INSTANCE = "example-bgp-rib"
RT_CONSTRAIN_DIR = "variables/bgpfunctional/rt_constrain"

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=36)
class TestBgpfunctionalL3Vpn:

    def verify_exabgp_received_update(self, exp_update_fn):
        """Verification of receiving particular update message."""
        exp_update = templated_requests.resolve_templated_text(
            f"{BGP_L3VPN_DIR}/{exp_update_fn}/data.json"
        )
        rcv_update_dict = BGP_RPC_CLIENT.exa_get_update_message(msg_only=True)
        rcv_update = json.dumps(rcv_update_dict)
        utils.verify_jsons_matach(exp_update, rcv_update)

    def verify_empty_reported_data(self):
        """Verfiy empty data response"""
        templated_requests.get_templated_request(
            f"{BGP_L3VPN_DIR}/empty_route", None, verify=True
        )

    def verify_reported_data(self, exprspdir):
        """Verifies expected response"""
        templated_requests.get_templated_request(
            f"{BGP_L3VPN_DIR}/{exprspdir}", None, verify=True
        )

    def l3vpn_ipv4_to_app(self):
        """Testing mpls vpn ipv4 routes reported to odl from exabgp."""
        BGP_RPC_CLIENT.exa_clean_update_message()
        mapping = {"BGP_PEER_IP": TOOLS_IP, "APP_PEER_IP": ODL_IP}
        templated_requests.post_templated_request(f"{BGP_L3VPN_DIR}/route", mapping)
        utils.wait_until_function_pass(
            5, 2, self.verify_exabgp_received_update, L3VPN_EXP
        )

    def prepare_config_files(self):
        infra.shell(f"cp variables/bgpfunctional/{DEFAULT_EXA_CFG} tmp/")
        infra.shell(f"sed -i -e 's/EXABGPIP/{TOOLS_IP}/g' tmp/{DEFAULT_EXA_CFG}")
        infra.shell(f"sed -i -e 's/ODLIP/{ODL_IP}/g' tmp/{DEFAULT_EXA_CFG}")
        infra.shell(f"sed -i -e 's/ROUTEREFRESH/disable/g' tmp/{DEFAULT_EXA_CFG}")
        infra.shell(f"sed -i -e 's/ADDPATH/disable/g' tmp/{DEFAULT_EXA_CFG}")
        infra.shell(f"cp variables/bgpfunctional/l3vpn_ipv4/{L3VPN_EXA_CFG} tmp/")
        infra.shell(f"sed -i -e 's/EXABGPIP/{TOOLS_IP}/g' tmp/{L3VPN_EXA_CFG}")
        infra.shell(f"sed -i -e 's/ODLIP/{ODL_IP}/g' tmp/{L3VPN_EXA_CFG}")

    def test_bgp_functional_l3vpn(self, allure_step_with_separate_logging):
        with allure_step_with_separate_logging("step_prepare_config_files"):
            self.prepare_config_files()

        with allure_step_with_separate_logging("step_configure_app_peer"):
            """Configures bgp application peer. Openconfig is used for carbon and above."""
            bgp.set_bgp_application_peer(ip=ODL_IP)

        with allure_step_with_separate_logging(
            "step_reconfigure_odl_to_accept_connection"
        ):
            """Configure BGP peer module with initiate-connection set to false."""
            mapping = {
                "IP": TOOLS_IP,
                "BGP_RIB": RIB_INSTANCE,
            }
            templated_requests.put_templated_request(
                f"{RT_CONSTRAIN_DIR}/bgp_peer", mapping, json=False
            )

        with allure_step_with_separate_logging("step_l3vpn_ipv4_to_odl"):
            """Testing mpls vpn ipv4 routes reported to odl from exabgp."""
            utils.wait_until_function_pass(3, 2, self.verify_empty_reported_data)
            exabgp_process = bgp.start_exabgp_and_verify_connected(
                f"tmp/{L3VPN_EXA_CFG}", TOOLS_IP
            )
            utils.wait_until_function_pass(15, 1, self.verify_reported_data, L3VPN_RSP)
            bgp.stop_exabgp(exabgp_process)
            utils.wait_until_function_pass(3, 2, self.verify_empty_reported_data)

        with allure_step_with_separate_logging("step_start_play"):
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
                allf=True,
                wfr=1,
            )
            utils.verify_process_did_not_stop_immediately(self.bgp_speaker_process.pid)

        with allure_step_with_separate_logging("step_play_to_odl_rt_constrain_type_0"):
            """Sends route-target route containg route-target argument so odl
            can identify this peer as appropriate for advertizement when it
            recieves such route."""
            bgp.play_to_odl_non_removal_template(
                "rt_constrain_type_0", RT_CONSTRAIN_DIR
            )

        with allure_step_with_separate_logging("step_odl_to_play_l3vpn_rt_arg"):
            """Same as test step before but fluorine and further this l3vpn
            route also needs to contain route-target argument."""
            bgp.odl_to_play_template("l3vpn_rt_arg", RT_CONSTRAIN_DIR, False)

        with allure_step_with_separate_logging("step_kill_talking_bgp_speaker"):
            """Abort the Python speaker."""
            bgp.stop_bgp_speaker(self.bgp_speaker_process)
            infra.backup_file(
                src_file_name="play.py.out", target_file_name="010_l3vpn_play.log"
            )

        with allure_step_with_separate_logging("step_delete_bgp_peer_configuration"):
            """Revert the BGP configuration to the original state: without any configured peers."""
            mapping = {
                "IP": TOOLS_IP,
                "BGP_RIB": RIB_INSTANCE,
            }
            templated_requests.delete_templated_request(
                f"{RT_CONSTRAIN_DIR}/bgp_peer", mapping
            )

        with allure_step_with_separate_logging("step_deconfigure_app_peer"):
            """Revert the BGP configuration to the original state: without application peer"""
            bgp.delete_bgp_application_peer(ip=ODL_IP)
