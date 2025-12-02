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

import logging
import pytest

from libraries import bgp
from libraries import infra
from libraries import templated_requests
from libraries import utils
from libraries.variables import variables


PEERS_COUNT = 1

ODL_IP = variables.ODL_IP
RESTCONF_PORT = variables.RESTCONF_PORT
TOOLS_IP = variables.TOOLS_IP
BGP_RPC_CLIENT = bgp.BgpRpcClient(TOOLS_IP)
BGP_DIR = "variables/bgpfunctional"
#ROUTE_DIR = "variables/bgpfunctional/l2vpn_evpn"
#ROUTE_TYPE = "route_es_arb"
ROUTE_DIR = "variables/bgpfunctional/rt_constrain"
ROUTE_TYPE = "l3vpn_rt_arg"
RIB_NAME = "example-bgp-rib"

RT_CONSTRAIN_DIR = "variables/bgpfunctional/rt_constrain"
EVPN_CONF_URL = f"rests/data/bgp-rib:application-rib={ODL_IP}/tables=odl-bgp-evpn%3Al2vpn-address-family,odl-bgp-evpn%3Aevpn-subsequent-address-family/odl-bgp-evpn:evpn-routes"
EVPN_LOC_RIB = f"rests/data/bgp-rib:bgp-rib/rib={RIB_NAME}/loc-rib/tables=odl-bgp-evpn%3Al2vpn-address-family,odl-bgp-evpn%3Aevpn-subsequent-address-family/odl-bgp-evpn:evpn-routes?content=nonconfig"
EVPN_FAMILY_LOC_RIB = f"rests/data/bgp-rib:bgp-rib/rib={RIB_NAME}/loc-rib/tables=odl-bgp-evpn%3Al2vpn-address-family,odl-bgp-evpn%3Aevpn-subsequent-address-family?content=nonconfig"
XML_HEADERS = {"Content-Type": "application/xml"}
JSON_HEADERS = {"Content-Type": "application/json"}

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("allure_step_with_separate_logging")
@pytest.mark.run(order=37)
class TestBgpfunctionalEvpn:
    
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

    def loc_rib_presence(self, expected_content):
        resp = templated_requests.get_from_uri(uri=EVPN_LOC_RIB, headers=JSON_HEADERS)
        utils.verify_jsons_matach(
            expected_content, resp.content, "expected content", "received response"
        )

    def play_to_odl(self, allure_step_with_separate_logging, announce_peer_id, withdraw_peer_id):
        with allure_step_with_separate_logging("get test data fro play_to_odl"):
            data_json = infra.get_file_content(f"{EVPN_DIR}/route_es_arb/route_es_arb.json")
            announce_hex = infra.get_file_content(
                f"{EVPN_DIR}/route_es_arb/announce_route_es_arb.hex"
            )
            withdraw_hex = infra.get_file_content(
                f"{EVPN_DIR}/route_es_arb/withdraw_route_es_arb.hex"
            )
        with allure_step_with_separate_logging("announce hex"):
            BGP_RPC_CLIENT.play_send(announce_hex, peer_id=announce_peer_id)
        with allure_step_with_separate_logging("check hex data present in ODL"):
            utils.wait_until_function_pass(4, 2, self.loc_rib_presence, data_json)
        with allure_step_with_separate_logging("withdraw hex"):
            BGP_RPC_CLIENT.play_send(withdraw_hex, peer_id=withdraw_peer_id)
        with allure_step_with_separate_logging("check hex data removed from ODL"):
            utils.wait_until_function_pass(4, 2, self.verify_test_preconditions)

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


    def odl_to_play(self, allure_step_with_separate_logging):
        with allure_step_with_separate_logging("get test data for odl_to_play"):
            announce_hex = infra.get_file_content(
                f"{ROUTE_DIR}/{ROUTE_TYPE}/announce_{ROUTE_TYPE}.hex"
            )
            logging.warn(f"{announce_hex=}")
            announce_hex = announce_hex.strip()
            withdraw_hex = infra.get_file_content(
                f"{ROUTE_DIR}/{ROUTE_TYPE}/withdraw_{ROUTE_TYPE}.hex"
            )
            withdraw_hex = withdraw_hex.strip()
            logging.warn(f"{withdraw_hex=}")
            bgp.play_to_odl_non_removal_template(
                "rt_constrain_type_0", RT_CONSTRAIN_DIR
            )
        # announce hex
        with allure_step_with_separate_logging("announce hex"):
            #data_xml = infra.get_file_content(f"{ROUTE_DIR}/{ROUTE_TYPE}/{ROUTE_TYPE}.xml")
            #templated_requests.post_to_uri(
            #    uri=EVPN_CONF_URL, headers=XML_HEADERS, data=data_xml, expected_code=201
            #)
            mapping = {"IP": ODL_IP, "BGP_RIB": "example-bgp-rib"}
            templated_requests.post_templated_request(
                f"{ROUTE_DIR}/{ROUTE_TYPE}/app", mapping, json=False
            )
        #with allure_step_with_separate_logging("check hex data received by play"):
        #    utils.wait_until_function_pass(4, 2, self.check_all_peers, announce_hex)
        # --- FIX: Wrap the call that fails ---
        with allure_step_with_separate_logging("check hex data received by play"):
            check_passed = False  # Start with a failure flag
            try:
                # This is the function that raises the "poisonous" exception
                utils.wait_until_function_pass(4, 2, self.check_all_peers, announce_hex)
                check_passed = True  # If it returns, it passed

            except AssertionError as e:
                # 1. Catch the exception
                # 2. Log it, so the error message appears in the Allure logs
                log.error(f"wait_until_function_pass failed: {e}")
                # 3. check_passed remains False
            
            # 4. Assert on the flag. This is a "clean" assertion
            #    that Allure will handle correctly.
            assert check_passed, "check_all_peers failed to verify hex data received by play"

        # --- END FIX ---
        with allure_step_with_separate_logging("clean play"):
            BGP_RPC_CLIENT.play_clean()
        # withdraw hex
        with allure_step_with_separate_logging("withdraw hex"):
            self.remove_configured_routes()
        with allure_step_with_separate_logging("check hex withdrawn from play"):
            utils.wait_until_function_pass(4, 2, self.check_all_peers, withdraw_hex)

    def check_all_peers(self, expected_hex):
        for i in range(PEERS_COUNT):
            log.warn(f"Checking peer no. {i}")
            update_message = BGP_RPC_CLIENT.play_get(peer_id=i)
            log.warn(f"{update_message=}")
            bgp.verify_two_hex_messages_are_equal(update_message, expected_hex)

    @pytest.fixture(scope="function")
    def setup_with_all_peers_evpn(self, allure_step_with_separate_logging):
        yield from self.setup(True, allure_step_with_separate_logging)

    @pytest.fixture(scope="function")
    def setup_with_some_peers_evpn(self, allure_step_with_separate_logging):
        yield from self.setup(False, allure_step_with_separate_logging)

    def setup(self, all_peers_support_evpn, allure_step_with_separate_logging):
        # setup app peer
        with allure_step_with_separate_logging("Setup: Configure app peer"):
            mapping = {
                "IP": ODL_IP,
                "BGP_RIB_OPENCONFIG": "example-bgp-rib",
            }
            templated_requests.put_templated_request(
                f"{BGP_DIR}/app_peer", mapping, json=False
            )
        # setup peers
        with allure_step_with_separate_logging("Setup: Configure BGP peers"):
            if all_peers_support_evpn:
                peers_supporting_evpn = 1
                peers_not_supporting_evpn = 0
            else:
                peers_supporting_evpn = 10
                peers_not_supporting_evpn = 10
            for i in range(0, peers_supporting_evpn):
                #mapping = {
                #    "IP": f"127.0.1.{i}",
                #    "BGP_RIB_OPENCONFIG": "example-bgp-rib",
                #    "HOLDTIME": 180,
                #    "PEER_PORT": 17900,
                #    "PASSIVE_MODE": "true",
                #}
                #templated_requests.put_templated_request(
                #    f"{BGP_DIR}/bgp_peer", mapping, json=False
                #)
                mapping = {
                    "IP": TOOLS_IP,
                    "BGP_RIB": "example-bgp-rib",
                }
                templated_requests.put_templated_request(
                    f"{RT_CONSTRAIN_DIR}/bgp_peer", mapping, json=False
                )
            for i in range(peers_supporting_evpn, peers_supporting_evpn + peers_not_supporting_evpn):
                mapping = {
                    "IP": f"127.0.1.{i}",
                    "BGP_RIB_OPENCONFIG": "example-bgp-rib",
                    "HOLDTIME": 180,
                    "PEER_PORT": 17900,
                    "PASSIVE_MODE": "true",
                }
                templated_requests.put_templated_request(
                    f"variables/bgpuser/bgp_peer", mapping, json=False
                )
        
        # start bgp speaker
        with allure_step_with_separate_logging("Setup: Start BGP speaker"):
            self.bgp_speaker_process = bgp.start_bgp_speaker_with_verify_and_retry(
                    retries=3,
                    ammount=0,
                    my_ip=TOOLS_IP,
                    peer_ip=ODL_IP,
                    log_level="debug",
                    #evpn=True,
                    wfr=1,
                    allf=True,
                    multiplicity=PEERS_COUNT
                )
            utils.verify_process_did_not_stop_immediately(self.bgp_speaker_process.pid)
        yield
        exceptions = []
        # stop bgp speaker
        with allure_step_with_separate_logging("Teardown: Stop BGP speaker"):
            try:
                bgp.stop_bgp_speaker(self.bgp_speaker_process, gracefully=False)
            except Exception as e:
                exceptions.append(e)
        # delete peers
        with allure_step_with_separate_logging("Teardown: Delete BGP peers"):
            try:
                for i in range(20):
                    mapping = {
                    "IP": f"127.0.1.{i}",
                    "BGP_RIB_OPENCONFIG": "example-bgp-rib",
                    }
                    templated_requests.delete_templated_request(f"{BGP_DIR}/bgp_peer", mapping)
            except Exception as e:
                exceptions.append(e)
        # delete app peer
        with allure_step_with_separate_logging("Teardown: Delete app peer"):
            try:
                mapping = {
                    "IP": ODL_IP,
                    "BGP_RIB_OPENCONFIG": "example-bgp-rib",
                }
                templated_requests.delete_templated_request(f"{BGP_DIR}/app_peer", mapping)
            except Exception as e:
                exceptions.append(e)
        #if exceptions:
        #    raise exceptions[0]
        if exceptions:
            log.warn(exceptions)

    def test_route_received_by_all_peers(self, setup_with_all_peers_evpn, allure_step_with_separate_logging):
        self.odl_to_play(allure_step_with_separate_logging)

    """def test_sending_and_withdrawing_route_to_odl_using_specific_peer(self, setup_with_all_peers_evpn, allure_step_with_separate_logging):
        self.play_to_odl(allure_step_with_separate_logging, announce_peer_id=4, withdraw_peer_id=4)

    @pytest.mark.xfail
    def test_route_received_by_some_peers(self, setup_with_some_peers_evpn, allure_step_with_separate_logging):
        self.odl_to_play(allure_step_with_separate_logging)

    @pytest.mark.xfail
    def test_sending_and_withdrawing_route_to_odl_using_different_peers(self, setup_with_all_peers_evpn, allure_step_with_separate_logging):
        self.play_to_odl(allure_step_with_separate_logging, announce_peer_id=1, withdraw_peer_id=4)

    @pytest.mark.xfail
    def test_not_set_peer_send_to_odl(self, setup_with_some_peers_evpnm, allure_step_with_separate_logging):
        self.play_to_odl(allure_step_with_separate_logging, announce_peer_id=15, withdraw_peer_id=15)
    """