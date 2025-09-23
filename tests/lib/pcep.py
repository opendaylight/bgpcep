#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#


import gc
import logging
import os
import re
import subprocess

from lib import AuthStandalone
from lib import infra
from lib import templated_requests

ODL_IP = os.environ["ODL_IP"]
RESTCONF_PORT = os.environ["RESTCONF_PORT"]

log = logging.getLogger(__name__)


def check_empty_pcep_topology():
    templated_requests.get_templated_request(
        "data/templates/empty_pcep_topology", mapping=None, json=True, verify=True
    )

def get_pcep_topology():
    """Returns pcep topology from ODL using RESTCONF.

    Args:
        None

    Returns:
        json: Pcep topology
    """
    rest_session = AuthStandalone.Init_Session(ODL_IP, "admin", "admin")
    resp = AuthStandalone.Get_Using_Session(rest_session, "data/network-topology:network-topology/topology=pcep-topology?content=nonconfig")
    # Not Logging content, as it may be huge.
    #logging.warn(resp.text)
    assert resp.status_code == 200, f"Response code for get pcep topology does not meach expected 200, but is {resp.status_code}"
    log.debug(resp.text)
    return resp

def get_pcep_topology_hop_count(hop):
    """Returns number of occurences of concrete hop value in pcep topology.

    Args:
        hop (str): Hop value to be counted in response

    Returns:
        int: Number of hop value stored in pcep topology.
    """
    resp = get_pcep_topology()
    topology_count = len(re.findall(hop, resp.text))
    log.info(f"Current pcep topology lsps count for hop {hop} is: {topology_count}")
    gc.collect()

    return topology_count
    

def start_pcc_mock(
    pcc: int,
    lsp: int = 1,
    log_level: str | None = None,
    output_file_name: str = "pcep-pcc-mock_output.txt"
) -> subprocess.Popen:
    """Starts pcep-pcc-mock.jar tool as PCC device simulator.

    This test tool is used for simulating PCC enabled device. 
    TODO: ADD DOCUMENTATION

    Args:
        pcc (int): ****
        lsp (int): ****
        log_level (str): Level of logs used by bgp app peer tool.

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

    command_parts.append(f"&>tmp/{output_file_name}")

    process = infra.shell(" ".join(command_parts), run_in_background=True)

    return process

def start_pcc_mock_with_flapping(
    local_address: str,
    remote_address: str,
    pcc: int,
    lsp: int,
    log_file_name: str,
    interval: int
) -> subprocess.Popen:
    """ TODO: ADD DOCUMENTATION
    """
    start_mock_script_command = f"tools/mock.sh \
        build_tools/pcep-pcc-mock.jar \
        {local_address} \
        {remote_address} \
        {pcc} \
        {lsp} \
        {log_file_name} \
        {interval}"
    
    process = infra.shell(start_mock_script_command, run_in_background=True)

    return process

def kill_all_pcc_mock_simulators(gracefully: bool = True):
    signal_to_be_sent = "INT" if gracefully else "KILL"
    infra.shell(f"pkill --signal {signal_to_be_sent} -f pcep-pcc-mock")
    infra.shell(f"pkill --signal {signal_to_be_sent} mock.sh")

def start_update(
    hop: str,
    pcc: int,
    lsp: int,
    workers: int = 1,
    refresh: float | None = 0.1,
    reuse: bool | None = False,
    delegate: bool | None = True,
    pccip: str | None = None,
    tunnel_no: int | None = None,
    timeout: int | None = 900
) -> subprocess.Popen:
    """Starts updater.py script to update LSPs.

    This script is used for updating LSPs stored in ODL by sending RESTCONF requests to ODL.

    Args:
        ammount (int): Number of ipv4 prefixes to be sent to ODL.
        insert (int): Number of ipv4 prefixes to be added to ODL
            within one message.
        withdraw (int): Number of ipv4 prefixes to be removed to ODL
            within one message.
        prefill (int): Number of ipv4 prefixes to be prefilled in ODL
            before adding or removing other prefixes.
        update (int): Number of ipv4 prefixes to be updated in ODL
            within one message.
        multiplicity (int): Number BGP peers to be simulated
            (each with its own ip address).
        listen (bool): If BGP peering should be initiated by ODL or not.
            (Listen means bgp peer is in passive mode)

    Returns:
        subprocess.Popen: BGP speaker process handler.
    """
    command_parts = [
        f"python tools/updater.py \
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

    return rc, stdout

