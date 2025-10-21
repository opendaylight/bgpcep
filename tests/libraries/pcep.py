#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#


import gc
import json
import logging
import os
import re
import subprocess

from libraries import AuthStandalone
from libraries import infra
from libraries import templated_requests
from libraries import utils
from libraries.variables import variables

ODL_IP = variables.ODL_IP
RESTCONF_PORT = variables.RESTCONF_PORT
TOOLS_IP = variables.TOOLS_IP

log = logging.getLogger(__name__)


def check_empty_pcep_topology():
    """Verifies pcep tology does not have any node in ODL using RESTCONF.

    Args:
        None

    Returns:
        None
    """
    templated_requests.get_templated_request(
        "variables/bgpuser/empty_pcep_topology", mapping=None, json=True, verify=True
    )


def get_pcep_topology() -> json:
    """Returns pcep topology from ODL using RESTCONF.

    Args:
        None

    Returns:
        json: Pcep topology
    """
    rest_session = AuthStandalone.Init_Session(ODL_IP, "admin", "admin")
    # Not Logging content, as it may be huge.
    resp = AuthStandalone.Get_Using_Session(
        rest_session,
        "data/network-topology:network-topology/topology=pcep-topology?content=nonconfig",
    )
    assert (
        resp.status_code == 200
    ), f"Response code for get pcep topology does not meach expected 200, but is {resp.status_code}"
    log.debug(resp.text)
    return resp

def get_path_computation_client():
    uri = f"rests/data/network-topology:network-topology/topology=pcep-topology/node=pcc:%2F%2F{TOOLS_IP}/network-topology-pcep:path-computation-client?content=nonconfig"
    response = templated_requests.get_request(
        f"http://{ODL_IP}:{RESTCONF_PORT}/{uri}"
    )

    return response


def get_pcep_topology_hop_count(hop):
    """Returns number of hop value occurences in pcep topology.

    Args:
        hop (str): Hop value to be counted in response

    Returns:
        int: Number of hop values found in pcep topology.
    """
    resp = get_pcep_topology()
    topology_count = len(re.findall(hop, resp.text))
    log.info(f"Current pcep topology lsps count for hop {hop} is: {topology_count}")
    gc.collect()

    return topology_count

def configure_speaker_entitiy_identifier():
    """???.

    Args:
        None

    Returns:
        int: Number of hop values found in pcep topology.
    """
    mapping = {"IP": ODL_IP}
    resposne = templated_requests.put_templated_request(
        "data/templates/node_speaker_entity", mapping, json=False
    )
    
    return resposne

def add_lsp(pcc_ip: str, lsp_name: str):
    mapping = {"IP": pcc_ip, "NAME": lsp_name}
    resposne = templated_requests.post_templated_request(
        "data/templates/pcep_add_lsp", mapping, json=False
    )
    
    return resposne

def update_lsp(pcc_ip: str, lsp_name: str):
    mapping = {"IP": pcc_ip, "NAME": lsp_name}
    resposne = templated_requests.post_templated_request(
        "data/templates/pcep_update_lsp", mapping, json=False
    )
    
    return resposne

def remove_lsp(pcc_ip: str, lsp_name: str):
    mapping = {"IP": pcc_ip, "NAME": lsp_name}
    resposne = templated_requests.post_templated_request(
        "data/templates/pcep_remove_lsp", mapping, json=False, accept="*/*"
    )
    
    return resposne


def start_pcc_mock(
    pcc: int,
    lsp: int = 1,
    log_level: str | None = None,
    log_file_name: str = "pcep-pcc-mock_output.txt",
    verify_introduced_lsps=True,
    verify_timeout: int = 900,
) -> subprocess.Popen:
    """Starts pcep-pcc-mock.jar tool for simulating PCC device.

    This test tool is used for reporting and processing LSPs using PCC protocol.

    Args:
        pcc (int): Number of simulated pcc devices
        lsp (int): Number of reported LSPs by each device
        log_level (str): Level of logs used by pcep pcc mock tool.
        log_file_name (str): Name of the file where to store tool output.
        verify_introduced_lsps (bool): Flag to indicate if presence of LSPs
            automatiically reported by pcep pcc mock devices should be verified in ODL.
        verify_timeout (int): Number of seconds to time out verification

    Returns:
        subprocess.Popen: PCEP device simulator process handler.
    """
    command_parts = [
        f"java -jar build_tools/pcep-pcc-mock.jar \
        --pcc {pcc} \
        --lsp {lsp} \
        --local-address 127.0.0.1 \
        --remote-address 127.0.0.1"
    ]

    if log_level:
        command_parts.append(f"--log-level {log_level.upper()}")

    command_parts.append(f"&>tmp/{log_file_name}")

    process = infra.shell(
        " ".join(command_parts), use_shell=False, run_in_background=True
    )

    if verify_introduced_lsps:
        utils.wait_until_function_returns_value(
            verify_timeout,
            1,
            pcc * lsp,
            get_pcep_topology_hop_count,
            "1.1.1.1/32",
        )

    return process


def stop_pcc_mock_process(process: subprocess.Popen, timeout=3):
    """Stop pcep pcc mock process by sending SIGINT signal.

    Args:
        process (subprocess.Popen): Pcc mock process handler.

    Returns:
        None
    """
    output = process.stdout
    log.debug(f"Pcc mock process output: {output=}")
    pid = infra.get_children_processes_pids(process, "java")[0]
    log.info(f"Killing pcc mock process with PID {pid}")
    infra.stop_process_by_pid(pid, gracefully=True)


def start_pcc_mock_with_flapping(
    local_address: str,
    remote_address: str,
    pcc: int,
    lsp: int,
    log_file_name: str,
    interval: int,
) -> subprocess.Popen:
    """Starts pcep-pcc-mock.jar tool with some regular flapping.

    It uses mock.sh script which repeatedly starts and stops pcep pcc mock tool
    after some delay.

    Args:
        local_address (str): ip address bound to the pcep pcc mock too;
        remote_address (str): PCE endpoint ip address (ODL ip address)
        pcc (int): Number of simulated pcc devices
        lsp (int): Number of reported LSPs by each device
        log_file_name (str): Name of the file where to store tool output.
        interval (bool): Time interval in seconds between starting and stopping
            test tool

    Returns:
        subprocess.Popen: mock.sh process handler.
    """
    start_mock_script_command = f"tools/pcep_updater/mock.sh \
        build_tools/pcep-pcc-mock.jar \
        {local_address} \
        {remote_address} \
        {pcc} \
        {lsp} \
        {log_file_name} \
        {interval}"

    process = infra.shell(
        start_mock_script_command, use_shell=True, run_in_background=True
    )

    return process


def kill_all_pcc_mock_simulators(gracefully: bool = True):
    """Stop all pcep pcc mock process.

    It stops not only pcep pcc mock simulator by sending proper signal, but also
    the mock.sh script which is responsible for starting pcep pcc mock tool.

    Args:
        gracefully (bool): Determines which signal should be sent,
            for gracefully it sends SIGINT, otherwise SIGKILL

    Returns:
        None
    """
    signal_to_be_sent = "INT" if gracefully else "KILL"
    infra.shell(f"pkill --signal {signal_to_be_sent} -f pcep-pcc-mock")
    infra.shell(f"pkill --signal {signal_to_be_sent} mock.sh")


def run_updater(
    hop: str,
    pcc: int,
    lsp: int,
    workers: int = 1,
    refresh: float | None = 0.1,
    reuse: bool | None = False,
    delegate: bool | None = True,
    pccip: str | None = None,
    tunnel_no: int | None = None,
    timeout: int | None = 900,
) -> subprocess.Popen:
    """Starts updater.py script to update LSPs.

    This script is used for updating LSPs stored in ODL by sending RESTCONF
    requests to ODL. It uses taskset to controll process affinity to be steadily bound
    only to a single core.

    Args:
        hop (str): Updated hop value.
        pcc (int): Number of PCC devices.
        lsp (int): Number of LSPs reported by PCC devices.
        workers (int): Number of worker threads used for updating LSPs.
        refresh (float): Refresh value
        reuse (bool): Create only single session to send all update requests,
            otherwise create seperate connections
        listen (bool): If BGP peering should be initiated by ODL or not.
            (Listen means bgp peer is in passive mode)
        delegate (bool): Updated delegate value.
        pccip (str): Ip address of pcep pcc mock simulator
        tunnel_no (int): Number of tunnel which should be updated
        timeout (int): Timeout in seconds

    Returns:
        subprocess.Popen: Updater process handler.
    """
    command_parts = [
        f"taskset 0x00000001 \
            python tools/pcep_updater/updater.py \
            --odladdress '127.0.0.1' \
            --pccaddress '127.0.0.1' \
            --user 'admin' \
            --password 'admin' \
            --hop '{hop}' \
            --pccs '{pcc}' \
            --lsps '{lsp}' \
            --workers '{workers}'"
    ]

    command_parts.append(f"--pccip '{str(pccip)}'")
    if refresh is not None:
        command_parts.append(f"--refresh '{refresh}'")
    if reuse is not None:
        command_parts.append(f"--reuse '{reuse}'")
    if delegate is not None:
        command_parts.append(f"--delegate '{delegate}'")
    if timeout:
        command_parts.append(f"--timeout '{timeout}'")
    if tunnel_no:
        command_parts.append(f"--tunnelnumber  '{tunnel_no}'")
    command_parts.append("2>&1")

    rc, stdout = infra.shell(" ".join(command_parts))
    assert rc == 0, f"Updater scipt returned non zero return code {rc=} {stdout=}"

    return rc, stdout


def check_updater_response(stdout: str, lsps: int, parallel: bool):
    """Verifies if all updates were executed successfully by observig log output.

    Args:
        stdout (str): Updater log entries.
        lsps (int): Number of updated LSPs.
        parallel (bool): Flag indicating if the update process was executed in parallel

    Returns:
        None
    """
    if parallel:
        not_expected_log_message = f"Counter({{'pass': {0}}})"
        assert (
            not_expected_log_message not in stdout
        ), f"Did not expect message {not_expected_log_message=}, but was found in {stdout=}"
    else:
        expected_log_message = f"Counter({{'pass': {lsps}}})"
        assert (
            expected_log_message in stdout
        ), f"Expected message {expected_log_message=} was not found in {stdout=}"
