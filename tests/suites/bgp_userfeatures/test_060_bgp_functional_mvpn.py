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
# Functional test suite for bgp - mvpn
#
# This suite tests advertising mvpn routes to odl. For advertising play.py
# is used, and particular files are stored as *.hex files. There are
# 7 different types of routes used for auto-discovery of multicast network.
# Also 4 more routes with new attributes specific for mvpn.

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
MVPN_DIR = "variables/bgpfunctional/mvpn"
log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=39)
class TestBgpfunctionalMvpn:

    def set_app_peer(self):
        mapping = {
            "IP": ODL_IP,
            "BGP_RIB": "example-bgp-rib",
        }
        templated_requests.put_templated_request(
            f"{MVPN_DIR}/app_peer", mapping, json=False
        )

    def delete_app_peer(self):
        mapping = {
            "IP": ODL_IP,
            "BGP_RIB": "example-bgp-rib",
        }
        templated_requests.delete_templated_request(f"{MVPN_DIR}/app_peer", mapping)

    def set_bgp_peer_configuration(self):
        mapping = {
            "IP": TOOLS_IP,
            "BGP_RIB": "example-bgp-rib",
            "HOLDTIME": 180,
            "PEER_PORT": 17900,
            "PASSIVE_MODE": "true",
        }
        templated_requests.put_templated_request(
            f"{MVPN_DIR}/bgp_peer", mapping, json=False
        )

    def delete_bgp_peer_configuration(self):
        mapping = {
            "IP": TOOLS_IP,
            "BGP_RIB": "example-bgp-rib",
        }
        templated_requests.delete_templated_request(f"{MVPN_DIR}/bgp_peer", mapping)

    def test_bgp_functional_Mvpn(self, allure_step_with_separate_logging):
        with allure_step_with_separate_logging("step_configure_app_peer"):
            """Configures bgp application peer."""
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
                mvpn=True,
                wfr=1,
            )
            utils.verify_process_did_not_stop_immediately(self.bgp_speaker_process.pid)

        with allure_step_with_separate_logging("step_odl_to_play_intra_as_ipmsi_ad"):
            bgp.odl_to_play_template("intra_as_ipmsi_ad", MVPN_DIR)

        with allure_step_with_separate_logging("step_play_to_odl_intra_as_ipmsi_ad"):
            bgp.play_to_odl_template("intra_as_ipmsi_ad", MVPN_DIR)

        with allure_step_with_separate_logging("step_odl_to_play_inter_as_ipmsi_ad"):
            bgp.odl_to_play_template("inter_as_ipmsi_ad", MVPN_DIR)

        with allure_step_with_separate_logging("step_play_to_odl_inter_as_ipmsi_ad"):
            bgp.play_to_odl_template("inter_as_ipmsi_ad", MVPN_DIR)

        with allure_step_with_separate_logging("step_odl_to_play_spmsi_ad"):
            bgp.odl_to_play_template("spmsi_ad", MVPN_DIR)

        with allure_step_with_separate_logging("step_play_to_odl_spmsi_ad"):
            bgp.play_to_odl_template("spmsi_ad", MVPN_DIR)

        with allure_step_with_separate_logging("step_odl_to_play_leaf_ad"):
            bgp.odl_to_play_template("leaf_ad", MVPN_DIR)

        with allure_step_with_separate_logging("step_play_to_odl_leaf_ad"):
            bgp.play_to_odl_template("leaf_ad", MVPN_DIR)

        with allure_step_with_separate_logging("step_odl_to_play_source_active_ad"):
            bgp.odl_to_play_template("source_active_ad", MVPN_DIR)

        with allure_step_with_separate_logging("step_play_to_odl_source_active_ad"):
            bgp.play_to_odl_template("source_active_ad", MVPN_DIR)

        with allure_step_with_separate_logging("step_odl_to_play_shared_tree_join"):
            bgp.odl_to_play_template("shared_tree_join", MVPN_DIR)

        with allure_step_with_separate_logging("step_play_to_odl_shared_tree_join"):
            bgp.play_to_odl_template("shared_tree_join", MVPN_DIR)

        with allure_step_with_separate_logging("step_odl_to_play_source_tree_join"):
            bgp.odl_to_play_template("source_tree_join", MVPN_DIR)

        with allure_step_with_separate_logging("step_play_to_odl_source_tree_join"):
            bgp.play_to_odl_template("source_tree_join", MVPN_DIR)

        with allure_step_with_separate_logging(
            "step_odl_to_play_intra_pe_distinguisher"
        ):
            bgp.odl_to_play_template("intra_pe_distinguisher", MVPN_DIR)

        with allure_step_with_separate_logging(
            "step_play_to_odl_intra_pe_distinguisher"
        ):
            bgp.play_to_odl_template("intra_pe_distinguisher", MVPN_DIR)

        with allure_step_with_separate_logging("step_odl_to_play_intra_vrf"):
            bgp.odl_to_play_template("intra_vrf", MVPN_DIR)

        with allure_step_with_separate_logging("step_play_to_odl_intra_vrf"):
            bgp.play_to_odl_template("intra_vrf", MVPN_DIR)

        with allure_step_with_separate_logging("step_odl_to_play_intra_source_as"):
            bgp.odl_to_play_template("intra_source_as", MVPN_DIR)

        with allure_step_with_separate_logging("step_play_to_odl_intra_source_as"):
            bgp.play_to_odl_template("intra_source_as", MVPN_DIR)

        with allure_step_with_separate_logging("step_odl_to_play_intra_source_as_4"):
            bgp.odl_to_play_template("intra_source_as_4", MVPN_DIR)

        with allure_step_with_separate_logging("step_play_to_odl_intra_source_as_4"):
            bgp.play_to_odl_template("intra_source_as_4", MVPN_DIR)

        with allure_step_with_separate_logging("step_play_to_odl_intra_ipv6"):
            bgp.play_to_odl_template("intra_ipv6", MVPN_DIR, "ipv6")

        with allure_step_with_separate_logging("step_kill_talking_bgp_speaker"):
            """Abort the Python speaker."""
            bgp.stop_bgp_speaker(self.bgp_speaker_process)
            infra.shell("cp play.py.out results/mvpn_play.log")

        with allure_step_with_separate_logging("step_delete_bgp_peer_configuration"):
            """Revert the BGP configuration to the original state: without any configured peers."""
            self.delete_bgp_peer_configuration()

        with allure_step_with_separate_logging("step_deconfigure_app_peer"):
            """Revert the BGP configuration to the original state: without application peer"""
            self.delete_app_peer()
