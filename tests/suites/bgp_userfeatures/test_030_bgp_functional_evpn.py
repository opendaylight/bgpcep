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
# Functional test suite for bgp - evpn
#
# This suite tests advertising and receiveing routes with evpn content.
# It uses play.py and odl as bgp peers. Routes advertized from odl are
# configured via application peer. Routes advertised from play.py are stored
# in *.hex files. These files are used also as expected data which is
# recevied from odl.

import json
import logging
import os
import pytest
import time

from libraries import bgp
from libraries import flowspec
from libraries import infra
from libraries import pcep
from libraries import templated_requests
from libraries import utils
from libraries.variables import variables


ODL_IP = variables.ODL_IP
RESTCONF_PORT = variables.RESTCONF_PORT
TOOLS_IP = variables.TOOLS_IP
BGP_RPC_CLIENT = bgp.BgpRpcClient(TOOLS_IP)
BGP_DIR = "variables/bgpfunctional"
EVPN_DIR = "variables/bgpfunctional/l2vpn_evpn"
RIB_NAME = "example-bgp-rib"
EVPN_CONF_URL = f"rests/data/bgp-rib:application-rib={ODL_IP}/tables=odl-bgp-evpn%3Al2vpn-address-family,odl-bgp-evpn%3Aevpn-subsequent-address-family/odl-bgp-evpn:evpn-routes"
EVPN_LOC_RIB = f"rests/data/bgp-rib:bgp-rib/rib={RIB_NAME}/loc-rib/tables=odl-bgp-evpn%3Al2vpn-address-family,odl-bgp-evpn%3Aevpn-subsequent-address-family/odl-bgp-evpn:evpn-routes?content=nonconfig"
EVPN_FAMILY_LOC_RIB = f"rests/data/bgp-rib:bgp-rib/rib={RIB_NAME}/loc-rib/tables=odl-bgp-evpn%3Al2vpn-address-family,odl-bgp-evpn%3Aevpn-subsequent-address-family?content=nonconfig"
XML_HEADERS = {"Content-Type": "application/xml"}
JSON_HEADERS = {"Content-Type": "application/json"}

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=37)
class TestBgpfunctionalEvpn:

    def set_app_peer(self):
        mapping = {
            "IP": ODL_IP,
            "BGP_RIB_OPENCONFIG": "example-bgp-rib",
        }
        templated_requests.put_templated_request(
            f"{BGP_DIR}/app_peer", mapping, json=False
        )

    def delete_app_peer(self):
        mapping = {
            "IP": ODL_IP,
            "BGP_RIB_OPENCONFIG": "example-bgp-rib",
        }
        templated_requests.delete_templated_request(f"{BGP_DIR}/app_peer", mapping)

    def set_bgp_peer_configuration(self):
        mapping = {
            "IP": TOOLS_IP,
            "BGP_RIB_OPENCONFIG": "example-bgp-rib",
            "HOLDTIME": 180,
            "PEER_PORT": 17900,
            "PASSIVE_MODE": "true",
        }
        templated_requests.put_templated_request(
            f"{BGP_DIR}/bgp_peer", mapping, json=False
        )

    def delete_bgp_peer_configuration(self):
        mapping = {
            "IP": TOOLS_IP,
            "BGP_RIB_OPENCONFIG": "example-bgp-rib",
        }
        templated_requests.delete_templated_request(f"{BGP_DIR}/bgp_peer", mapping)

    def get_update_content(self, expected_codes):
        templated_requests.get_from_uri(
            uri=EVPN_LOC_RIB, headers=XML_HEADERS, expected_code=expected_codes
        )
        update_message = bgp.get_update_message()
        return update_message

    def remove_configured_routes(self):
        """Removes the route if present. First GET is for debug purposes."""
        templated_requests.get_from_uri(
            uri=EVPN_LOC_RIB,
            headers=JSON_HEADERS,
            expected_code=templated_requests.ALLOWED_DELETE_STATUS_CODES,
        )
        response = templated_requests.get_from_uri(
            uri=f"{EVPN_CONF_URL}?content=config",
            headers=JSON_HEADERS,
            expected_code=templated_requests.ALLOWED_DELETE_STATUS_CODES,
        )
        if response.status_code not in templated_requests.DELETED_STATUS_CODES:
            templated_requests.delete_from_uri_request(
                uri=EVPN_CONF_URL, expected_code=204
            )

    def odl_to_play_template(self, to_test: str):
        data_xml = infra.get_file_content(f"{EVPN_DIR}/{to_test}/{to_test}.xml")
        announce_hex = infra.get_file_content(
            f"{EVPN_DIR}/{to_test}/announce_{to_test}.hex"
        )
        announce_hex = announce_hex.strip()
        withdraw_hex = infra.get_file_content(
            f"{EVPN_DIR}/{to_test}/withdraw_{to_test}.hex"
        )
        withdraw_hex = withdraw_hex.strip()
        try:
            BGP_RPC_CLIENT.play_clean()
            templated_requests.post_to_uri(
                uri=EVPN_CONF_URL, headers=XML_HEADERS, data=data_xml, expected_code=201
            )
            templated_requests.get_from_uri(
                uri=f"{EVPN_CONF_URL}?content=config", headers=XML_HEADERS
            )
            update_messag = utils.wait_until_function_pass(
                4, 2, self.get_update_content, templated_requests.ALLOWED_STATUS_CODES
            )
            bgp.verify_two_hex_messages_are_equal(update_messag, announce_hex)
            BGP_RPC_CLIENT.play_clean()
            self.remove_configured_routes()
            update_messag = utils.wait_until_function_pass(
                4, 2, self.get_update_content, templated_requests.DELETED_STATUS_CODES
            )
            bgp.verify_two_hex_messages_are_equal(update_messag, withdraw_hex)
        finally:
            self.remove_configured_routes()

    def loc_rib_presence(self, expected_content):
        resp = templated_requests.get_from_uri(uri=EVPN_LOC_RIB, headers=JSON_HEADERS)
        utils.verify_jsons_matach(
            expected_content, resp.content, "expected content", "received response"
        )

    def verify_test_preconditions(self):
        templated_requests.get_from_uri(
            uri=f"{EVPN_CONF_URL}?content=config",
            headers=JSON_HEADERS,
            expected_code=templated_requests.DELETED_STATUS_CODES,
        )
        resp = templated_requests.get_from_uri(
            uri=EVPN_FAMILY_LOC_RIB, headers=JSON_HEADERS
        )
        expected_content = infra.get_file_content(
            f"{EVPN_DIR}/empty_routes/empty_routes.json"
        )
        utils.verify_jsons_matach(
            expected_content, resp.content, "expected content", "received response"
        )

    def withdraw_route_and_verify(self, withdraw_hex):
        """Sends withdraw update message from exabgp and verifies route removal from odl's rib"""
        BGP_RPC_CLIENT.play_send(withdraw_hex)
        utils.wait_until_function_pass(3, 2, self.verify_test_preconditions)

    def play_to_odl_template(self, to_test: str):
        data_xml = infra.get_file_content(f"{EVPN_DIR}/{to_test}/{to_test}.xml")
        data_json = infra.get_file_content(f"{EVPN_DIR}/{to_test}/{to_test}.json")
        announce_hex = infra.get_file_content(
            f"{EVPN_DIR}/{to_test}/announce_{to_test}.hex"
        )
        withdraw_hex = infra.get_file_content(
            f"{EVPN_DIR}/{to_test}/withdraw_{to_test}.hex"
        )
        try:
            BGP_RPC_CLIENT.play_clean()
            BGP_RPC_CLIENT.play_send(announce_hex)
            utils.wait_until_function_pass(4, 2, self.loc_rib_presence, data_json)
            BGP_RPC_CLIENT.play_send(withdraw_hex)
            utils.wait_until_function_pass(4, 2, self.verify_test_preconditions)
        finally:
            self.withdraw_route_and_verify(withdraw_hex)

    def verify_reported_data(self, tempalate_path):
        """Verifies expected response"""
        templated_requests.get_templated_request(tempalate_path, None, verify=True)

    def wait_until_expected_data(self, template_path, retry_count=20, interval=1):
        utils.wait_until_function_pass(
            retry_count, interval, self.verify_reported_data, template_path
        )

    def test_bgp_functional_evpn(self, allure_step_with_separate_logging):
        with allure_step_with_separate_logging("step_configure_app_peer"):
            """Configures bgp application peer. Openconfig is used for carbon and above."""
            self.set_app_peer()

        with allure_step_with_separate_logging(
            "step_reconfigure_odl_to_accept_connection"
        ):
            """Configure BGP peer module with initiate-connection set to false."""
            self.set_bgp_peer_configuration()

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
                evpn=True,
                wfr=1,
            )
            utils.verify_process_did_not_stop_immediately(self.bgp_speaker_process.pid)

        with allure_step_with_separate_logging("step_odl_to_play_route_es_arb"):
            self.odl_to_play_template("route_es_arb")

        with allure_step_with_separate_logging("step_play_to_odl_route_es_arb"):
            self.play_to_odl_template("route_es_arb")

        with allure_step_with_separate_logging("step_odl_to_play_route_es_as"):
            self.odl_to_play_template("route_es_as")

        with allure_step_with_separate_logging("step_play_to_odl_route_es_as"):
            self.play_to_odl_template("route_es_as")

        with allure_step_with_separate_logging("step_odl_to_play_route_es_lacp"):
            self.odl_to_play_template("route_es_lacp")

        with allure_step_with_separate_logging("step_play_to_odl_route_es_lacp"):
            self.play_to_odl_template("route_es_lacp")

        with allure_step_with_separate_logging("step_odl_to_play_route_es_lan"):
            self.odl_to_play_template("route_es_lan")

        with allure_step_with_separate_logging("step_play_to_odl_route_es_lan"):
            self.play_to_odl_template("route_es_lan")

        with allure_step_with_separate_logging("step_odl_to_play_route_es_mac"):
            self.odl_to_play_template("route_es_mac")

        with allure_step_with_separate_logging("step_play_to_odl_route_es_mac"):
            self.play_to_odl_template("route_es_mac")

        with allure_step_with_separate_logging("step_odl_to_play_route_es_rou"):
            self.odl_to_play_template("route_es_rou")

        with allure_step_with_separate_logging("step_play_to_odl_route_es_rou"):
            self.play_to_odl_template("route_es_rou")

        with allure_step_with_separate_logging("step_odl_to_play_route_eth_arb"):
            self.odl_to_play_template("route_eth_arb")

        with allure_step_with_separate_logging("step_play_to_odl_route_eth_arb"):
            self.play_to_odl_template("route_eth_arb")

        with allure_step_with_separate_logging("step_odl_to_play_route_eth_as"):
            self.odl_to_play_template("route_eth_as")

        with allure_step_with_separate_logging("step_play_to_odl_route_eth_as"):
            self.play_to_odl_template("route_eth_as")

        with allure_step_with_separate_logging("step_odl_to_play_route_eth_lacp"):
            self.odl_to_play_template("route_eth_lacp")

        with allure_step_with_separate_logging("step_play_to_odl_route_eth_lacp"):
            self.play_to_odl_template("route_eth_lacp")

        with allure_step_with_separate_logging(
            "step_odl_to_play_route_eth_lacp_extdef"
        ):
            self.odl_to_play_template("route_eth_lacp_extdef")

        with allure_step_with_separate_logging(
            "step_play_to_odl_route_eth_lacp_extdef"
        ):
            self.play_to_odl_template("route_eth_lacp_extdef")

        with allure_step_with_separate_logging(
            "step_odl_to_play_route_eth_lacp_extesilab"
        ):
            self.odl_to_play_template("route_eth_lacp_extesilab")

        with allure_step_with_separate_logging(
            "step_play_to_odl_route_eth_lacp_extesilab"
        ):
            self.play_to_odl_template("route_eth_lacp_extesilab")

        with allure_step_with_separate_logging(
            "step_odl_to_play_route_eth_lacp_extesr"
        ):
            self.odl_to_play_template("route_eth_lacp_extesr")

        with allure_step_with_separate_logging(
            "step_play_to_odl_route_eth_lacp_extesr"
        ):
            self.play_to_odl_template("route_eth_lacp_extesr")

        with allure_step_with_separate_logging("step_odl_to_play_route_eth_lacp_extl2"):
            self.odl_to_play_template("route_eth_lacp_extl2")

        with allure_step_with_separate_logging("step_play_to_odl_route_eth_lacp_extl2"):
            self.play_to_odl_template("route_eth_lacp_extl2")

        with allure_step_with_separate_logging(
            "step_odl_to_play_route_eth_lacp_extmac"
        ):
            self.odl_to_play_template("route_eth_lacp_extmac")

        with allure_step_with_separate_logging(
            "step_play_to_odl_route_eth_lacp_extmac"
        ):
            self.play_to_odl_template("route_eth_lacp_extmac")

        with allure_step_with_separate_logging("step_odl_to_play_route_eth_lan"):
            self.odl_to_play_template("route_eth_lan")

        with allure_step_with_separate_logging("step_play_to_odl_route_eth_lan"):
            self.play_to_odl_template("route_eth_lan")

        with allure_step_with_separate_logging("step_odl_to_play_route_eth_mac"):
            self.odl_to_play_template("route_eth_mac")

        with allure_step_with_separate_logging("step_play_to_odl_route_eth_mac"):
            self.play_to_odl_template("route_eth_mac")

        with allure_step_with_separate_logging("step_odl_to_play_route_eth_rou"):
            self.odl_to_play_template("route_eth_rou")

        with allure_step_with_separate_logging("step_play_to_odl_route_eth_rou"):
            self.play_to_odl_template("route_eth_rou")

        with allure_step_with_separate_logging("step_odl_to_play_route_inc_arb"):
            self.odl_to_play_template("route_inc_arb")

        with allure_step_with_separate_logging("step_play_to_odl_route_inc_arb"):
            self.play_to_odl_template("route_inc_arb")

        with allure_step_with_separate_logging("step_odl_to_play_route_inc_as"):
            self.odl_to_play_template("route_inc_as")

        with allure_step_with_separate_logging("step_play_to_odl_route_inc_as"):
            self.play_to_odl_template("route_inc_as")

        with allure_step_with_separate_logging("step_odl_to_play_route_inc_lacp"):
            self.odl_to_play_template("route_inc_lacp")

        with allure_step_with_separate_logging("step_play_to_odl_route_inc_lacp"):
            self.play_to_odl_template("route_inc_lacp")

        with allure_step_with_separate_logging("step_odl_to_play_route_inc_lan"):
            self.odl_to_play_template("route_inc_lan")

        with allure_step_with_separate_logging("step_play_to_odl_route_inc_lan"):
            self.play_to_odl_template("route_inc_lan")

        with allure_step_with_separate_logging("step_odl_to_play_route_inc_mac"):
            self.odl_to_play_template("route_inc_mac")

        with allure_step_with_separate_logging("step_play_to_odl_route_inc_mac"):
            self.play_to_odl_template("route_inc_mac")

        with allure_step_with_separate_logging("step_odl_to_play_route_inc_rou"):
            self.odl_to_play_template("route_inc_rou")

        with allure_step_with_separate_logging("step_play_to_odl_route_inc_rou"):
            self.play_to_odl_template("route_inc_rou")

        with allure_step_with_separate_logging("step_odl_to_play_route_mac_arb"):
            self.odl_to_play_template("route_mac_arb")

        with allure_step_with_separate_logging("step_play_to_odl_route_mac_arb"):
            self.play_to_odl_template("route_mac_arb")

        with allure_step_with_separate_logging("step_odl_to_play_route_mac_as"):
            self.odl_to_play_template("route_mac_as")

        with allure_step_with_separate_logging("step_play_to_odl_route_mac_as"):
            self.play_to_odl_template("route_mac_as")

        with allure_step_with_separate_logging("step_odl_to_play_route_mac_lacp"):
            self.odl_to_play_template("route_mac_lacp")

        with allure_step_with_separate_logging("step_play_to_odl_route_mac_lacp"):
            self.play_to_odl_template("route_mac_lacp")

        with allure_step_with_separate_logging("step_odl_to_play_route_mac_lan"):
            self.odl_to_play_template("route_mac_lan")

        with allure_step_with_separate_logging("step_play_to_odl_route_mac_lan"):
            self.play_to_odl_template("route_mac_lan")

        with allure_step_with_separate_logging("step_odl_to_play_route_mac_mac"):
            self.odl_to_play_template("route_mac_mac")

        with allure_step_with_separate_logging("step_play_to_odl_route_mac_mac"):
            self.play_to_odl_template("route_mac_mac")

        with allure_step_with_separate_logging("step_odl_to_play_route_mac_rou"):
            self.odl_to_play_template("route_mac_rou")

        with allure_step_with_separate_logging("step_play_to_odl_route_mac_rou"):
            self.play_to_odl_template("route_mac_rou")

        with allure_step_with_separate_logging(
            "step_odl_to_play_pmsi_rsvp_te_p2mp_lsp"
        ):
            self.odl_to_play_template("pmsi_rsvp_te_p2mp_lsp")

        with allure_step_with_separate_logging(
            "step_play_to_odl_pmsi_rsvp_te_p2mp_lsp"
        ):
            self.play_to_odl_template("pmsi_rsvp_te_p2mp_lsp")

        with allure_step_with_separate_logging("step_odl_to_play_pmsi_mldp_p2mp_lsp"):
            self.odl_to_play_template("pmsi_mldp_p2mp_lsp")

        with allure_step_with_separate_logging("step_play_to_odl_pmsi_mldp_p2mp_lsp"):
            self.play_to_odl_template("pmsi_mldp_p2mp_lsp")

        with allure_step_with_separate_logging("step_odl_to_play_pmsi_pim_ssm_tree"):
            self.odl_to_play_template("pmsi_pim_ssm_tree")

        with allure_step_with_separate_logging("step_play_to_odl_pmsi_pim_ssm_tree"):
            self.play_to_odl_template("pmsi_pim_ssm_tree")

        with allure_step_with_separate_logging("step_odl_to_play_pmsi_pim_sm_tree"):
            self.odl_to_play_template("pmsi_pim_sm_tree")

        with allure_step_with_separate_logging("step_play_to_odl_pmsi_pim_sm_tree"):
            self.play_to_odl_template("pmsi_pim_sm_tree")

        with allure_step_with_separate_logging("step_odl_to_play_pmsi_bidir_pim_tree"):
            self.odl_to_play_template("pmsi_bidir_pim_tree")

        with allure_step_with_separate_logging("step_play_to_odl_pmsi_bidir_pim_tree"):
            self.play_to_odl_template("pmsi_bidir_pim_tree")

        with allure_step_with_separate_logging(
            "step_odl_to_play_pmsi_ingress_replication"
        ):
            self.odl_to_play_template("pmsi_ingress_replication")

        with allure_step_with_separate_logging(
            "step_play_to_odl_pmsi_ingress_replication"
        ):
            self.play_to_odl_template("pmsi_ingress_replication")

        with allure_step_with_separate_logging("step_odl_to_play_pmsi_mldp_mp2mp_lsp"):
            self.odl_to_play_template("pmsi_mldp_mp2mp_lsp")

        with allure_step_with_separate_logging("step_play_to_odl_pmsi_mldp_mp2mp_lsp"):
            self.play_to_odl_template("pmsi_mldp_mp2mp_lsp")

        with allure_step_with_separate_logging("step_kill_talking_bgp_speaker"):
            """Abort the Python speaker."""
            bgp.stop_bgp_speaker(self.bgp_speaker_process)
            infra.shell("cp play.py.out results/evpn_play.log")

        with allure_step_with_separate_logging("step_delete_bgp_peer_configuration"):
            """Revert the BGP configuration to the original state: without any configured peers."""
            self.delete_bgp_peer_configuration()

        with allure_step_with_separate_logging("step_deconfigure_app_peer"):
            """Revert the BGP configuration to the original state: without application peer"""
            self.delete_app_peer()
