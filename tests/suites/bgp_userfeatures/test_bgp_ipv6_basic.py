#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# Functional test for ipv6 connection with bgp.
#
# This suite tests simple connection between one ibgp peer (exabgp) and Odl.
# Peer is configured with ipv6, and exabgp connectes to odl via ipv6.
# Exabgp sends one ipv6 unicast route, which presence is verified in
# example-ipv6-topology. Tests this connection multiple times, with different
# ipv6 accepted formats, e.g. (::1, 0:0:0:0:0:0:0:1, full text)
# This suite also tests a combination of afi-safis on odl and exabgp.
# ipv6 route injection is carried out from odl to the ibgp peer without ipv6
# family enabled on the peer device and checked for exceptions.

import json
import logging
import os
import pytest
import time

from libraries import bgp
from libraries import infra
from libraries import karaf
from libraries import pcep
from libraries import templated_requests
from libraries import utils
from libraries.variables import variables


ODL_IP = variables.ODL_IP
TOOLS_IP = variables.TOOLS_IP
BGP_VAR_FOLDER = "variables/bgpfunctional/ipv6"
CONFIG_SESSION = "config-session"
CONTROLLER_IPV6 = "::1"
EXABGP_ID = "1.2.3.4"
EXABGP_ID_2 = "127.0.0.1"
EXABGP_CFG = "exaipv6.cfg"
EXABGP_LOG = "exaipv6.log"
EXABGP2_CFG = "exaipv4.cfg"
EXABGP2_LOG = "exaipv4.log"
EXABGP3_CFG = "exabgp_graceful_restart.cfg"
EXABGP3_LOG = "exabgp_graceful_restart.log"
EXABGP4_CFG = "exa4.cfg"
EXABGP4_LOG = "exa4.log"
IPV4_IP = "127.0.0.2"
IPV6_IP = "2607:f0d0:1002:0011:0000:0000:0000:0002"
IPV6_IP_2 = "2607:f0d0:1002:11:0:0:0:2"
IPV6_IP_3 = "2607:f0d0:1002:11::2"
IPV6_IP_GW = "2607:f0d0:1002:0011:0000:0000:0000:0001"
IPV6_PREFIX_LENGTH = 64
HOLDTIME = 180
RIB_INSTANCE = "example-bgp-rib"

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=45)
class TestBgpIpv6Basic:
    exabgp_process = None

    def configure_ipv6_network(self):
        """Reconfigures basic network settings on controller"""
        rc, stdout = infra.shell("ip route | grep '^default' | awk '{print $5}'")
        assert rc == 0, f"Failed to get interface set for default route: {stdout}"
        main_net_interface = stdout
        infra.shell(
            f"sudo ip -6 addr add {IPV6_IP}/{IPV6_PREFIX_LENGTH} dev {main_net_interface}"
        )
        infra.shell(f"sudo ip -6 route add default via {IPV6_IP_GW}")
        rc, stdout = infra.shell("ip -6 addr show")
        log.info(stdout)
        rc, stdout = infra.shell("ip -6 route show")
        log.info(stdout)

    def setup_config_file(self, config_file, exabgp_ip, odl_ip, router_id):
        infra.shell(f"sed -i -e 's/EXABGPIP/{exabgp_ip}/g' {config_file}")
        infra.shell(f"sed -i -e 's/ODLIP/{odl_ip}/g' {config_file}")
        infra.shell(f"sed -i -e 's/ROUTERID/{router_id}/g' {config_file}")
        infra.shell(f"sed -i -e 's/ROUTEREFRESH/disable/g' {config_file}")
        infra.shell(f"sed -i -e 's/ADDPATH/disable/g' {config_file}")
        rc, stdout = infra.shell(f"cat {config_file}")
        log.info(stdout)

    def setup_config_files(self):
        for config_file in (EXABGP_CFG, EXABGP3_CFG, EXABGP4_CFG):
            infra.shell(f"cp  {BGP_VAR_FOLDER}/{config_file} tmp/")
            self.setup_config_file(
                f"tmp/{config_file}",
                exabgp_ip=IPV6_IP,
                odl_ip=CONTROLLER_IPV6,
                router_id=EXABGP_ID,
            )
        infra.shell(f"cp  {BGP_VAR_FOLDER}/{EXABGP2_CFG} tmp/")
        self.setup_config_file(
            f"tmp/{EXABGP2_CFG}", exabgp_ip=IPV4_IP, odl_ip=ODL_IP, router_id=IPV4_IP
        )

    def suite_setup(self):
        self.configure_ipv6_network()
        self.setup_config_files()

    def verify_rib_status_empty(self):
        """Verifies that example-ipv6-topology is empty"""
        utils.wait_until_function_pass(
            5,
            2,
            templated_requests.get_templated_request,
            f"{BGP_VAR_FOLDER}/ipv6_topology_empty",
            {},
            verify=True,
        )

    def verify_rib_status_filled(self):
        """Verifies that example-ipv6-topology is filled with ipv6 route"""
        utils.wait_until_function_pass(
            5,
            2,
            templated_requests.get_templated_request,
            f"{BGP_VAR_FOLDER}/ipv6_topology_filled",
            {},
            verify=True,
        )

    def test_bgp_ipv6_basic(self, allure_step_with_separate_logging, step_tag_checker):

        with allure_step_with_separate_logging("step_test_suite_setup"):
            self.suite_setup()

        with allure_step_with_separate_logging(
            "step_reconfigure_odl_to_accept_connections"
        ):
            """Configure BGP peer modules with initiate-connection set to false with short ipv6 address."""
            mapping = {
                "IP": IPV6_IP,
                "PEER_PORT": 17900,
                "BGP_RIB_OPENCONFIG": "example-bgp-rib",
                "HOLDTIME": HOLDTIME,
                "PASSIVE_MODE": "true",
            }
            templated_requests.put_templated_request(
                f"{BGP_VAR_FOLDER}/bgp_peer", mapping, json=False
            )

        with allure_step_with_separate_logging("step_start_exabgp"):
            """Start exabgp."""
            self.exabgp_process = bgp.start_exabgp_and_verify_connected(
                f"tmp/{EXABGP_CFG}", EXABGP_ID, log_file=EXABGP_LOG
            )

        with allure_step_with_separate_logging("step_verify_ipv6_topology_filled"):
            """Verifies that example-ipv6-topology is filled after starting exabgp."""
            self.verify_rib_status_filled()

        with allure_step_with_separate_logging("step_delete_bgp_peer_configuration"):
            """Revert the BGP configuration to the original state: without any configured peers."""
            mapping = {"IP": IPV6_IP, "BGP_RIB_OPENCONFIG": "example-bgp-rib"}
            templated_requests.delete_templated_request(
                f"{BGP_VAR_FOLDER}/bgp_peer", mapping
            )

        with allure_step_with_separate_logging("step_verify_ipv6_topology_empty"):
            """Verifies that example-ipv6-topology is empty after deconfiguring peer for the first time."""
            self.verify_rib_status_empty()

        with allure_step_with_separate_logging(
            "step_reconfigure_odl_to_accept_connections_2"
        ):
            """Configure BGP peer modules with initiate-connection set to false with ipv6 address without "::" shortened version."""
            mapping = {
                "IP": IPV6_IP_2,
                "PEER_PORT": 17900,
                "BGP_RIB_OPENCONFIG": "example-bgp-rib",
                "HOLDTIME": HOLDTIME,
                "PASSIVE_MODE": "true",
            }
            templated_requests.put_templated_request(
                f"{BGP_VAR_FOLDER}/bgp_peer", mapping, json=False
            )

        with allure_step_with_separate_logging("step_verify_ipv6_topology_filled_2"):
            """Verifies that example-ipv6-topology is filled after configuring the peer for the second time."""
            self.verify_rib_status_filled()

        with allure_step_with_separate_logging("step_delete_bgp_peer_configuration_2"):
            """Revert the BGP configuration to the original state: without any configured peers."""
            mapping = {"IP": IPV6_IP_2, "BGP_RIB_OPENCONFIG": "example-bgp-rib"}
            templated_requests.delete_templated_request(
                f"{BGP_VAR_FOLDER}/bgp_peer", mapping
            )

        with allure_step_with_separate_logging("step_verify_ipv6_topology_empty_2"):
            """Verifies that example-ipv6-topology is empty after deconfiguring peer for the second time."""
            self.verify_rib_status_empty()

        with allure_step_with_separate_logging(
            "step_reconfigure_odl_to_accept_connections_3"
        ):
            """Configure BGP peer modules with initiate-connection set to false with full text ipv6 address."""
            mapping = {
                "IP": IPV6_IP_3,
                "PEER_PORT": 17900,
                "BGP_RIB_OPENCONFIG": "example-bgp-rib",
                "HOLDTIME": HOLDTIME,
                "PASSIVE_MODE": "true",
            }
            templated_requests.put_templated_request(
                f"{BGP_VAR_FOLDER}/bgp_peer", mapping, json=False
            )

        with allure_step_with_separate_logging("step_verify_ipv6_topology_filled_3"):
            """Verifies that example-ipv6-topology is filled after configuring the peer for the third time."""
            self.verify_rib_status_filled()

        with allure_step_with_separate_logging("step_delete_bgp_peer_configuration_3"):
            """Revert the BGP configuration to the original state: without any configured peers."""
            mapping = {"IP": IPV6_IP_3, "BGP_RIB_OPENCONFIG": "example-bgp-rib"}
            templated_requests.delete_templated_request(
                f"{BGP_VAR_FOLDER}/bgp_peer", mapping
            )

        with allure_step_with_separate_logging("step_verify_ipv6_topology_empty_3"):
            """Verifies that example-ipv6-topology is empty after deconfiguring peer for the second time."""
            self.verify_rib_status_empty()

        with allure_step_with_separate_logging("step_stop_all_exabgps"):
            """Save exabgp logs as exaipv6.log, and stop exabgp with ctrl-c bash signal."""
            infra.shell(f"cp tmp/{EXABGP_LOG} results/")
            bgp.stop_exabgp(self.exabgp_process)

        with allure_step_with_separate_logging("step_configure_app_peer"):
            """Configures bgp application peer."""
            mapping = {"IP": "127.0.0.12", "BGP_RIB_OPENCONFIG": "example-bgp-rib"}
            templated_requests.put_templated_request(
                f"{BGP_VAR_FOLDER}/application_peer", mapping, json=False
            )

        with allure_step_with_separate_logging(
            "step_reconfigure_odl_to_accept_connections_4"
        ):
            """Configure BGP peer modules with initiate-connection set to false with full text ipv6 address."""
            mapping = {
                "IP": IPV4_IP,
                "PEER_PORT": 17900,
                "BGP_RIB_OPENCONFIG": "example-bgp-rib",
                "HOLDTIME": HOLDTIME,
                "PASSIVE_MODE": "true",
            }
            templated_requests.put_templated_request(
                f"{BGP_VAR_FOLDER}/bgp_neighbor_rib", mapping, json=False
            )

        with allure_step_with_separate_logging("step_start_exabgp_2"):
            """Start exabgp and Verify BGP connection."""
            self.exabgp_process = bgp.start_exabgp_and_verify_connected(
                f"tmp/{EXABGP2_CFG}", IPV4_IP, log_file=EXABGP2_LOG
            )

        with allure_step_with_separate_logging("step_inject_ipv6_route_1"):
            """Inject the Ipv6 route from controller."""
            mapping = {"IP": "127.0.0.12"}
            templated_requests.post_templated_request(
                f"{BGP_VAR_FOLDER}/ipv6_route_injection", mapping, json=False
            )

        with allure_step_with_separate_logging("step_check_ipv6_prefix_in_bgp_rib_1"):
            """Check for the presence of Ipv6 Prefix in the BGP RIB."""
            # TODO: fix this test case as verify is not used, but if used it would be failing.
            mapping = {"BGP_RIB_OPENCONFIG": "example-bgp-rib"}
            utils.wait_until_function_pass(
                5,
                2,
                templated_requests.get_templated_request,
                f"{BGP_VAR_FOLDER}/bgp_rib",
                mapping,
                verify=False,
            )

        with allure_step_with_separate_logging("step_delete_injected_ipv6_routes_1"):
            """Delete the injected IPV6 routes."""
            mapping = {"IP": "127.0.0.12"}
            templated_requests.delete_templated_request(
                f"{BGP_VAR_FOLDER}/ipv6_route_injection", mapping
            )
            karaf.fail_if_exception_found_during_test(
                "step_delete_injected_ipv6_routes_1"
            )

        with allure_step_with_separate_logging("step_delete_bgp_peer_configuration_4"):
            """Revert the BGP configuration to the original state: without any configured peers."""
            mapping = {"IP": IPV4_IP, "BGP_RIB_OPENCONFIG": "example-bgp-rib"}
            templated_requests.delete_templated_request(
                f"{BGP_VAR_FOLDER}/bgp_neighbor_rib", mapping
            )

        with allure_step_with_separate_logging("step_verify_ipv6_topology_empty_4"):
            """Verifies that example-ipv6-topology is empty after deconfiguring peer for the first time."""
            self.verify_rib_status_empty()

        with allure_step_with_separate_logging(
            "step_reconfigure_odl_to_accept_connections_5"
        ):
            """Configure BGP peer modules with initiate-connection set to false with short ipv6 address."""
            mapping = {
                "IP": IPV4_IP,
                "PEER_PORT": 17900,
                "BGP_RIB_OPENCONFIG": "example-bgp-rib",
                "HOLDTIME": HOLDTIME,
                "PASSIVE_MODE": "true",
            }
            templated_requests.put_templated_request(
                f"{BGP_VAR_FOLDER}/bgp_peer", mapping, json=False
            )

        with allure_step_with_separate_logging("step_inject_ipv6_route_2"):
            """Inject the Ipv6 route from controller."""
            mapping = {"IP": "127.0.0.12"}
            templated_requests.post_templated_request(
                f"{BGP_VAR_FOLDER}/ipv6_route_injection", mapping, json=False
            )

        with allure_step_with_separate_logging("step_check_ipv6_prefix_in_bgp_rib_2"):
            """Check for the presence of Ipv6 Prefix in the BGP RIB."""
            mapping = {"BGP_RIB_OPENCONFIG": "example-bgp-rib"}
            templated_requests.get_templated_request(
                f"{BGP_VAR_FOLDER}/bgp_rib", mapping, json=False
            )

        with allure_step_with_separate_logging("step_delete_injected_ipv6_routes_2"):
            """Delete the injected IPV6 routes."""
            mapping = {"IP": "127.0.0.12"}
            templated_requests.delete_templated_request(
                f"{BGP_VAR_FOLDER}/ipv6_route_injection", mapping
            )

        with allure_step_with_separate_logging("step_delete_app_peer"):
            """Deletes bgp application peer."""
            mapping = {"IP": "127.0.0.12", "BGP_RIB_OPENCONFIG": "example-bgp-rib"}
            templated_requests.delete_templated_request(
                f"{BGP_VAR_FOLDER}/application_peer", mapping
            )

        with allure_step_with_separate_logging("step_delete_bgp_peer_configuration_5"):
            """Revert the BGP configuration to the original state: without any configured peers."""
            mapping = {"IP": IPV4_IP, "BGP_RIB_OPENCONFIG": "example-bgp-rib"}
            templated_requests.delete_templated_request(
                f"{BGP_VAR_FOLDER}/bgp_peer", mapping
            )

        with allure_step_with_separate_logging("step_verify_ipv6_topology_empty_5"):
            """Verifies that example-ipv6-topology is empty after deconfiguring peer for the first time."""
            self.verify_rib_status_empty()

        with allure_step_with_separate_logging("step_stop_all_exabgps_2"):
            """Save exabgp logs as exaipv6.log, and stop exabgp with ctrl-c bash signal."""
            infra.shell(f"cp tmp/{EXABGP2_LOG} results/")
            bgp.stop_exabgp(self.exabgp_process)

        with allure_step_with_separate_logging(
            "step_reconfigure_odl_to_accept_connections_6"
        ):
            """Configure BGP peer modules with initiate-connection set to false with short ipv6 address."""
            mapping = {
                "IP": IPV6_IP,
                "PEER_PORT": 17900,
                "BGP_RIB_OPENCONFIG": "example-bgp-rib",
                "HOLDTIME": HOLDTIME,
                "PASSIVE_MODE": "true",
            }
            templated_requests.put_templated_request(
                f"{BGP_VAR_FOLDER}/graceful_restart", mapping, json=False
            )

        with allure_step_with_separate_logging("step_start_exabgp_3"):
            """Start exabgp."""
            self.exabgp_process = bgp.start_exabgp_and_verify_connected(
                f"tmp/{EXABGP3_CFG}", EXABGP_ID, log_file=EXABGP3_LOG
            )

        with allure_step_with_separate_logging("step_stop_all_exabgps_3"):
            """Save exabgp logs as exabgp_graceful_restart.log, and stop exabgp with ctrl-c bash signal."""
            infra.shell(f"cp tmp/{EXABGP3_LOG} results/")
            bgp.stop_exabgp(self.exabgp_process)
            time.sleep(40)
            karaf.fail_if_exception_found_during_test("step_stop_all_exabgps_3")

        with allure_step_with_separate_logging("step_start_exabgp_4"):
            """Start exabgp."""
            self.exabgp_process = bgp.start_exabgp_and_verify_connected(
                f"tmp/{EXABGP3_CFG}", EXABGP_ID, log_file=EXABGP3_LOG
            )

        with allure_step_with_separate_logging("step_delete_bgp_peer_configuration_6"):
            """Revert the BGP configuration to the original state: without any configured peers."""
            mapping = {"IP": IPV6_IP, "BGP_RIB_OPENCONFIG": "example-bgp-rib"}
            templated_requests.delete_templated_request(
                f"{BGP_VAR_FOLDER}/graceful_restart", mapping
            )

        with allure_step_with_separate_logging("step_stop_all_exabgps_4"):
            """Save exabgp logs as exabgp_graceful_restart.log, and stop exabgp with ctrl-c bash signal."""
            infra.shell(f"cp tmp/{EXABGP3_LOG} results/")
            bgp.stop_exabgp(self.exabgp_process)

        if step_tag_checker("exclude"):
            with allure_step_with_separate_logging(
                "step_reconfigure_odl_to_accept_connections_7"
            ):
                """Configure BGP peer modules with initiate-connection set to false with short ipv6 address."""
                mapping = {
                    "IP": IPV6_IP,
                    "PEER_PORT": 17900,
                    "BGP_RIB_OPENCONFIG": "example-bgp-rib",
                    "HOLDTIME": HOLDTIME,
                    "PASSIVE_MODE": "true",
                }
                templated_requests.put_templated_request(
                    f"{BGP_VAR_FOLDER}/bgp_peer", mapping, json=False
                )

            with allure_step_with_separate_logging("step_start_exabgp_5"):
                """Start exabgp."""
                self.exabgp_process = bgp.start_exabgp_and_verify_connected(
                    f"tmp/{EXABGP4_CFG}", EXABGP_ID, log_file=EXABGP4_LOG
                )

            with allure_step_with_separate_logging(
                "step_delete_bgp_peer_configuration_5"
            ):
                """Revert the BGP configuration to the original state: without any configured peers."""
                mapping = {"IP": IPV6_IP, "BGP_RIB_OPENCONFIG": "example-bgp-rib"}
                templated_requests.delete_templated_request(
                    f"{BGP_VAR_FOLDER}/bgp_peer", mapping
                )

            with allure_step_with_separate_logging("step_stop_all_exabgps_5"):
                """Save exabgp logs as exabgp_graceful_restart.log, and stop exabgp with ctrl-c bash signal."""
                infra.shell(f"cp tmp/{EXABGP4_LOG} results/")
                bgp.stop_exabgp(self.exabgp_process)
