#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import ipaddress
import logging
import textwrap

import allure
from jinja2 import Environment, FileSystemLoader
import pytest

from libraries import bgp
from libraries import infra
from libraries import prefix_counting
from libraries import templated_requests
from libraries import utils
from libraries.variables import variables


BGP_PEERS_COUNT = 20
ODL_IP = variables.ODL_IP
ODL_BGP_PORT = variables.ODL_BGP_PORT
TOOLS_IP = variables.TOOLS_IP
BGP_TOOL_PORT = variables.BGP_TOOL_PORT
BGP_VARIABLES_FOLDER = "variables/bgpuser/"
BGP_PEER_LOG_LEVEL = "debug"
HOLDTIME = 180
ODL_LOG_LEVEL = "INFO"
ODL_BGP_LOG_LEVEL = "DEFAULT"
iBGP_PEER1_IP = "127.0.1.0"
eBGP_PEER1_IP = "127.0.2.0"
eBGP_PEER2_IP = "127.0.3.0"
iBGP_PEER1_FIRST_PREFIX_IP = "8.1.0.0"
eBGP_PEERS_FIRST_PREFIX_IP = "8.0.0.0"
eBGP_PEER1_FIRST_PREFIX_IP = eBGP_PEERS_FIRST_PREFIX_IP
eBGP_PEER2_FIRST_PREFIX_IP = eBGP_PEERS_FIRST_PREFIX_IP
eBGP_PEER1_NEXT_HOP = "1.1.1.1"
eBGP_PEER2_NEXT_HOP = "2.2.2.2"
PREFIX_LEN = 28
iBGP_PEER1_PREFIX_LEN = PREFIX_LEN
eBGP_PEER1_PREFIX_LEN = PREFIX_LEN
eBGP_PEER2_PREFIX_LEN = PREFIX_LEN
eBGP_PEERS_AS = 32768
eBGP_PEER1_AS = eBGP_PEERS_AS
eBGP_PEER2_AS = eBGP_PEERS_AS
iBGP_PEER1_LOG_FILE = "bgp_peer1.log"
eBGP_PEER1_LOG_FILE = "ebgp_peer1.log"
eBGP_PEER2_LOG_FILE = "ebgp_peer2.log"
iBGP_PEERS1_COUNT = int(BGP_PEERS_COUNT / 2)
eBGP_PEERS1_COUNT = int(BGP_PEERS_COUNT / 2)
eBGP_PEERS2_COUNT = int(BGP_PEERS_COUNT / 2)
iBGP_PEER1_PREFIX_COUNT = 0
eBGP_PEER1_PREFIX_COUNT = eBGP_PEERS1_COUNT
eBGP_PEER2_PREFIX_COUNT = eBGP_PEERS2_COUNT
iBGP_PEER1_COMMAND = (
    f"python3 tools/fastbgp/play.py --multiplicity {iBGP_PEERS1_COUNT} "
    f"--firstprefix {iBGP_PEER1_FIRST_PREFIX_IP} --prefixlen {iBGP_PEER1_PREFIX_LEN} "
    f"--amount {iBGP_PEER1_PREFIX_COUNT} --myip={iBGP_PEER1_IP} "
    f"--myport={BGP_TOOL_PORT} --peerip={ODL_IP} --peerport={ODL_BGP_PORT} "
    f"--{BGP_PEER_LOG_LEVEL} --logfile {iBGP_PEER1_LOG_FILE}"
)
eBGP_PEER1_COMMAND = (
    f"python3 tools/fastbgp/play.py --multiplicity {eBGP_PEERS1_COUNT} "
    f"--firstprefix {eBGP_PEER1_FIRST_PREFIX_IP} --prefixlen {eBGP_PEER1_PREFIX_LEN} "
    f"--amount {eBGP_PEER1_PREFIX_COUNT} --myip={eBGP_PEER1_IP} "
    f"--myport={BGP_TOOL_PORT} --peerip={ODL_IP} --peerport={ODL_BGP_PORT} "
    f"--nexthop {eBGP_PEER1_NEXT_HOP} --asnumber {eBGP_PEER1_AS} "
    f"--{BGP_PEER_LOG_LEVEL} --logfile {eBGP_PEER1_LOG_FILE}"
)
eBGP_PEER2_COMMAND = (
    f"python3 tools/fastbgp/play.py --multiplicity {eBGP_PEERS2_COUNT} "
    f"--firstprefix {eBGP_PEER2_FIRST_PREFIX_IP} --prefixlen {eBGP_PEER2_PREFIX_LEN} "
    f"--amount {eBGP_PEER2_PREFIX_COUNT} --myip={eBGP_PEER2_IP} "
    f"--myport={BGP_TOOL_PORT} --peerip={ODL_IP} --peerport={ODL_BGP_PORT} "
    f"--nexthop {eBGP_PEER2_NEXT_HOP} --asnumber {eBGP_PEER2_AS} "
    f"--{BGP_PEER_LOG_LEVEL} --logfile {eBGP_PEER2_LOG_FILE}"
)
iBGP_PEER1_OPTIONS = f"2>&1 >{iBGP_PEER1_LOG_FILE}"
eBGP_PEER1_OPTIONS = f"2>&1 >{eBGP_PEER1_LOG_FILE}"
eBGP_PEER2_OPTIONS = f"2>&1 >{eBGP_PEER2_LOG_FILE}"
DEFAULT_LOG_CHECK_TIMEOUT = "20s"
DEFAULT_LOG_CHECK_PERIOD = "1s"
DEFAULT_TOPOLOGY_CHECK_COUNT = 10
DEFAULT_TOPOLOGY_CHECK_PERIOD = 1
RIB_INSTANCE = "example-bgp-rib"
PROTOCOL_OPENCONFIG = RIB_INSTANCE
DEVICE_NAME = "controller-config"
DEFAULT_AS = 64496
LOCAL_AS = 65432
eBGP_AS = 64497

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=70)
class TestEbgpPeersBasic:
    bgp_peer1_process = None
    bgp_peer2_process = None

    def setup_everything(self):
        """Configure karaf logging level"""
        infra.execute_karaf_command(f"log:set {ODL_LOG_LEVEL}")
        infra.execute_karaf_command(
            f"log:set ${ODL_BGP_LOG_LEVEL} org.opendaylight.bgpcep"
        )
        infra.execute_karaf_command(
            f"log:set ${ODL_BGP_LOG_LEVEL} org.opendaylight.protocol"
        )

    def validate_rib(self, ip, skipped_prefix):
        step = 1 << (32 - PREFIX_LEN)
        routes = []
        base_prefix = ipaddress.IPv4Address(iBGP_PEER1_FIRST_PREFIX_IP)
        for i in range(iBGP_PEERS1_COUNT):
            prefix = str(base_prefix + i * step)
            if prefix == skipped_prefix:
                continue
            route = {"prefix": prefix, "peer_as": DEFAULT_AS}
            routes.append(route)
        base_prefix = ipaddress.IPv4Address(eBGP_PEER1_FIRST_PREFIX_IP)
        for i in range(eBGP_PEERS1_COUNT):
            prefix = str(base_prefix + i * step)
            if prefix == skipped_prefix:
                continue
            route = {"prefix": prefix, "peer_as": eBGP_AS}
            routes.append(route)
        mapping = {
            "IP": ip,
            "BGP_RIB_OPENCONFIG": PROTOCOL_OPENCONFIG,
            "AS_NUMBER": LOCAL_AS,
            "ROUTES": routes,
        }
        env = Environment(
            loader=FileSystemLoader(
                f"{BGP_VARIABLES_FOLDER}/local_as/manypeers_adj_rib_out"
            )
        )
        template = env.get_template("data.j2")
        expected_rib = template.render(mapping)
        response = templated_requests.get_templated_request(
            f"{BGP_VARIABLES_FOLDER}/local_as/adj_rib_out", mapping
        )
        utils.verify_jsons_match(
            response.text,
            expected_rib,
            "received response",
            "expected response",
        )

    @allure.description(
        textwrap.dedent("""
            **Basic tests for eBGP application peers.**

            Test suite performs basic eBGP functional tests: Twenty eBGP peers \
            (two peer groups) advertise the same group of prefixes (aka BGP HA)
            *https://wiki.opendaylight.org/view/BGP_LS_PCEP:BGP*

            **Reported bugs:**
            **Bug 4834** - ODL controller announces the same route twice \
            (two eBGP scenario aka HA)
            **Bug 4835** - Routes not withdrawn when eBGP peers are disconnected \
            (the same prefixes announced)

            For versions Fluorine and above, there are test cases: TC_LAS \
            (test case local AS):
            - configuration of ebgp with local-as and ibgp without local-as
            - connect bgp speakers (play.py) to both peers groups and check their \
            connection
            - check adj-rib-out on both peers gropus, expecting local-as \
            in as-sequence on both peers.

            **TODO**: Extend testsuite by tests dedicated to path selection algorithm
            **TODO**: Choose functions used by more than one test suite to be placed \
            in a common place.
        """)
    )
    def test_ebgp_peers_basic(self, allure_step_with_separate_logging):
        with allure_step_with_separate_logging("step_configure_bgp_peers"):
            """Configure an iBGP and two eBGP peers groups."""
            for i in range(iBGP_PEERS1_COUNT):
                mapping = {
                    "IP": f"127.0.1.{i}",
                    "HOLDTIME": HOLDTIME,
                    "PEER_PORT": BGP_TOOL_PORT,
                    "BGP_RIB_OPENCONFIG": PROTOCOL_OPENCONFIG,
                    "RR_CLIENT": "false",
                    "PASSIVE_MODE": "true",
                }
                templated_requests.put_templated_request(
                    f"{BGP_VARIABLES_FOLDER}/ibgp_peers", mapping, json=False
                )
            for i in range(eBGP_PEERS1_COUNT):
                mapping = {
                    "IP": f"127.0.2.{i}",
                    "HOLDTIME": HOLDTIME,
                    "PEER_PORT": BGP_TOOL_PORT,
                    "BGP_RIB_OPENCONFIG": PROTOCOL_OPENCONFIG,
                    "RR_CLIENT": "false",
                    "PASSIVE_MODE": "true",
                    "AS_NUMBER": eBGP_PEERS_AS,
                }
                templated_requests.put_templated_request(
                    f"{BGP_VARIABLES_FOLDER}/ebgp_peers", mapping, json=False
                )
            for i in range(eBGP_PEERS2_COUNT):
                mapping = {
                    "IP": f"127.0.3.{i}",
                    "HOLDTIME": HOLDTIME,
                    "PEER_PORT": BGP_TOOL_PORT,
                    "BGP_RIB_OPENCONFIG": PROTOCOL_OPENCONFIG,
                    "RR_CLIENT": "false",
                    "PASSIVE_MODE": "true",
                    "AS_NUMBER": eBGP_PEERS_AS,
                }
                templated_requests.put_templated_request(
                    f"{BGP_VARIABLES_FOLDER}/ebgp_peers", mapping, json=False
                )

        with allure_step_with_separate_logging("step_connect_ibgp_peer1"):
            """Connect BGP peers."""
            self.ibgp_peer1_process = infra.shell(
                f"{iBGP_PEER1_COMMAND} {iBGP_PEER1_OPTIONS}", run_in_background=True
            )
            utils.verify_process_did_not_stop_immediately(self.ibgp_peer1_process.pid)
            prefix_counting.check_example_ipv4_topology_does_not_contain("prefix")

        with allure_step_with_separate_logging("step_connect_ebgp_peer1"):
            """Connect BGP peers."""
            self.ebgp_peer1_process = infra.shell(
                f"{eBGP_PEER1_COMMAND} {eBGP_PEER1_OPTIONS}", run_in_background=True
            )
            utils.verify_process_did_not_stop_immediately(self.ebgp_peer1_process.pid)

        with allure_step_with_separate_logging(
            "step_check_ipv4_topology_for_first_path"
        ):
            """The IPv4 topology shall contain the route announced by the first
            eBGP group."""
            utils.wait_until_function_pass(
                DEFAULT_TOPOLOGY_CHECK_COUNT,
                DEFAULT_TOPOLOGY_CHECK_PERIOD,
                bgp.check_example_ipv4_topology_content,
                f'"node-id":"{eBGP_PEER1_NEXT_HOP}"',
            )
            bgp.check_example_ipv4_topology_content(
                f'"prefix":"{eBGP_PEER1_FIRST_PREFIX_IP}/{PREFIX_LEN}"',
            )

        with allure_step_with_separate_logging(
            "step_ibgp_check_log_for_introduced_prefixes"
        ):
            """Check incomming updates for introduced routes."""
            utils.wait_until_function_pass(
                DEFAULT_TOPOLOGY_CHECK_COUNT,
                DEFAULT_TOPOLOGY_CHECK_PERIOD,
                infra.verify_string_occurence_count_in_file,
                "nlri_prefix_received:",
                iBGP_PEER1_LOG_FILE,
                iBGP_PEERS1_COUNT * (iBGP_PEERS1_COUNT - 1 + eBGP_PEER1_PREFIX_COUNT),
            )
            count = bgp.count_key_value_pairs(
                iBGP_PEER1_LOG_FILE, "Network Address of Next Hop", eBGP_PEER1_NEXT_HOP
            )
            assert count == iBGP_PEERS1_COUNT * eBGP_PEER1_PREFIX_COUNT
            count = bgp.count_key_value_pairs(
                iBGP_PEER1_LOG_FILE, "Network Address of Next Hop", eBGP_PEER2_NEXT_HOP
            )
            assert count == 0

        with allure_step_with_separate_logging("step_connect_ebgp_peer2"):
            """Check incomming updates for introduced routes."""
            self.ebgp_peer2_process = infra.shell(
                f"{eBGP_PEER2_COMMAND} {eBGP_PEER2_OPTIONS}", run_in_background=True
            )
            utils.verify_process_did_not_stop_immediately(self.ebgp_peer2_process.pid)

        with allure_step_with_separate_logging("step_disconnect_ebgp_peer1"):
            """Stop BGP peer and store logs."""
            bgp.stop_bgp_speaker(self.ebgp_peer1_process)
            infra.backup_file(eBGP_PEER1_LOG_FILE)

        with allure_step_with_separate_logging(
            "step_check_ipv4_topology_for_second_path"
        ):
            """The IPv4 topology shall contain the route announced by
            the second eBGP group now."""
            utils.wait_until_function_pass(
                DEFAULT_TOPOLOGY_CHECK_COUNT,
                DEFAULT_TOPOLOGY_CHECK_PERIOD,
                bgp.check_example_ipv4_topology_content,
                f'"node-id":"${eBGP_PEER2_NEXT_HOP}"',
            )
            utils.wait_until_function_pass(
                DEFAULT_TOPOLOGY_CHECK_COUNT,
                DEFAULT_TOPOLOGY_CHECK_PERIOD,
                bgp.check_example_ipv4_topology_content,
                f'"prefix":"{eBGP_PEER2_FIRST_PREFIX_IP}/{PREFIX_LEN}"',
            )

        with allure_step_with_separate_logging(
            "step_ibgp_check_log_for_updated_prefixes"
        ):
            """Check incomming updates for updated routes."""
            total_prefix_count = iBGP_PEERS1_COUNT * (
                iBGP_PEERS1_COUNT
                - 1
                + eBGP_PEER1_PREFIX_COUNT
                + eBGP_PEER2_PREFIX_COUNT
            )
            utils.wait_until_function_pass(
                DEFAULT_TOPOLOGY_CHECK_COUNT,
                DEFAULT_TOPOLOGY_CHECK_PERIOD,
                infra.verify_string_occurence_count_in_file,
                "nlri_prefix_received:",
                iBGP_PEER1_LOG_FILE,
                total_prefix_count,
            )
            count = bgp.count_key_value_pairs(
                iBGP_PEER1_LOG_FILE, "Network Address of Next Hop", eBGP_PEER1_NEXT_HOP
            )
            assert count == iBGP_PEERS1_COUNT * eBGP_PEER1_PREFIX_COUNT
            count = bgp.count_key_value_pairs(
                iBGP_PEER1_LOG_FILE, "Network Address of Next Hop", eBGP_PEER2_NEXT_HOP
            )
            assert count == iBGP_PEERS1_COUNT * eBGP_PEER2_PREFIX_COUNT

        with allure_step_with_separate_logging("step_disconnect_ebgp_peer2"):
            """Stop BGP peers and store logs."""
            bgp.stop_bgp_speaker(self.ebgp_peer2_process)
            infra.backup_file(eBGP_PEER2_LOG_FILE)

        with allure_step_with_separate_logging("step_check_for_empty_ipv4_topology"):
            """The IPv4 topology shall be empty."""
            utils.wait_until_function_pass(
                DEFAULT_TOPOLOGY_CHECK_COUNT,
                DEFAULT_TOPOLOGY_CHECK_PERIOD,
                prefix_counting.check_example_ipv4_topology_does_not_contain,
                "prefix",
            )

        with allure_step_with_separate_logging(
            "step_ibgp_check_log_for_withdrawn_prefixes"
        ):
            """Check incomming updates for withdrawn routes."""
            prefixes_to_be_removed = (
                max(eBGP_PEER1_PREFIX_COUNT, eBGP_PEER2_PREFIX_COUNT)
                * iBGP_PEERS1_COUNT
            )
            utils.wait_until_function_pass(
                DEFAULT_TOPOLOGY_CHECK_COUNT,
                DEFAULT_TOPOLOGY_CHECK_PERIOD,
                infra.verify_string_occurence_count_in_file,
                "withdrawn_prefix_received:",
                iBGP_PEER1_LOG_FILE,
                prefixes_to_be_removed,
            )

        with allure_step_with_separate_logging("step_disconnect_ibgp_peer1"):
            """Stop BGP peer and store logs."""
            bgp.stop_bgp_speaker(self.ibgp_peer1_process)
            infra.backup_file(iBGP_PEER1_LOG_FILE)

        with allure_step_with_separate_logging("step_delete_bgp_peers_configuration"):
            """Delete all previously configured BGP peers."""
            for i in range(iBGP_PEERS1_COUNT):
                mapping = {
                    "IP": f"127.0.1.{i}",
                    "BGP_RIB_OPENCONFIG": PROTOCOL_OPENCONFIG,
                }
                templated_requests.delete_templated_request(
                    f"{BGP_VARIABLES_FOLDER}/ibgp_peers", mapping
                )
            for i in range(eBGP_PEERS1_COUNT):
                mapping = {
                    "IP": f"127.0.2.{i}",
                    "BGP_RIB_OPENCONFIG": PROTOCOL_OPENCONFIG,
                }
                templated_requests.delete_templated_request(
                    f"{BGP_VARIABLES_FOLDER}/ebgp_peers", mapping
                )
            for i in range(eBGP_PEERS2_COUNT):
                mapping = {
                    "IP": f"127.0.3.{i}",
                    "BGP_RIB_OPENCONFIG": PROTOCOL_OPENCONFIG,
                }
                templated_requests.delete_templated_request(
                    f"{BGP_VARIABLES_FOLDER}/ebgp_peers", mapping
                )

        with allure_step_with_separate_logging(
            "step_tc_las_reconfigure_odl_to_accept_connection"
        ):
            """Configure neighbors. One ibgp and one ebgp group neighbors with
            local-as configured."""
            for i in range(iBGP_PEERS1_COUNT):
                mapping = {
                    "IP": f"127.0.1.{i}",
                    "HOLDTIME": HOLDTIME,
                    "PEER_PORT": BGP_TOOL_PORT,
                    "BGP_RIB_OPENCONFIG": PROTOCOL_OPENCONFIG,
                    "PASSIVE_MODE": "true",
                }
                templated_requests.put_templated_request(
                    f"{BGP_VARIABLES_FOLDER}/bgp_peer", mapping, json=False
                )
            for i in range(eBGP_PEERS1_COUNT):
                mapping = {
                    "IP": f"127.0.2.{i}",
                    "HOLDTIME": HOLDTIME,
                    "PEER_PORT": BGP_TOOL_PORT,
                    "BGP_RIB_OPENCONFIG": PROTOCOL_OPENCONFIG,
                    "PASSIVE_MODE": "true",
                    "AS_NUMBER": LOCAL_AS,
                    "PEER_AS": eBGP_AS,
                }
                templated_requests.put_templated_request(
                    f"{BGP_VARIABLES_FOLDER}/local_as/ebgp_peer", mapping, json=False
                )

        with allure_step_with_separate_logging(
            "step_tc_las_start_ibgp_speaker_and_verify_connected"
        ):
            """Verify that peers are present in odl's rib. Peers are
            configured with local-as."""
            self.bgp_speaker_process = bgp.start_bgp_speaker_and_verify_connected(
                speaker_ips=[f"127.0.1.{i}" for i in range(iBGP_PEERS1_COUNT)],
                firstprefix=iBGP_PEER1_FIRST_PREFIX_IP,
                prefixlen=PREFIX_LEN,
                ammount=iBGP_PEERS1_COUNT,
                my_ip=iBGP_PEER1_IP,
                my_port=BGP_TOOL_PORT,
                peer_ip=ODL_IP,
                peer_port=ODL_BGP_PORT,
                multiplicity=iBGP_PEERS1_COUNT,
                log_level="debug",
            )

        with allure_step_with_separate_logging(
            "step_tc_las_start_ebgp_speaker_and_verify_connected"
        ):
            """Verify that peers are present in odl's rib. Peers are
            configured with local-as."""
            self.bgp_speaker_process = bgp.start_bgp_speaker_and_verify_connected(
                speaker_ips=[f"127.0.2.{i}" for i in range(eBGP_PEERS1_COUNT)],
                firstprefix=eBGP_PEER1_FIRST_PREFIX_IP,
                prefixlen=PREFIX_LEN,
                ammount=eBGP_PEERS1_COUNT,
                asnumber=eBGP_AS,
                my_ip=eBGP_PEER1_IP,
                my_port=BGP_TOOL_PORT,
                peer_ip=ODL_IP,
                peer_port=ODL_BGP_PORT,
                multiplicity=eBGP_PEERS1_COUNT,
                log_level="debug",
            )

        with allure_step_with_separate_logging("step_tc_las_verify_ibgp_rib_out"):
            """Verifies iBGP's adj-rib-out output. Expects local-as, and ebgp
            peer-as presence."""
            step = 1 << (32 - PREFIX_LEN)
            base_ip = int(ipaddress.IPv4Address(iBGP_PEER1_IP))
            base_prefix = int(ipaddress.IPv4Address(iBGP_PEER1_FIRST_PREFIX_IP))
            for i in range(iBGP_PEERS1_COUNT):
                ip = base_ip + i
                prefix = base_prefix + i * step
                utils.wait_until_function_pass(
                    DEFAULT_TOPOLOGY_CHECK_COUNT,
                    DEFAULT_TOPOLOGY_CHECK_PERIOD,
                    self.validate_rib,
                    ip,
                    prefix,
                )

        with allure_step_with_separate_logging("step_tc_las_verify_ebgp_rib_out"):
            """Verifies eBGP's adj-rib-out output. Expects local-as, and ibgp
            peer-as presence."""
            step = 1 << (32 - PREFIX_LEN)
            base_ip = int(ipaddress.IPv4Address(eBGP_PEER1_IP))
            base_prefix = int(ipaddress.IPv4Address(eBGP_PEER1_FIRST_PREFIX_IP))
            for i in range(eBGP_PEERS1_COUNT):
                ip = base_ip + i
                prefix = base_prefix + i * step
                utils.wait_until_function_pass(
                    DEFAULT_TOPOLOGY_CHECK_COUNT,
                    DEFAULT_TOPOLOGY_CHECK_PERIOD,
                    self.validate_rib,
                    ip,
                    prefix,
                )

        with allure_step_with_separate_logging(
            "step_tc_las_kill_ibgp_speaker_after_talking"
        ):
            """Abort the Python speaker."""
            bgp.stop_bgp_speaker(self.ibgp_peer1_process)

        with allure_step_with_separate_logging(
            "step_tc_las_kill_ibgp_speaker_after_talking"
        ):
            """Abort the Python speaker."""
            bgp.stop_bgp_speaker(self.ebgp_peer1_process)

        with allure_step_with_separate_logging(
            "step_tc_las_delete_bgp_peer_configurations"
        ):
            """Delete peer configuration."""
            for i in range(iBGP_PEERS1_COUNT):
                mapping = {
                    "IP": f"127.0.1.{i}",
                    "BGP_RIB_OPENCONFIG": PROTOCOL_OPENCONFIG,
                }
                templated_requests.delete_templated_request(
                    f"{BGP_VARIABLES_FOLDER}/bgp_peer", mapping
                )
            for i in range(eBGP_PEERS2_COUNT):
                mapping = {
                    "IP": f"127.0.2.{i}",
                    "BGP_RIB_OPENCONFIG": PROTOCOL_OPENCONFIG,
                }
                templated_requests.delete_templated_request(
                    f"{BGP_VARIABLES_FOLDER}/local_as/ebgp_peer", mapping
                )
