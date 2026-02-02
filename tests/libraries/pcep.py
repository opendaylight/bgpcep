#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#


import gc
import ipaddress
import logging
import re
import subprocess
from typing import List

import requests

from libraries import AuthStandalone
from libraries import infra
from libraries import templated_requests
from libraries import utils
from libraries.variables import variables
from variables.pcepuser.titanium import variables as pcep_variables

ODL_IP = variables.ODL_IP
RESTCONF_PORT = variables.RESTCONF_PORT
TOOLS_IP = variables.TOOLS_IP
MAX_HTTP_RESPONSE_BODY_LOG_SIZE = variables.MAX_HTTP_RESPONSE_BODY_LOG_SIZE
VARIABLES = pcep_variables.get_variables(TOOLS_IP)

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


def get_pcep_topology() -> requests.Response:
    """Returns pcep topology from ODL using RESTCONF.

    Args:
        None

    Returns:
        requests.Response:: Pcep topology
    """
    rest_session = AuthStandalone.Init_Session(ODL_IP, "admin", "admin")
    resp = AuthStandalone.Get_Using_Session(
        rest_session,
        (
            "data/network-topology:network-topology/"
            "topology=pcep-topology"
            "?content=nonconfig"
        ),
    )
    assert resp.status_code == 200, (
        f"Response code for get pcep topology does not meach expected 200, "
        f"but is {resp.status_code}"
    )
    resposne_text = utils.truncate_long_text(resp.text, MAX_HTTP_RESPONSE_BODY_LOG_SIZE)
    log.debug(f"Response: {resposne_text}")
    log.info(f"Response code: {resp.status_code}")
    log.debug(f"Response headers: {resp.headers}")
    return resp


def get_path_computation_client() -> requests.Response:
    """Returns PCCs path computation client.

    Args:
        None

    Returns:
        requests.Response:: Path computation client.
    """
    uri = (
        f"rests/data/network-topology:network-topology/topology=pcep-topology/"
        f"node=pcc:%2F%2F{TOOLS_IP}/"
        f"network-topology-pcep:path-computation-client?content=nonconfig"
    )
    response = templated_requests.get_from_uri(uri)

    return response


def get_pcep_topology_hop_count(hop) -> int:
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


def configure_speaker_entitiy_identifier() -> requests.Response:
    """Setup spekare entity identifier.

    Args:
        None

    Returns:
        requests.Response: PUT request response.
    """
    mapping = {"IP": ODL_IP}
    resposne = templated_requests.put_templated_request(
        "variables/pcepuser/titanium/node_speaker_entity_identifier",
        mapping,
        json=False,
    )

    return resposne


def get_statistics(
    pcc_ip: str | None = None, verify_response: bool = False
) -> requests.Response:
    """Returns nodes PCEP statistics.

    Args:
        pcc_ip (str | None): PCC node IP address. If not specified,
            gets PCEP statistics for all pcc nodes.
        verify_response (bool): Verify response data using template.

    Returns:
        requests.Response: PCEP node statistics.
    """
    if pcc_ip is None:
        response = templated_requests.post_templated_request(
            "variables/pcepuser/titanium/get_stats_all",
            None,
            json=True,
            verify=False,
        )
    else:
        mapping = {"PCC_IP": pcc_ip}
        response = templated_requests.post_templated_request(
            "variables/pcepuser/titanium/get_pcc_stats",
            mapping,
            json=True,
            verify=verify_response,
        )

    return response


def verify_odl_does_not_return_statistics_for_pcc(pcc_ip: str):
    """Verify ODL does not return statistics using get-stats RPC.

    Args:
        pcc_ip (str): PCC node IP address.

    Returns:
        None
    """
    response = get_statistics(pcc_ip=pcc_ip)
    assert "pcep-session-state" not in response.text, (
        f'Did not expect "pcep-session-state" to be returned in "get-stats" RPC \n '
        f"Response:{response.text}"
    )


def verify_global_pcep_statistics(
    expected_pcc_count: int, expected_lsps_per_pcc_count: int | None = None
):
    """Verify total number of reported PCC nodes and their statistics infromation.

    To fetch the current PCEP statistics get-stats RPC is used.

    Args:
        expected_pcc_count (int): Expected number of reported PCC nodes.
        expected_lsps_per_pcc_count (int | None): Expected number of LSPs reported
            by each PCC node. If it is set to None, do not check this value.

    Returns:
        None
    """
    response = get_statistics()
    response_json = response.json()
    topology = response_json["pcep-topology-stats-rpc:output"]["topology"][0]
    nodes = topology.get("node", ())
    for node in nodes:
        assert (
            "pcep-session-state" in node
        ), f"Found PCC node without PCEP statistics: {node}."
        if expected_lsps_per_pcc_count is not None:
            reported_lsps_count = node["pcep-session-state"]["delegated-lsps-count"]
            assert reported_lsps_count == expected_lsps_per_pcc_count, (
                f"Number of expected LSP/s count {expected_lsps_per_pcc_count} "
                f"does not match number of reported LSP/s {reported_lsps_count}."
            )
    nodes_count = len(nodes)
    assert expected_pcc_count == nodes_count, (
        f'Number of returned {nodes_count} node/s in "get-stats" RPC '
        f"does not match expected {expected_pcc_count} node/s."
    )


def verify_statsistics_contains_pcc_mock_data(
    expected_pcc_count: int, expected_lsps_per_pcc_count: int, first_pcc_ip: str
):
    """Verify all PCCs and LSPs introduced by pcc mock sim are shown in PCEP stats.

    To fetch the current PCEP statistics get-stats RPC is used. Only PCC nodes
    expected to belong to the simulator are verifyed, based on ip address, ignoring
    the rest. Pcc mock simulator assignes ip addresses to nodes in sequence.


    Args:
        expected_pcc_count (int): Expected number of reported PCC nodes.
        expected_lsps_per_pcc_count (int): Expected number of reported LSPs
            for each PCC node.
        first_pcc_ip (str): IP address of the first PCC node in the sequence.

    Returns:
        None
    """
    first_ip = ipaddress.IPv4Address(first_pcc_ip)
    last_ip = first_ip + expected_pcc_count
    response = get_statistics()
    response_json = response.json()
    topology = response_json["pcep-topology-stats-rpc:output"]["topology"][0]
    nodes = topology.get("node", ())
    found_notes = 0
    for node in nodes:
        node_ip = ipaddress.IPv4Address(node["node-id"].removeprefix("pcc://"))
        if node_ip < first_ip or node_ip >= last_ip:
            continue
        found_notes += 1
        assert (
            "pcep-session-state" in node
        ), f"Found PCC node without PCEP statistics: {node}."
        reported_lsps_count = node["pcep-session-state"]["delegated-lsps-count"]
        assert reported_lsps_count == expected_lsps_per_pcc_count, (
            f"Number of expected LSP/s count {expected_lsps_per_pcc_count} "
            f"does not match number of reported LSP/s {reported_lsps_count}."
        )
    assert expected_pcc_count == found_notes, (
        f'Number of returned {found_notes} node/s in "get-stats" RPC '
        f"does not match expected {expected_pcc_count} node/s."
    )


def get_stat_timer_value(
    expected_response_code: int | List[int] | None = 200,
) -> requests.Response:
    """Get the PCEP statistics timer value.

    This value determines the interval in which the statistics are updated.

    Args:
        expected_response_code (int | List[int] | None): Expected HTTP response code
            returned from ODL. Can be either single value or list of possible values.

    Returns:
        requests.Response: PCEP node statistics.
    """
    response = templated_requests.get_templated_request(
        "variables/pcepuser/titanium/get_timer_value",
        None,
        json=True,
        expected_code=expected_response_code,
    )

    return response


def set_stat_timer_value(
    timer_value: int, expected_response_code: int | List[int] | None = (201, 204)
) -> requests.Response:
    """Set the PCEP statistics timer value.

    This value determines the interval in which the statistics are updated.

    Args:
        timer_value (str): PCEP statistics timer value.
        expected_response_code (int | List[int] | None): Expected HTTP response code
            returned from ODL. Can be either single value or list of possible values.

    Returns:
        requests.Response: PCEP node statistics.
    """
    mapping = {"TIMER": timer_value}
    response = templated_requests.put_templated_request(
        "variables/pcepuser/titanium/set_timer_value",
        mapping,
        json=True,
        expected_code=expected_response_code,
    )

    return response


def add_lsp(xml_data: str) -> requests.Response:
    """Invoke add-lsp operation on ODL.

    Args:
        xml_data (str): Lsp data payload.

    Returns:
        requests.Response: POST request response.
    """
    response = operate_xml_lsp_return_json("network-topology-pcep:add-lsp", xml_data)
    return response


def update_lsp(xml_data: str) -> requests.Response:
    """Invoke update-lsp operation on ODL.

    Args:
        xml_data (str): Lsp data payload.

    Returns:
        requests.Response: POST request response.
    """
    response = operate_xml_lsp_return_json("network-topology-pcep:update-lsp", xml_data)
    return response


def remove_lsp(xml_data: str) -> requests.Response:
    """Invoke remove-lsp operation on ODL.

    Args:
        xml_data (str): Lsp data payload.

    Returns:
        requests.Response: POST request response.
    """
    response = operate_xml_lsp_return_json("network-topology-pcep:remove-lsp", xml_data)
    return response


def operate_xml_lsp_return_json(uri_part: str, xml_data: str) -> requests.Response:
    """Invoke specific lsp operation on ODL.

    Args:
        uri_part (str): Full operation name.
        xml_data (str): Lsp data payload.

    Returns:
        requests.Response: POST request response.
    """
    uri_path = f"/rests/operations/{uri_part}"
    response = templated_requests.post_to_uri(
        uri_path, None, xml_data, expected_code=templated_requests.ALLOWED_STATUS_CODES
    )

    return response


def wait_until_concrete_number_of_lsps_reported(
    total_lsp_count: int, interval: int, timeout: int = 90
):
    """Repeatedly check PCEP topology until concrete number of LSPs is shown.

    Args:
        total_lsp_count (int): Expected total number of LSPs to be reported.
        interval (int): Number of seconds to elapse between each verification retry.
        timeout (int): Number of seconds to time out verification.

    Returns:
        None
    """
    utils.wait_until_function_returns_value(
        int(timeout / interval),
        interval,
        total_lsp_count,
        get_pcep_topology_hop_count,
        "1.1.1.1/32",
    )


def start_pcc_mock(
    pcc: int,
    lsp: int = 1,
    local_address: str = "127.0.1.0",
    remote_address: str = "127.0.0.1",
    log_level: str | None = None,
    log_file_name: str = "pcep-pcc-mock_output.txt",
    verify_introduced_lsps=True,
    verify_timeout: int = 90,
    verify_interval: int = 1,
) -> subprocess.Popen:
    """Starts pcep-pcc-mock.jar tool for simulating PCC device.

    This test tool is used for reporting and processing LSPs using PCC protocol.

    Args:
        pcc (int): Number of simulated pcc devices.
        lsp (int): Number of reported LSPs by each device.
        local_address (str): PCC ip address.
        remote_address (str): PCE ip address.
        log_level (str): Level of logs used by pcep pcc mock tool.
        log_file_name (str): Name of the file where to store tool output.
        verify_introduced_lsps (bool): Flag to indicate if presence of LSPs
            automatiically reported by pcep pcc mock devices should be verified in ODL.
        verify_timeout (int): Number of seconds to time out verification.
        verify_interval (int): Number of seconds to elapse between each verification
            retry.

    Returns:
        subprocess.Popen: PCEP device simulator process handler.
    """
    command_parts = [
        f"java -jar build_tools/pcep-pcc-mock.jar \
        --pcc {pcc} \
        --lsp {lsp} \
        --local-address {local_address} \
        --remote-address {remote_address}"
    ]

    if log_level:
        command_parts.append(f"--log-level {log_level.upper()}")

    command_parts.append(f"2>1 >tmp/{log_file_name}")

    process = infra.shell(
        " ".join(command_parts), use_shell=True, run_in_background=True
    )

    if verify_introduced_lsps:
        wait_until_concrete_number_of_lsps_reported(
            pcc * lsp, verify_interval, verify_timeout
        )

    return process


def stop_pcc_mock_process(process: subprocess.Popen, timeout: int = 5):
    """Stop pcep pcc mock process by sending SIGINT signal.

    Args:
        process (subprocess.Popen): Pcc mock process handler.
        timeout (int): Timeout in seconds.

    Returns:
        None
    """
    output = process.stdout
    log.debug(f"Pcc mock process output: {output=}")
    log.info(f"Killing pcc mock process with PID {process.pid}")
    infra.stop_process_by_pid(process.pid, gracefully=True, timeout=timeout)


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
        interval (int): Time interval in seconds between starting and stopping
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
    odladdress: str = "127.0.0.1",
    pccaddress: str = "127.0.1.0",
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
        odladdress (str): ODL controller ip address.
        pccaddress (str): PCC device ip address.
        workers (int): Number of worker threads used for updating LSPs.
        refresh (float): Refresh value
        reuse (bool): Create only single session to send all update requests,
            otherwise create seperate connections.
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
            --odladdress '{odladdress}' \
            --pccaddress '{pccaddress}' \
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
        command_parts.append(f"--delegate '{"true" if delegate else "false"}'")
    if timeout:
        command_parts.append(f"--timeout '{timeout}'")
    if tunnel_no:
        command_parts.append(f"--tunnelnumber  '{tunnel_no}'")
    command_parts.append("2>&1")

    rc, stdout = infra.shell(" ".join(command_parts))
    assert rc == 0, f"Updater scipt returned non zero return code {rc=} {stdout=}"

    return rc, stdout


def check_updater_response(stdout: str, lsps: int, parallel: bool):
    """Verifies if all updates were executed successfully by observig the log
    output.

    Args:
        stdout (str): Updater log entries.
        lsps (int): Number of updated LSPs.
        parallel (bool): Flag indicating if the update process was executed in parallel

    Returns:
        None
    """
    if parallel:
        not_expected_log_message = f"Counter({{'pass': {0}}})"
        assert not_expected_log_message not in stdout, (
            f"Did not expect message {not_expected_log_message=}, "
            f"but was found in {stdout=}"
        )
    else:
        expected_log_message = f"Counter({{'pass': {lsps}}})"
        assert (
            expected_log_message in stdout
        ), f"Expected message {expected_log_message=} was not found in {stdout=}"
