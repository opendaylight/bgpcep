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
# employing RESTCONF. Performance measurement focuses on two different workflows.
#
# The first workflow is initial synchronization, when ODL learns the state of
# PCEP topology as devices connect to it, while RESTCONF user reads the state
# repeatedly. The second workflow is mass update, when RESTCONF users issue
# RPCs to updale Layer Switched Paths on Path Computation Clients.
# This suite uses pcc-mock (pre-build) to simulate PCCs. It needs
# segment of bindable IP addresses, one for each simulated PCC; so running
# pcc-mock from remote machine is only viable when just single PCC is
# simulated. Testing with multiple PCCs works best when pcc-mock runs on the
# same VM as ODL, so 127.0.0.0/8 subnet can be used.
# Library AuthStandalone is used directly for RESTCONF reads in the first
# workflow. That library transparently handles several http authentication
# methods, based on credentials and other arguments.
#
# In the second workflow, updater.py utility is used for issuing rapid
# RESTCONF requests. It can use multiple worker threads, as http requests are
# blocking. Due to CPython interpreter itself being single threaded,
# amounts of threads above 8-16 are actually slightly slower (which may
# roughly correspond to network traffic being more limiting factor than CPU).
# This suite starts updater utility bound to single CPU, as this setup was
# the most performant in other tests.
# In case of failed step, whole test fails to finish test run sooner.
# Having a tool co-located with ODL reduces network latency, but puts more pressure
# on CPU and memory on Controller VM.
# In some environments, issues with TIME-WAIT prevent high restconf rates,
# so TCP reuse is temporarily allowed during the suite run, if possible
# (and if not disabled by UPDATERVM_ENABLE_TCP_RW_REUSE option value).
# See http://vincent.bernat.im/en/blog/2014-tcp-time-wait-state-linux.html
# This suite ignores possible failures when changing reuse.
# Similarly, in some environments, handling of requests.Session object matters
# try changing RESTCONF_REUSE value to see if it helps.


# Variables to override (only if needed):
# LOG_NAME: Filename (without path) to save pcc-mock output into.
# LSPS: Number of LSPs per PCC to simulate and test.
# ODL_IP: Numeric IP address of VM where ODL runs.
# ODL_USER: Username for ssh login to ODL VM.
# ODL_PASSWD: Ssh password, empty means public keys are used instead.
# PCCS: Number of PCCs to simulate and test.
# PCEP_READY_VERIFY_TIMEOUT: Grace period for pcep-topology to appear.
# Lower if ODL is ready.
# TOOLS_IP: Numeric IP address of VM to run pcc-mock and updater from by default.
# TOOLS_PASSWD: Linux password to go with the username (empty means keys).
# TOOLS_USER: Linux username to SSH to on TOOLS_SYSTEM VM.
# UPDATER_REFRESH: Main updater thread may sleep this long. Balance precision
# with overhead.
# UPDATER_TIMEOUT: If updater stops itself if running more than this time.
# (Set this limit according to your performance target.)
# ENABLE_TCP_TW_REUSE: Set to false if changing Linux configuration is not desired.


import logging
import pytest

from libraries import infra
from libraries import pcep
from libraries import utils
from libraries.variables import variables


LSPS = 65535
PCCS = 1
TOTAL_LSPS = LSPS * PCCS
PCEP_READY_VERIFY_TIMEOUT = 180
UPDATER_REFRESH = 0.1
UPDATER_TIMEOUT = 900
LOG_NAME = "throughpcep.log"
ENABLE_TCP_TW_REUSE = True
RESTCONF_REUSE = True

ODL_IP = variables.ODL_IP
ODL_USER = variables.ODL_USER
ODL_PASSWD = variables.ODL_PASSWORD
TOOLS_IP = variables.TOOLS_IP
TOOLS_USER = variables.TOOLS_USER
TOOLS_PASSWD = variables.TOOLS_PASSWORD

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.run(order=12)
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
        self.pcc_mock_process = pcep.start_pcc_mock(
            pcc=self.pccs,
            local_address=ODL_IP,
            remote_address=ODL_IP,
            lsp=self.lsps,
            log_file_name=LOG_NAME
        )
        rc, stdout = pcep.run_updater(
            hop=updated_hop,
            pcc=PCCS,
            lsp=LSPS,
            pccaddress=ODL_IP,
            odladdress=ODL_IP,
            workers=2,
            reuse=RESTCONF_REUSE,
            refresh=UPDATER_REFRESH,
            timeout=UPDATER_TIMEOUT,
        )
        pcep.check_updater_response(stdout, TOTAL_LSPS, False)
        utils.wait_until_function_returns_value(
            30, 1, TOTAL_LSPS, pcep.get_pcep_topology_hop_count, updated_hop
        )
        pcep.stop_pcc_mock_process(self.pcc_mock_process)
        pcep.kill_all_pcc_mock_simulators(gracefully=False)

    def test_cases(self, allure_step_with_separate_logging):

        if not ENABLE_TCP_TW_REUSE:
            with allure_step_with_separate_logging(
                "Skipped: step_save_and_enable_tcp_tw_reuse"
            ):
                log.warn(
                    "There is no point in running this step if changing TCP RW REUSE is prohibited."
                )
        else:
            with allure_step_with_separate_logging("step_save_and_enable_tcp_tw_reuse"):
                """If requested, temporarily enable TCP port reuse to allow for high rate of
                TCP connections. Do not fail whole test on possible step failures.
                In case of failure only log the erorr message."""
                try:
                    rc, output = infra.shell("cat /proc/sys/net/ipv4/tcp_tw_reuse")
                    self.original_tcp_tw_reuse_value = output
                    rc, output = infra.shell(
                        "sudo -n bash -c 'echo 1 > " "/proc/sys/net/ipv4/tcp_tw_reuse'"
                    )
                    assert rc == 0, (
                        f"Failed to enable tcp_tw_resue, return code "
                        f"is not zero, but: {rc}"
                    )
                except Exception as e:
                    log.warn(
                        f"Failed step_save_and_enable_tcp_rw_reuse "
                        f"with the following error message: {e}"
                    )

        with allure_step_with_separate_logging("step_topology_precondition"):
            """Verify that within timeout, PCEP topology is present, with no PCC
            connected."""
            utils.wait_until_function_pass(
                PCEP_READY_VERIFY_TIMEOUT, 1, pcep.check_empty_pcep_topology
            )

        with allure_step_with_separate_logging("strep_topology_intercondition"):
            """Verify that within timeout, PCEP topology contains correct numbers of
            LSPs."""
            self.pcc_mock_process = pcep.start_pcc_mock(
                pcc=PCCS,
                lsp=LSPS,
                local_address=ODL_IP,
                remote_address=ODL_IP,
                log_file_name=LOG_NAME,
                verify_introduced_lsps=True,
                verify_timeout=PCEP_READY_VERIFY_TIMEOUT,
            )

        with allure_step_with_separate_logging("step_updater_1"):
            """Run updater tool to change hops, using 1 blocking http thread."""
            hop = self.get_next_hop()
            rc, stdout = pcep.run_updater(
                hop=hop,
                pcc=PCCS,
                lsp=LSPS,
                pccaddress=ODL_IP,
                odladdress=ODL_IP,
                workers=1,
                reuse=RESTCONF_REUSE,
                refresh=UPDATER_REFRESH,
                timeout=UPDATER_TIMEOUT,
            )
            pcep.check_updater_response(stdout, TOTAL_LSPS, False)

        with allure_step_with_separate_logging("step_verify_1"):
            """Verify that within timeout, the correct number of new hops is in PCEP
            topology."""
            utils.wait_until_function_returns_value(
                PCEP_READY_VERIFY_TIMEOUT,
                1,
                TOTAL_LSPS,
                pcep.get_pcep_topology_hop_count,
                hop,
            )

        with allure_step_with_separate_logging("step_updater_2"):
            """Run updater tool to change hops, using 1 blocking http thread."""
            hop = self.get_next_hop()
            rc, stdout = pcep.run_updater(
                hop=hop,
                pcc=PCCS,
                lsp=LSPS,
                pccaddress=ODL_IP,
                odladdress=ODL_IP,
                workers=2,
                reuse=RESTCONF_REUSE,
                refresh=UPDATER_REFRESH,
                timeout=UPDATER_TIMEOUT,
            )
            pcep.check_updater_response(stdout, TOTAL_LSPS, False)

        with allure_step_with_separate_logging("step_verify_2"):
            """Verify that within timeout, the correct number of new hops is in PCEP
            topology."""
            utils.wait_until_function_returns_value(
                PCEP_READY_VERIFY_TIMEOUT,
                1,
                TOTAL_LSPS,
                pcep.get_pcep_topology_hop_count,
                hop,
            )

        with allure_step_with_separate_logging("step_updater_3"):
            """Run updater tool to change hops, using 1 blocking http thread."""
            hop = self.get_next_hop()
            rc, stdout = pcep.run_updater(
                hop=hop,
                pcc=PCCS,
                lsp=LSPS,
                pccaddress=ODL_IP,
                odladdress=ODL_IP,
                workers=4,
                reuse=RESTCONF_REUSE,
                refresh=UPDATER_REFRESH,
                timeout=UPDATER_TIMEOUT,
            )
            pcep.check_updater_response(stdout, TOTAL_LSPS, False)

        with allure_step_with_separate_logging("step_verify_3"):
            """Verify that within timeout, the correct number of new hops is in PCEP
            topology."""
            utils.wait_until_function_returns_value(
                PCEP_READY_VERIFY_TIMEOUT,
                1,
                TOTAL_LSPS,
                pcep.get_pcep_topology_hop_count,
                hop,
            )

        with allure_step_with_separate_logging("step_updater_4"):
            """Run updater tool to change hops, using 1 blocking http thread."""
            hop = self.get_next_hop()
            rc, stdout = pcep.run_updater(
                hop=hop,
                pcc=PCCS,
                lsp=LSPS,
                pccaddress=ODL_IP,
                odladdress=ODL_IP,
                workers=8,
                reuse=RESTCONF_REUSE,
                refresh=UPDATER_REFRESH,
                timeout=UPDATER_TIMEOUT,
            )
            pcep.check_updater_response(stdout, TOTAL_LSPS, False)

        with allure_step_with_separate_logging("step_verify_4"):
            """Verify that within timeout, the correct number of new hops is in PCEP
            topology."""
            utils.wait_until_function_returns_value(
                PCEP_READY_VERIFY_TIMEOUT,
                1,
                TOTAL_LSPS,
                pcep.get_pcep_topology_hop_count,
                hop,
            )

        with allure_step_with_separate_logging("step_updater_5"):
            """Run updater tool to change hops, using 1 blocking http thread."""
            hop = self.get_next_hop()
            rc, stdout = pcep.run_updater(
                hop=hop,
                pcc=PCCS,
                lsp=LSPS,
                pccaddress=ODL_IP,
                odladdress=ODL_IP,
                workers=16,
                reuse=RESTCONF_REUSE,
                refresh=UPDATER_REFRESH,
                timeout=UPDATER_TIMEOUT,
            )
            pcep.check_updater_response(stdout, TOTAL_LSPS, False)

        with allure_step_with_separate_logging("step_verify_5"):
            """Verify that within timeout, the correct number of new hops is in PCEP
            topology."""
            utils.wait_until_function_returns_value(
                PCEP_READY_VERIFY_TIMEOUT,
                1,
                TOTAL_LSPS,
                pcep.get_pcep_topology_hop_count,
                hop,
            )

        with allure_step_with_separate_logging("step_updater_6"):
            """Run updater tool to change hops, using 1 blocking http thread."""
            hop = self.get_next_hop()
            rc, stdout = pcep.run_updater(
                hop=hop,
                pcc=PCCS,
                lsp=LSPS,
                pccaddress=ODL_IP,
                odladdress=ODL_IP,
                workers=32,
                reuse=RESTCONF_REUSE,
                refresh=UPDATER_REFRESH,
                timeout=UPDATER_TIMEOUT,
            )
            pcep.check_updater_response(stdout, TOTAL_LSPS, False)

        with allure_step_with_separate_logging("step_verify_6"):
            """Verify that within timeout, the correct number of new hops is in PCEP
            topology."""
            utils.wait_until_function_returns_value(
                PCEP_READY_VERIFY_TIMEOUT,
                1,
                TOTAL_LSPS,
                pcep.get_pcep_topology_hop_count,
                hop,
            )

        with allure_step_with_separate_logging("step_updater_7"):
            """Run updater tool to change hops, using 1 blocking http thread."""
            hop = self.get_next_hop()
            rc, stdout = pcep.run_updater(
                hop=hop,
                pcc=PCCS,
                lsp=LSPS,
                pccaddress=ODL_IP,
                odladdress=ODL_IP,
                workers=64,
                reuse=RESTCONF_REUSE,
                refresh=UPDATER_REFRESH,
                timeout=UPDATER_TIMEOUT,
            )
            pcep.check_updater_response(stdout, TOTAL_LSPS, False)

        with allure_step_with_separate_logging("step_verify_7"):
            """Verify that within timeout, the correct number of new hops is in PCEP
            topology."""
            utils.wait_until_function_returns_value(
                PCEP_READY_VERIFY_TIMEOUT,
                1,
                TOTAL_LSPS,
                pcep.get_pcep_topology_hop_count,
                hop,
            )

        with allure_step_with_separate_logging("step_updater_8"):
            """Run updater tool to change hops, using 1 blocking http thread."""
            hop = self.get_next_hop()
            rc, stdout = pcep.run_updater(
                hop=hop,
                pcc=PCCS,
                lsp=LSPS,
                pccaddress=ODL_IP,
                odladdress=ODL_IP,
                workers=128,
                reuse=RESTCONF_REUSE,
                refresh=UPDATER_REFRESH,
                timeout=UPDATER_TIMEOUT,
            )
            pcep.check_updater_response(stdout, TOTAL_LSPS, False)

        with allure_step_with_separate_logging("step_verify_8"):
            """Verify that within timeout, the correct number of new hops is in PCEP
            topology."""
            utils.wait_until_function_returns_value(
                PCEP_READY_VERIFY_TIMEOUT,
                1,
                TOTAL_LSPS,
                pcep.get_pcep_topology_hop_count,
                hop,
            )

        with allure_step_with_separate_logging("step_updater_9"):
            """Run updater tool to change hops, using 1 blocking http thread."""
            hop = self.get_next_hop()
            rc, stdout = pcep.run_updater(
                hop=hop,
                pcc=PCCS,
                lsp=LSPS,
                pccaddress=ODL_IP,
                odladdress=ODL_IP,
                workers=256,
                reuse=RESTCONF_REUSE,
                refresh=UPDATER_REFRESH,
                timeout=UPDATER_TIMEOUT,
            )
            pcep.check_updater_response(stdout, TOTAL_LSPS, False)

        with allure_step_with_separate_logging("step_verify_9"):
            """Verify that within timeout, the correct number of new hops is in PCEP
            topology."""
            utils.wait_until_function_returns_value(
                PCEP_READY_VERIFY_TIMEOUT,
                1,
                TOTAL_LSPS,
                pcep.get_pcep_topology_hop_count,
                hop,
            )

        with allure_step_with_separate_logging("step_updater_10"):
            """Run updater tool to change hops, using 1 blocking http thread."""
            hop = self.get_next_hop()
            rc, stdout = pcep.run_updater(
                hop=hop,
                pcc=PCCS,
                lsp=LSPS,
                pccaddress=ODL_IP,
                odladdress=ODL_IP,
                workers=512,
                reuse=RESTCONF_REUSE,
                refresh=UPDATER_REFRESH,
                timeout=UPDATER_TIMEOUT,
            )
            pcep.check_updater_response(stdout, TOTAL_LSPS, False)

        with allure_step_with_separate_logging("step_verify_10"):
            """Verify that within timeout, the correct number of new hops is in PCEP
            topology."""
            utils.wait_until_function_returns_value(
                PCEP_READY_VERIFY_TIMEOUT,
                1,
                TOTAL_LSPS,
                pcep.get_pcep_topology_hop_count,
                hop,
            )

        with allure_step_with_separate_logging("step_updater_with_delegate"):
            """Run updater tool to revoke the delegate for the given hop, using 1
            blocking http thread."""
            rc, stdout = pcep.run_updater(
                hop=hop,
                pcc=PCCS,
                lsp=LSPS,
                pccaddress=ODL_IP,
                odladdress=ODL_IP,
                workers=1,
                reuse=RESTCONF_REUSE,
                refresh=UPDATER_REFRESH,
                timeout=UPDATER_TIMEOUT,
                delegate=False,
                pccip="127.0.0.1",
                tunnel_no=2,
            )
            pcep.check_updater_response(stdout, TOTAL_LSPS, True)
            utils.wait_until_function_returns_value(
                PCEP_READY_VERIFY_TIMEOUT,
                1,
                TOTAL_LSPS,
                pcep.get_pcep_topology_hop_count,
                hop,
            )

        if ODL_IP == TOOLS_IP:
            with allure_step_with_separate_logging(
                "Skipped: step_PCEP_sessions_from_multiple_machines"
            ):
                log.warn(
                    "There is no point in running this step if only one machine is"
                    "used, ODL and TOOLs system is the same machine."
                )
        else:
            with allure_step_with_separate_logging(
                "step_PCEP_sessions_from_multiple_machines"
            ):
                """Start two instances of pcep mock simulator one on ODL host and one
                on TOOLS host. If both of these hosts, ODL and TOOLS are the same,
                then this step is skipped
                """
                infra.ssh_put_file(
                    local_file_path="build_tools/pcep-pcc-mock.jar",
                    remot_file_path="/tmp/pcep-pcc-mock.jar",
                    host=TOOLS_IP,
                    username=TOOLS_USER,
                    password=TOOLS_PASSWD,
                )
                self.ssh_handler = infra.ssh_start_command(
                    f"java -jar /tmp/pcep-pcc-mock.jar --local-address {TOOLS_IP} "
                    f"--remote-address {ODL_IP} --pcc {PCCS} --lsp {LSPS}",
                    host=TOOLS_IP,
                    username=TOOLS_USER,
                    password=TOOLS_PASSWD,
                )
                utils.wait_until_function_returns_value(
                    PCEP_READY_VERIFY_TIMEOUT,
                    1,
                    2 * TOTAL_LSPS,
                    pcep.get_pcep_topology_hop_count,
                    "1.1.1.1/32",
                )
                infra.ssh_put_file(
                    local_file_path="tools/pcep_updater/updater.py",
                    remot_file_path="/tmp/updater.py",
                    host=TOOLS_IP,
                    username=TOOLS_USER,
                    password=TOOLS_PASSWD,
                )
                infra.ssh_put_file(
                    local_file_path="libraries/AuthStandalone.py",
                    remot_file_path="/tmp/AuthStandalone.py",
                    host=TOOLS_IP,
                    username=TOOLS_USER,
                    password=TOOLS_PASSWD,
                )
                stdout, stderr = infra.ssh_run_command(
                    f"taskset 0x00000001 python3 /tmp/updater.py  "
                    f"--odladdress '{ODL_IP}' --pccaddress '{TOOLS_IP}' "
                    f"--user 'admin' --password 'admin' --hop '11.11.11.11/32' "
                    f"--pccs '{PCCS}' --lsps '{LSPS}' --workers '1' --pccip 'None' "
                    f"--refresh '{UPDATER_REFRESH}' --reuse 'False' --delegate 'True' "
                    f"--timeout '900' 2>&1",
                    host=TOOLS_IP,
                    username=TOOLS_USER,
                    password=TOOLS_PASSWD,
                )
                expected_log_message = f"Counter({{'pass': {TOTAL_LSPS}}})"
                assert expected_log_message in stdout, (
                    f"Expected message {expected_log_message=} was not found in "
                    f"{stdout=}"
                )
                utils.wait_until_function_returns_value(
                    UPDATER_TIMEOUT,
                    1,
                    2 * TOTAL_LSPS,
                    pcep.get_pcep_topology_hop_count,
                    "11.11.11.11/32",
                )
                infra.ssh_stop_command(self.ssh_handler)

        with allure_step_with_separate_logging("step_stop_pcc_mock"):
            """Send ctrl+c to pcc-mock, see prompt again within timeout."""
            pcep.stop_pcc_mock_process(self.pcc_mock_process)
            pcep.kill_all_pcc_mock_simulators(gracefully=False)
            utils.wait_until_function_pass(
                PCEP_READY_VERIFY_TIMEOUT, 5, pcep.check_empty_pcep_topology
            )

        with allure_step_with_separate_logging(
            "step_PCEP_sessions_flapped_with_LSP_updates"
        ):
            """Flapping PCEP sessions and perform LSP updates within flapping."""
            pccs = 10
            lsps = 15
            total_lsps = pccs * lsps
            updated_hop = "2.2.2.2/32"
            for _ in range(15):
                utils.wait_until_function_pass(
                    PCEP_READY_VERIFY_TIMEOUT, 5, pcep.check_empty_pcep_topology
                )
                self.pcc_mock_process = pcep.start_pcc_mock(
                    pcc=pccs,
                    lsp=lsps,
                    local_address=ODL_IP,
                    remote_address=ODL_IP,
                    verify_introduced_lsps=True,
                    verify_timeout=PCEP_READY_VERIFY_TIMEOUT,
                    log_file_name="serial_execution.log"
                )
                rc, stdout = pcep.run_updater(
                    hop=updated_hop,
                    pcc=pccs,
                    lsp=lsps,
                    pccaddress=ODL_IP,
                    odladdress=ODL_IP,
                    workers=1,
                    reuse=RESTCONF_REUSE,
                    refresh=UPDATER_REFRESH,
                    timeout=UPDATER_TIMEOUT,
                )
                pcep.check_updater_response(stdout, total_lsps, False)
                utils.wait_until_function_returns_value(
                    60, 5, total_lsps, pcep.get_pcep_topology_hop_count, updated_hop
                )
                pcep.stop_pcc_mock_process(self.pcc_mock_process)
            self.check_stability()

        with allure_step_with_separate_logging(
            "step_PCEP_sessions_flapped_alongside_LSP_updates"
        ):
            """Flapping PCEP sessions and perform LSP updates alongside flapping."""
            pccs = 10
            lsps = 15
            total_lsps = pccs * lsps
            updated_hop = "2.2.2.2/32"
            utils.wait_until_function_pass(
                PCEP_READY_VERIFY_TIMEOUT, 5, pcep.check_empty_pcep_topology
            )
            pcc_mock_script_process = pcep.start_pcc_mock_with_flapping(
                local_address=ODL_IP,
                remote_address=ODL_IP,
                pcc=pccs,
                lsp=lsps,
                log_file_name="throughpcep_parallel_Execution.log",
                interval=10,
            )
            for _ in range(10):
                hop = self.get_next_hop()
                # Check if pcep-pcc-mock.jar file is running
                rc, output = infra.shell(
                    "ps -fu $WHOAMI | grep 'pcep-pcc-mock.jar' | grep -v 'grep' | "
                    "awk '{print $2}'"
                )
                if output:
                    rc, stdout = pcep.run_updater(
                        hop=hop,
                        pcc=pccs,
                        lsp=lsps,
                        pccaddress=ODL_IP,
                        odladdress=ODL_IP,
                        workers=1,
                        reuse=RESTCONF_REUSE,
                        refresh=UPDATER_REFRESH,
                        timeout=UPDATER_TIMEOUT,
                    )
                    pcep.check_updater_response(stdout, total_lsps, True)
            infra.stop_process(pcc_mock_script_process)
            pcep.kill_all_pcc_mock_simulators(gracefully=False)
            self.check_stability()

        with allure_step_with_separate_logging("step_download_pccmock_log"):
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
                f"mv tmp/throughpcep_parallel_Execution.log "
                f"results/throughpcep_parallel_Execution.log",
                check_rc=True,
            )
        with allure_step_with_separate_logging("step_topology_postcondition"):
            "Verify that within timeout, PCEP topology contains no PCCs again."
            utils.wait_until_function_pass(
                PCEP_READY_VERIFY_TIMEOUT, 5, pcep.check_empty_pcep_topology
            )

        if not ENABLE_TCP_TW_REUSE:
            with allure_step_with_separate_logging(
                "Skipped: step_restore_tcp_tw_reuse"
            ):
                log.warn(
                    "There is no point in running this step if changing TCP TW REUSE is prohibited."
                )
        else:
            with allure_step_with_separate_logging("step_restore_tcp_tw_reuse"):
                """If requested, restore the old tcp_tw_reuse value.
                In case of failure only log the erorr message."""
                try:
                    rc, output = infra.shell(
                        f"sudo -n bash -c 'echo {self.original_tcp_tw_reuse_value} > "
                        f"/proc/sys/net/ipv4/tcp_tw_reuse'"
                    )
                    assert rc == 0, (
                        f"Failed to reset tcp_tw_resue, return code"
                        f"is not zero, but: {rc}"
                    )
                except Exception as e:
                    log.warn(
                        f"Failed step_restore_tcp_tw_reuse "
                        f"with the following error message: {e}"
                    )
