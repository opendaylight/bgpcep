#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#


import logging
import subprocess

from libraries import infra
from libraries import templated_requests
from libraries.variables import variables

ODL_IP = variables.ODL_IP
RESTCONF_PORT = variables.RESTCONF_PORT
TOOLS_IP = variables.TOOLS_IP

log = logging.getLogger(__name__)


def check_empty_bmp_monitor():
    """Verifies example-bmp-monitor does not contain any data in ODL using RESTCONF.

    Args:
        None

    Returns:
        None
    """
    templated_requests.get_templated_request(
        "variables/bgpfunctional/bmp_basic/empty_structure", mapping=None, json=True, verify=True
    )

def check_bmp_monitor_filled():
    """Verifies example-bmp-monitor is filled with dummy data in ODL using RESTCONF.

    Args:
        None

    Returns:
        None
    """
    mapping = {"TOOL_IP": TOOLS_IP}
    templated_requests.get_templated_request(
        "variables/bgpfunctional/bmp_basic/filled_structure", mapping=mapping, json=True, verify=True
    )

def start_bmp_mock(
    routers_count: int = 1,
    peers_count: int = 1,
    log_level: str | None = None,
    log_file_name: str = "bmpmock.log"
) -> subprocess.Popen:
    """Starts bgp-bmp-mock.jar tool for simulating BMP enabled device.

    This test tool is used for reporting dummy routes and statistics.

    Args:
        routers_count (int): Number of reported routers
        peers_count (int): Number of reported peers
        log_level (str): Level of logs used by bgp bmp mock tool.
        log_file_name (str): Name of the file where to store logs.

    Returns:
        subprocess.Popen: bgp bmp mock simulator process handler.
    """
    command_parts = [
        f"java -jar build_tools/bgp-bmp-mock.jar \
        --routers_count  {routers_count } \
        --peers_count {peers_count} \
        --local_address {TOOLS_IP} \
        --remote_address {ODL_IP}:12345"
    ]

    if log_level:
        command_parts.append(f"--log-level {log_level.upper()}")

    command_parts.append(f"2>&1 | tee tmp/{log_file_name}")

    process = infra.shell(
        " ".join(command_parts), use_shell=True, run_in_background=True
    )

    return process

def stop_bmp_mock_process(process: subprocess.Popen):
    """Stop pbgp bmp mock process by sending SIGINT signal.

    Args:
        process (subprocess.Popen): BGP speaker process handler.

    Returns:
        None
    """

    pid = infra.get_children_processes_pids(process, "java")[0]
    log.info(f"Killing bgp bmp mock process with PID {pid}")
    infra.stop_process_by_pid(pid, gracefully=True)
