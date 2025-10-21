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
# Functional test suite for bgp - n-path and all-path selection
# 
# This suite tests n-path and all-path selection policy.
# It uses odl and exabgp as bgp peers. Routes advertized from
# odl are configured via application peer.

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
HOLDTIME =  180
DEVICE_NAME = "controller-config"
BGP_PEER_NAME = "example-bgp-peer"
RIB_INSTANCE = "example-bgp-rib"
PROTOCOL_OPENCONFIG = RIB_INSTANCE
APP_PEER_NAME = "example-bgp-peer-app"
BGP_VAR_FOLDER = "data/bgpfunctional"
MULT_VAR_FOLDER = f"{BGP_VAR_FOLDER}/multipaths"
DEFAUTL_RPC_CFG = "exa.cfg"
EXARPCSCRIPT = "tools/exabgp_files/exarpc.py"
N_PATHS_VALUE = 2
DEFAULT_MAPPING = {"ODLIP": ODL_IP, "EXAIP": TOOLS_IP, "NPATHS": N_PATHS_VALUE}
PATH_ID_LIST = (1, 2, 3)
NEXT_HOP_PREF = "100.100.100."
OPENCONFIG_RIB_URI = f"http://{ODL_IP}:{RESTCONF_PORT}/rests/data/openconfig-network-instance:network-instances/network-instance=global-bgp/openconfig-network-instance:protocols/protocol=openconfig-policy-types%3ABGP,example-bgp-rib"
NPATHS_SELM = "n-paths"
ALLPATHS_SELM = "all-paths"
ADDPATHCAP_SR = "send\\/receive"
ADDPATHCAP_S = "send"
ADDPATHCAP_R = "receive"
ADDPATHCAP_D =  "disable"

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=15)
class TestBgpfunctionalMultipath:
    exabgp_process = None
    rib_odl = None

    def setup_config_files(self, add_path="disable"):
        """Copies exabgp config files."""
        rc, output = infra.shell("which python")
        log.warn(output)
        infra.shell(f"cp {BGP_VAR_FOLDER}/{DEFAUTL_RPC_CFG} tmp/")
        infra.shell(f"cp {EXARPCSCRIPT} tmp/")
        infra.shell(f"sed -i -e 's/EXABGPIP/{TOOLS_IP}/g' tmp/{DEFAUTL_RPC_CFG}")
        infra.shell(f"sed -i -e 's/ODLIP/{ODL_IP}/g' tmp/{DEFAUTL_RPC_CFG}")
        infra.shell(f"sed -i -e 's/ROUTEREFRESH/enable/g' tmp/{DEFAUTL_RPC_CFG}")
        infra.shell(f"sed -i -e 's/ADDPATH/{add_path}/g' tmp/{DEFAUTL_RPC_CFG}")
        rc, stdout = infra.shell(f"cat tmp/{DEFAUTL_RPC_CFG}")
        log.info(stdout)

    def store_rib_configuration(self):
        """Stores rib configuration"""
        response = templated_requests.get_templated_request(f"{MULT_VAR_FOLDER}/rib_config", mapping=DEFAULT_MAPPING)
        self.rib_odl = response.text

    def restore_original_rib_configuration(self):
        """Suite teardown keyword with old rib restoration"""
        templated_requests.put_request(OPENCONFIG_RIB_URI, self.rib_odl)

    def configure_path_selection_and_app_peer_and_connect_peer(self, odl_path_sel_mode, exa_add_path_value):
        """Setup test case function. Early after the path selection config the incomming connection
        from exabgp towards odl may be rejected by odl due to config process not finished yet. Because of that
        we try to start the tool 3 times in case early attempts fail."""
        self.configure_odl_peer_with_path_selection_mode(odl_path_sel_mode)
        self.configure_app_peer_with_routes()
        self.setup_config_files(add_path=exa_add_path_value)
        bgp.start_exabgp_and_verify_connected(f"tmp/{DEFAUTL_RPC_CFG}", TOOLS_IP, "exabgp.log")


    def remove_odl_and_app_peer_configuration_and_stop_exaBgp(self):
        mapping = {"IP": TOOLS_IP, "BGP_RIB_OPENCONFIG": PROTOCOL_OPENCONFIG}
        templated_requests.delete_templated_request(f"{MULT_VAR_FOLDER}/bgp_peer", mapping)
        self.deconfigure_app_peer()
        bgp.stop_exabgp(self.exabgp_process)

    def verify_expected_update_count(self, exp_count):
        """Verify number of received update messages"""
        tool_count = BGP_RPC_CLIENT.exa_get_received_update_count()
        assert tool_count == exp_count

    def configure_odl_peer_with_path_selection_mode(self, psm):
        """Configures odl peer with path selection mode"""
        npaths = 0 if psm == ALLPATHS_SELM else N_PATHS_VALUE
        mapping = {
            "IP": TOOLS_IP,
            "BGP_RIB_OPENCONFIG": PROTOCOL_OPENCONFIG,
            "HOLDTIME": HOLDTIME,
            "PEER_PORT": 17900,
            "PASSIVE_MODE":"true",
            "MULTIPATH": npaths
        }
        templated_requests.put_templated_request(f"{MULT_VAR_FOLDER}/rib_policies", mapping, json=False)
        templated_requests.put_templated_request(f"{MULT_VAR_FOLDER}/bgp_peer", mapping, json=False)

    def log_loc_rib_operational(self):
        rsp = templated_requests.get_request(f"http://{ODL_IP}:{RESTCONF_PORT}/rests/data/bgp-rib:bgp-rib/rib=example-bgp-rib/loc-rib?content=nonconfig")
        log.info(rsp.json())

    def configure_app_peer_with_routes(self):
        """Configure bgp application peer and fill it immediately with routes."""
        mapping = {
            "IP": ODL_IP,
            "BGP_RIB_OPENCONFIG": PROTOCOL_OPENCONFIG
        }
        templated_requests.put_templated_request(f"{BGP_VAR_FOLDER}/app_peer", mapping, json=False)
        for path_id in PATH_ID_LIST:
            route_mapping = {"NEXTHOP": f"{NEXT_HOP_PREF}{path_id}", "LOCALPREF": f"{path_id}00", "PATHID": path_id, "APP_RIB": ODL_IP}
            templated_requests.post_templated_request(f"{MULT_VAR_FOLDER}/route", route_mapping, json=False)

    def deconfigure_app_peer(self):
        """Revert the BGP configuration to the original state: without application peer"""
        route_mapping = {"APP_RIB": ODL_IP, "BGP_RIB_OPENCONFIG": PROTOCOL_OPENCONFIG}
        templated_requests.delete_templated_request(f"{MULT_VAR_FOLDER}/route", route_mapping)
        mapping = {"IP": TOOLS_IP, "BGP_RIB_OPENCONFIG": PROTOCOL_OPENCONFIG}
        templated_requests.delete_templated_request(f"{BGP_VAR_FOLDER}/app_peer", mapping)
    
    def test_bgp_functional_multipath(self, allure_step_with_separate_logging):
        
        with allure_step_with_separate_logging("step_test_suite_setup"):
            self.setup_config_files()
            self.store_rib_configuration()

        with allure_step_with_separate_logging("step_odl_allpaths_exa_sendreceived"):
            """all-paths selected policy selected."""
            self.configure_path_selection_and_app_peer_and_connect_peer(ALLPATHS_SELM, ADDPATHCAP_SR)
            try:
                self.log_loc_rib_operational()
                # From neon onwards there is extra BGP End-Of-RIB message
                update_messages = 4
                utils.wait_until_function_pass(6, 2, self.verify_expected_update_count, update_messages)
            finally:
                self.remove_odl_and_app_peer_configuration_and_stop_exaBgp()

        with allure_step_with_separate_logging("step_odl_npaths_exa_sendreceived"):
            """n-paths policy selected on odl."""
            self.configure_path_selection_and_app_peer_and_connect_peer(ALLPATHS_SELM, ADDPATHCAP_SR)
            try:
                self.log_loc_rib_operational()
                # From neon onwards there is extra BGP End-Of-RIB message
                update_messages = 3
                utils.wait_until_function_pass(6, 2, self.verify_expected_update_count, update_messages)
            finally:
                self.remove_odl_and_app_peer_configuration_and_stop_exaBgp()