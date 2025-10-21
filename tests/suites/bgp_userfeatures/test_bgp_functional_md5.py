#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# This suite tests simple connection between one ibgp peer (exabgp) and Odl.
# Peer is configured with ipv6, and exabgp connectes to odl via ipv6. Exabgp
# sends one ipv6 unicast route, which presence is verified in
# example-ipv6-topology. Tests this connection multiple times, with different
# ipv6 accepted formats, e.g. (::1, 0:0:0:0:0:0:0:1, full text) This suite
# also tests a combination of afi-safis on odl and exabgp. ipv6 route
# injection is carried out from odl to the ibgp peer without ipv6 family
# enabled on the peer device and checked for exceptions

import json
import logging
import os
import pytest
import time

from libraries import bgp
from libraries import infra
from libraries import pcep
from libraries import templated_requests
from libraries import utils
from libraries.variables import variables


ODL_IP = variables.ODL_IP
TOOLS_IP = variables.TOOLS_IP
BGP_EXAMD5_CFG = "exa-md5.cfg"
MD5_SAME_PASSWD = "topsecret"
MD5_DIFF_PASSWD = "different"

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=15)
class TestBgpFunctionalMd5:

    def reconfigure_odl_to_accept_connections(self, password: str):
        mapping = {"BGP_RIB_OPENCONFIG": "example-bgp-rib",
                   "IP": TOOLS_IP,
                   "HOLDTIME": 180, 
                   "PEER_PORT": 17900,
                   "PASSWORD": password,
                   "PASSIVE_MODE": "true"}
        templated_requests.put_templated_request("variables/bgpfunctional/bgp_md5/bgp_peer", mapping, json=False)

    def delete_bgp_peer_configuration(self):
        mapping = {"BGP_RIB_OPENCONFIG": "example-bgp-rib", "IP":TOOLS_IP }
        templated_requests.delete_templated_request("variables/bgpfunctional/bgp_md5/bgp_peer", mapping)

    def prepare_exabgp_config_file(self):
        infra.shell("cp variables/bgpfunctional/bgp_md5/exa-md5.cfg tmp/")
        infra.shell(f"sed -i -e 's/EXABGPIP/{TOOLS_IP}/g' tmp/exa-md5.cfg")
        infra.shell(f"sed -i -e 's/ODLIP/{ODL_IP}/g' tmp/exa-md5.cfg")
        infra.shell("sed -i -e 's/ROUTEREFRESH/disable/g' tmp/exa-md5.cfg")
        infra.shell("sed -i -e 's/ADDPATH/disable/g' tmp/exa-md5.cfg")
        infra.shell(f"sed -i -e 's/PASSWORD/{MD5_SAME_PASSWD}/g' tmp/exa-md5.cfg")
        rc, stdout = infra.shell("cat tmp/exa-md5.cfg")
        log.info(f"Updated tmp/exa-md5.cfg config:\n{stdout}")


    def test_bgp_functional_md5(self, allure_step_with_separate_logging):

        with allure_step_with_separate_logging("step_prepare_exabgp_config_file"):
            self.prepare_exabgp_config_file()

        with allure_step_with_separate_logging("step_verify_exabgp_connected"):
            """Verifies exabgp connected with md5 settings"""
            self.reconfigure_odl_to_accept_connections(MD5_SAME_PASSWD)
            exabgp_process = bgp.start_exabgp_and_verify_connected("tmp/exa-md5.cfg", TOOLS_IP)
            bgp.stop_exabgp(exabgp_process)
            self.delete_bgp_peer_configuration()

        with allure_step_with_separate_logging("step_verify_exabgp_not_connected"):
            """Verifies exabgp connected with md5 settings"""
            self.reconfigure_odl_to_accept_connections(MD5_DIFF_PASSWD)
            exabgp_process = bgp.start_exabgp_and_verify_connected("tmp/exa-md5.cfg", ODL_IP, expect_connected=False)
            bgp.stop_exabgp(exabgp_process)
            self.delete_bgp_peer_configuration()
            

        
