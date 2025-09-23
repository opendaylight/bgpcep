#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# This is a suite which has both scale and performance aspects. Given scale
# target, suite reports failures if functional error is detected, or if
# various time limits expire. For passing test cases, their duration is
# the performance metric.
#
# ODL acts as a translation layer between PCEP capable devices and users
# employing RESTCONF. Performance measurement focuses on two different
# workflows.
#
# The first workflow is initial synchronization, when ODL learns the state of
# PCEP topology as devices connect to it, while restconf user reads the state
# repeatedly. The second workflow is mass update, when restconf users issue
# RPCs to updale Layer Switched Paths on Path Computation Clients.
# This suite uses pcc-mock (downloaded from Nexus) to simulate PCCs. It needs
# segment of bindable IP addresses, one for each simulated PCC; so running
# pcc-mock from remote machine is only viable when just single PCC is
# simulated. Testing with multiple PCCs works best when pcc-mock runs on the
# same VM as ODL, so 127.0.0.0/8 subnet can be used.
# Library AuthStandalone is used directly for restconf reads in the first
# workflow. That library transparently handles several http authentication
# methods, based on credentials and pybot arguments.
#
# In the second workflow, updater.py utility is used for issuing rapid
# restconf requests. It can use multiple worker threads, as http requests are
# blocking. Due to CPython interpreter itself being single threaded,
# amounts of threads above 8-16 are actually slightly slower (which may
# roughly correspond to network traffic being more limiting factor than CPU).
# This suite starts updater utility bound to single CPU, as this setup was
# the most performant in other tests.
# In case of failed test case, other tests are skipped (unless this is
# overriden by [Setup]) to finish test run sooner.
# Variables and test case names refer to Controller(ODL_SYSTEM) and Mininet
# (TOOLS_SYSTEM), those are assumed to be separate remote VMs, one to host
# ODL, other to host tools. In case updater and pcc-mock are desired to run
# from separate machines, their parameters use Mininet(TOOLS_SYSTEM) values
# as default. If both updater VM and pcc-mock VM parameters are specified,
# Mininet(TOOLS_SYSTEM) parameters may be skipped. Variable False decides
# the pcc-mock running machine.
# Some launch scripts put restrictions on how pybot options can be specified,
# so there are utility variables to help with copying Controller related value
# to apply fo updater of pccmock. Having a tool co-located with ODL reduces
# network latency, but puts more pressure on CPU and memory on Controller VM.
# In some environments, issues with TIME-WAIT prevent high restconf rates,
# so TCP reuse is temporarily allowed during the suite run, if possible
# (and if not disabled by UPDATERVM_ENABLE_TCP_RW_REUSE option value).
# See http://vincent.bernat.im/en/blog/2014-tcp-time-wait-state-linux.html
# This suite ignores possible failures when changing reuse.
# Similarly, in some environments, handling of requests.Session object matters
# try changing RESTCONF_REUSE value to see if it helps.
# Variables to override (only if needed) in pybot command:
# (Look into Variables table to see the default values.)
# FIRST_PCC_IP: Set in case bind address is different from public pcc-mock
# VM address. LOG_NAME: Filename (without path) to save pcc-mock output into.
# LOG_PATH: Override if not the same as pccmock VM workspace.
# LSPS: Number of LSPs per PCC to simulate and test.
# MOCK_FILE: Filename to use for mock-pcc executable instead of
# the timestamped one.
# ODL_SYSTEM_IP: Numeric IP address of VM where ODL runs.
# ODL_SYSTEM_USER: Username for ssh login to ODL VM.
# ODL_SYSTEM_PASSWORD: Ssh password, empty means public keys are used instead.
# ODL_SYSTEM_PROMPT: Substring to identify Linux prompt on ODL VM.
# ODL_SYSTEM_WORKSPACE: Path to where files can be written on ODL VM.
# PCCDOWNLOAD_HOSTHEADER: Download server may check checks this header before
# showing content.
# PCCDOWNLOAD_URLBASE: URL to pcep-pcc-mock folder in Nexus (use numberic IP
# if DNS has problems).
# PCCMOCK_COLOCATED: If True, set PCCMOCKVM* to mirror ODL_SYSTEM*
# PCCMOCKVM_IP: Override TOOLS_SYSTEM for pcc-mock usage.
# PCCMOCKVM_*: Override corresponding TOOLS_SYSTEM_* for pcc-mock usage.
# PCCS: Number of PCCs to simulate and test.
# PCEP_READY_VERIFY_TIMEOUT: Grace period for pcep-topology to appear.
# Lower if ODL is ready.
# RESTCONF_*: USER, PASSWORD and SCOPE to authenticate with, REUSE session.
# (Note: If SCOPE is not empty, token-based authentication is used.)
# TOOLS_SYSTEM_IP: Numeric IP address of VM to run pcc-mock and updater from
# by default.
# TOOLS_SYSTEM_PASSWORD: Linux password to go with the username
# (empty means keys).
# TOOLS_SYSTEM_PROMPT: Substring to identify Linux prompt on TOOLS_SYSTEM VM.
# TOOLS_SYSTEM_USER: Linux username to SSH to on TOOLS_SYSTEM VM.
# TOOLS_SYSTEM_WORKSPACE: Path to where files may be created on
# TOOLS_SYSTEM VM.
# UPDATER_COLOCATED: If True, overrides UPDATERVM_* parameters to point at
# ODL_SYSTEM (The purpose is to provide an option without ability to unpack
# ODL_SYSTEM value.)
# UPDATER_ODLADDRESS: Override if public ODL_SYSTEM address is not best fit.
# UPDATER_REFRESH: Main updater thread may sleep this long. Balance precision
# with overhead.
# UPDATER_TIMEOUT: If updater stops itself if running more than this time.
# (Set this limit according to your performance target.)
# UPDATERVM_ENABLE_TCP_RW_REUSE: Set to false if changing Linux configuration
# is not desired.
# UPDATERVM_IP: Override TOOLS_SYSTEM for updater.py usage.
# UPDATERVM_*: Override corresponding TOOLS_SYSTEM_* for updater.py usage.


import logging
import os
import pytest
import time

from lib import infra
from lib import pcep
from lib import utils


LSPS = 655
PCCS = 1
TOTAL_LSPS = LSPS * PCCS
LOG_NAME = "throughpcep.log"

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=7)
class TestCases:
    pcc_mock_process = None
    iteration = 1

    def get_next_hop(self):
        self.iteration += 1
        return f"{self.iteration}.{self.iteration}.{self.iteration}.{self.iteration}/32"
    
    def reset_hop_generator(self):
        self.iteration = 0

    def check_stability(self):
        updated_hop = "2.2.2.2/32"
        utils.wait_until_function_pass(90, 5, pcep.check_empty_pcep_topology)
        self.pcc_mock_process = pcep.start_pcc_mock(pcc=PCCS, lsp=LSPS, output_file_name=LOG_NAME)
        rc, stdout = pcep.run_updater(hop=updated_hop, pcc=PCCS, lsp=LSPS, workers=2)
        pcep.check_updater_response(stdout, TOTAL_LSPS, False)
        utils.wait_until_function_returns_value(30, 1, TOTAL_LSPS, pcep.get_pcep_topology_hop_count, updated_hop)
        pcep.stop_pcc_mock_process(self.pcc_mock_process)
        pcep.kill_all_pcc_mock_simulators(gracefully=False)

    def test_cases(self, allure_step_with_separate_logging):

        with allure_step_with_separate_logging("Topology_Precondition"):
            """Verify that within timeout, PCEP topology is present, with no PCC connected."""
            utils.wait_until_function_pass(30, 1, pcep.check_empty_pcep_topology)

        with allure_step_with_separate_logging("Topology_Intercondition"):
            """Verify that within timeout, PCEP topology contains correct numbers of LSPs."""
            self.pcc_mock_process = pcep.start_pcc_mock(pcc=PCCS, lsp=LSPS, output_file_name=LOG_NAME)

        with allure_step_with_separate_logging("Updater_1"):
            """Run updater tool to change hops, using 1 blocking http thread."""
            hop = self.get_next_hop()
            rc, stdout = pcep.run_updater(hop=hop, pcc=PCCS, lsp=LSPS, workers=1)
            pcep.check_updater_response(stdout, TOTAL_LSPS, False)

        with allure_step_with_separate_logging("Verify_1"):
            """Verify that within timeout, the correct number of new hops is in PCEP topology."""
            utils.wait_until_function_returns_value(120, 1, TOTAL_LSPS, pcep.get_pcep_topology_hop_count, hop)
        
        with allure_step_with_separate_logging("Updater_2"):
            """Run updater tool to change hops, using 1 blocking http thread."""
            hop = self.get_next_hop()
            rc, stdout = pcep.run_updater(hop=hop, pcc=PCCS, lsp=LSPS, workers=2)
            pcep.check_updater_response(stdout, TOTAL_LSPS, False)

        with allure_step_with_separate_logging("Verify_2"):
            """Verify that within timeout, the correct number of new hops is in PCEP topology."""
            utils.wait_until_function_returns_value(120, 1, TOTAL_LSPS, pcep.get_pcep_topology_hop_count, hop)

        with allure_step_with_separate_logging("Updater_3"):
            """Run updater tool to change hops, using 1 blocking http thread."""
            hop = self.get_next_hop()
            rc, stdout = pcep.run_updater(hop=hop, pcc=PCCS, lsp=LSPS, workers=4)
            pcep.check_updater_response(stdout, TOTAL_LSPS, False)

        with allure_step_with_separate_logging("Verify_3"):
            """Verify that within timeout, the correct number of new hops is in PCEP topology."""
            utils.wait_until_function_returns_value(120, 1, TOTAL_LSPS, pcep.get_pcep_topology_hop_count, hop)

        with allure_step_with_separate_logging("Updater_4"):
            """Run updater tool to change hops, using 1 blocking http thread."""
            hop = self.get_next_hop()
            rc, stdout = pcep.run_updater(hop=hop, pcc=PCCS, lsp=LSPS, workers=8)
            pcep.check_updater_response(stdout, TOTAL_LSPS, False)

        with allure_step_with_separate_logging("Verify_4"):
            """Verify that within timeout, the correct number of new hops is in PCEP topology."""
            utils.wait_until_function_returns_value(120, 1, TOTAL_LSPS, pcep.get_pcep_topology_hop_count, hop)

        with allure_step_with_separate_logging("Updater_5"):
            """Run updater tool to change hops, using 1 blocking http thread."""
            hop = self.get_next_hop()
            rc, stdout = pcep.run_updater(hop=hop, pcc=PCCS, lsp=LSPS, workers=16)
            pcep.check_updater_response(stdout, TOTAL_LSPS, False)

        with allure_step_with_separate_logging("Verify_5"):
            """Verify that within timeout, the correct number of new hops is in PCEP topology."""
            utils.wait_until_function_returns_value(120, 1, TOTAL_LSPS, pcep.get_pcep_topology_hop_count, hop)

        with allure_step_with_separate_logging("Updater_6"):
            """Run updater tool to change hops, using 1 blocking http thread."""
            hop = self.get_next_hop()
            rc, stdout = pcep.run_updater(hop=hop, pcc=PCCS, lsp=LSPS, workers=32)
            pcep.check_updater_response(stdout, TOTAL_LSPS, False)

        with allure_step_with_separate_logging("Verify_6"):
            """Verify that within timeout, the correct number of new hops is in PCEP topology."""
            utils.wait_until_function_returns_value(120, 1, TOTAL_LSPS, pcep.get_pcep_topology_hop_count, hop)
        
        with allure_step_with_separate_logging("Updater_7"):
            """Run updater tool to change hops, using 1 blocking http thread."""
            hop = self.get_next_hop()
            rc, stdout = pcep.run_updater(hop=hop, pcc=PCCS, lsp=LSPS, workers=64)
            pcep.check_updater_response(stdout, TOTAL_LSPS, False)

        with allure_step_with_separate_logging("Verify_7"):
            """Verify that within timeout, the correct number of new hops is in PCEP topology."""
            utils.wait_until_function_returns_value(120, 1, TOTAL_LSPS, pcep.get_pcep_topology_hop_count, hop)

        with allure_step_with_separate_logging("Updater_8"):
            """Run updater tool to change hops, using 1 blocking http thread."""
            hop = self.get_next_hop()
            rc, stdout = pcep.run_updater(hop=hop, pcc=PCCS, lsp=LSPS, workers=128)
            pcep.check_updater_response(stdout, TOTAL_LSPS, False)

        with allure_step_with_separate_logging("Verify_8"):
            """Verify that within timeout, the correct number of new hops is in PCEP topology."""
            utils.wait_until_function_returns_value(120, 1, TOTAL_LSPS, pcep.get_pcep_topology_hop_count, hop)

        with allure_step_with_separate_logging("Updater_9"):
            """Run updater tool to change hops, using 1 blocking http thread."""
            hop = self.get_next_hop()
            rc, stdout = pcep.run_updater(hop=hop, pcc=PCCS, lsp=LSPS, workers=256)
            pcep.check_updater_response(stdout, TOTAL_LSPS, False)

        with allure_step_with_separate_logging("Verify_9"):
            """Verify that within timeout, the correct number of new hops is in PCEP topology."""
            utils.wait_until_function_returns_value(120, 1, TOTAL_LSPS, pcep.get_pcep_topology_hop_count, hop)

        with allure_step_with_separate_logging("Updater_10"):
            """Run updater tool to change hops, using 1 blocking http thread."""
            hop = self.get_next_hop()
            rc, stdout = pcep.run_updater(hop=hop, pcc=PCCS, lsp=LSPS, workers=512)
            pcep.check_updater_response(stdout, TOTAL_LSPS, False)

        with allure_step_with_separate_logging("Verify_10"):
            """Verify that within timeout, the correct number of new hops is in PCEP topology."""
            utils.wait_until_function_returns_value(120, 1, TOTAL_LSPS, pcep.get_pcep_topology_hop_count, hop)

        with allure_step_with_separate_logging("Updater_with_delegate"):
            """Run updater tool to revoke the delegate for the given hop, using 1 blocking http thread."""
            rc, stdout = pcep.run_updater(hop=hop, pcc=PCCS, lsp=LSPS, workers=1, delegate=False, pccip="127.0.0.1", tunnel_no=2)
            pcep.check_updater_response(stdout, TOTAL_LSPS, True)
            utils.wait_until_function_returns_value(120, 1, TOTAL_LSPS, pcep.get_pcep_topology_hop_count, hop)

        if os.environ["ODL_IP"] == os.environ["TOOLS_IP"]:
            with allure_step_with_separate_logging("Skipped PCEP_sessions_from_multiple_machines"):
                log.warn("There is no point in running this step if only one machine is used, "
                "ODL and TOOLs system is the same machine.")
        else:
            with allure_step_with_separate_logging("PCEP_sessions_from_multiple_machines"):
                """Start two instances of pcep mock simulator one on ODL host and one on TOOLS host.
                If both of these hosts are the same ODL and TOOLs this step is skipped"""
                rc, stdout = pcep.run_updater(hop=hop, pcc=PCCS, lsp=LSPS, workers=1, delegate=False, pccip="127.0.0.1", tunnel_no=2)
                pcep.check_updater_response(stdout, TOTAL_LSPS, True)
                utils.wait_until_function_returns_value(120, 1, TOTAL_LSPS, pcep.get_pcep_topology_hop_count, hop)

        with allure_step_with_separate_logging("Stop_Pcc_Mock"):
            """Send ctrl+c to pcc-mock, see prompt again within timeout."""
            pcep.stop_pcc_mock_process(self.pcc_mock_process)
            pcep.kill_all_pcc_mock_simulators(gracefully=False)
            utils.wait_until_function_pass(30, 5, pcep.check_empty_pcep_topology)

        with allure_step_with_separate_logging("PCEP_Sessions_Flapped_with_LSP_updates"):
            """Flapping PCEP sessions and perform LSP updates within flapping."""
            pccs = 10
            lsps = 15
            total_lsps = pccs * lsps
            updated_hop = "2.2.2.2/32"
            for _ in range(15):
                utils.wait_until_function_pass(300, 5, pcep.check_empty_pcep_topology)
                self.pcc_mock_process = pcep.start_pcc_mock(pcc=pccs, lsp=lsps, output_file_name="serial_execution.log")
                rc, stdout = pcep.run_updater(hop=updated_hop, pcc=pccs, lsp=lsps, workers=1)
                pcep.check_updater_response(stdout, total_lsps, False)
                utils.wait_until_function_returns_value(60, 5, total_lsps, pcep.get_pcep_topology_hop_count, updated_hop)
                pcep.stop_pcc_mock_process(self.pcc_mock_process)
            self.check_stability()

        with allure_step_with_separate_logging("PCEP_Sessions_Flapped_alongside_LSP_updates"):
            """Flapping PCEP sessions and perform LSP updates alongside flapping."""
            pccs = 10
            lsps = 15
            total_lsps = pccs * lsps
            updated_hop = "2.2.2.2/32"
            utils.wait_until_function_pass(300, 5, pcep.check_empty_pcep_topology)
            pcc_mock_script_process = pcep.start_pcc_mock_with_flapping(local_address="127.0.0.1", remote_address="127.0.0.1", pcc=pccs, lsp=lsps, log_file_name="throughpcep_parallel_Execution.log", interval=10)
            for _ in range(10):
                hop = self.get_next_hop()
                rc, output = infra.shell("ps -fu $WHOAMI | grep 'pcep-pcc-mock.jar' | grep -v 'grep' | awk '{print $2}'")
                if output:
                    rc, stdout = pcep.run_updater(hop=hop, pcc=pccs, lsp=lsps, workers=1)
                    pcep.check_updater_response(stdout, total_lsps, True)
            infra.stop_process(pcc_mock_script_process)
            pcep.kill_all_pcc_mock_simulators(gracefully=False)
            self.check_stability()

        with allure_step_with_separate_logging("Download_Pccmock_Log"):
                "Transfer pcc-mock output from tmp dir to results."
                infra.shell(
                    f"mv tmp/{LOG_NAME} results/{LOG_NAME}",
                    check_rc=True,
                )
                infra.shell(
                    f"mv tmp/serial_execution.log results/serial_execution.log",
                    check_rc=True,
                )
                infra.shell(
                    f"mv tmp/throughpcep_parallel_Execution.log results/throughpcep_parallel_Execution.log",
                    check_rc=True,
                )
        with allure_step_with_separate_logging("Topology_Postcondition"):
                "Verify that within timeout, PCEP topology contains no PCCs again."
                utils.wait_until_function_pass(90, 5, pcep.check_empty_pcep_topology)
