#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import ipaddr
import requests
import subprocess
import os
import logging

from lib.BgpRpcClient import BgpRpcClient
from lib import infra
from lib import templated_requests
from lib import utils

ODL_IP = os.environ["ODL_IP"]
TOOLS_IP = os.environ["TOOLS_IP"]
RESTCONF_PORT = os.environ["RESTCONF_PORT"]
BGP_RPC_CLIENT = BgpRpcClient(TOOLS_IP)

log = logging.getLogger(__name__)


def set_bgp_neighbour(ip: str, passive_mode: bool = True) -> requests.Response:
    """Sets BGP neighbour in ODL using RESTCONF.

    Args:
        ip (str): Ip address of BGP neighbour.
        passive_mode (bool): If set to true BGP session
            should be initiated by BGP neighbour,
            otherwise it should be initiated by ODL.

    Returns:
        requests.Response: Requests library response as returned for PUT call.
    """
    passive_mode_mapping = "true" if passive_mode else "false"
    mapping = {
        "IP": ip,
        "HOLDTIME": 180,
        "PEER_PORT": 17900,
        "PASSIVE_MODE": passive_mode_mapping,
        "BGP_RIB_OPENCONFIG": "example-bgp-rib",
    }
    resposne = templated_requests.put_templated_request(
        "data/templates/bgp_peer", mapping, json=False
    )

    return resposne


def set_bgp_neighbours(first_neigbout_ip: str, count: int, passive_mode: bool = True):
    """Sets multiple BGP neighbours in ODL using RESTCONF.

    Ip addresses are assigned to BGP neigbours sequentialy
    (Eg. 127.0.0.1, 127.0.0.2, ...)
    Each neighbour share the same settings, except for ip address.

    Args:
        first_neigbout_ip (str): First ip address from sequention
            used for first neigbour settings.
        count (int): Number of BGP neighours to be set in ODL.
        passive_mode (bool): If set to true BGP session
            should be initiated by BGP neighbour,
            otherwise it should be initiated by ODL.

    Returns:
        None
    """
    passive_mode_mapping = "true" if passive_mode else "false"
    for i in range(count):
        ip_address = str(ipaddr.IPAddress(first_neigbout_ip) + i)
        mapping = {
            "IP": ip_address,
            "HOLDTIME": 180,
            "PEER_PORT": 17900,
            "PASSIVE_MODE": passive_mode_mapping,
            "BGP_RIB_OPENCONFIG": "example-bgp-rib",
        }
        templated_requests.put_templated_request(
            "data/templates/bgp_peer", mapping, json=False
        )


def set_bgp_peer_group_members(
    peer_group_name: str, first_neigbout_ip: str, count: int
):
    """Sets multiple BGP neighbours belonging to the same peer-group.

    Each neighbour with sequential ip address. (Eg. 127.0.0.1, 127.0.0.2, ...)

    Args:
        peer_group_name: Name of peer group to which BGP neighbours should be
            assigned.
        first_neigbout_ip (str): First ip address from sequention used for
            first neigbout settings.
        count (int): Number of BGP neighours to be set in ODL.

    Returns:
        None
    """
    for i in range(count):
        ip_address = str(ipaddr.IPAddress(first_neigbout_ip) + i)
        mapping = {
            "IP": ip_address,
            "PEER_GROUP_NAME": peer_group_name,
            "BGP_RIB_OPENCONFIG": "example-bgp-rib",
        }
        templated_requests.put_templated_request(
            "data/templates/bgp_peer_group_member", mapping, json=False
        )


def set_bgp_application_peer(ip: str) -> requests.Response:
    """Sets BGP neighbour in peer group.

    Args:
        ip (str): Ip address of BGP neighbour.
        passive_mode (bool): If set to true BGP session
            should be initiated by BGP neighbour,
            otherwise it should be initiated by ODL.

    Returns:
        requests.Response: Requests library response as returned for PUT call.
    """
    mapping = {"IP": ip, "BGP_RIB_OPENCONFIG": "example-bgp-rib"}
    resposne = templated_requests.put_templated_request(
        "data/templates/bgp_application_peer", mapping, json=False
    )

    return resposne


def set_bgp_peer_group(
    peer_group_name: str, passive_mode: bool = True, rr_client: bool = False
):
    """Sets BGP peer group in ODL using RESTCONF.

    Args:
        peer_group_name (str): Name of the peer group.
        passive_mode (bool): If set to true BGP session
            should be initiated by BGP neighbour,
            otherwise it should be initiated by ODL.
        rr_client(bool): If route reflector should be used.

    Returns:
        requests.Response: Requests library response as returned for PUT call.
    """
    passive_mode_mapping = "true" if passive_mode else "false"
    rr_client_mapping = "true" if rr_client else "false"
    mapping = {
        "PEER_GROUP_NAME": peer_group_name,
        "PASSIVE_MODE": passive_mode_mapping,
        "HOLDTIME": 180,
        "RR_CLIENT": rr_client_mapping,
        "BGP_RIB_OPENCONFIG": "example-bgp-rib",
    }
    response = templated_requests.put_templated_request(
        "data/templates/bgp_peer_group", mapping, json=False
    )
    templated_requests.get_templated_request(
        "data/templates/bgp_verify_peer_group", mapping, json=True, verify=True
    )

    return response


def delete_bgp_application_peer(ip: str) -> requests.Response:
    """Removes BGP neighbour from peer group.

    Args:
        ip (str): Neighbour ip address.

    Returns:
        requests.Response: Requests library response
            as returned for DELETE call.
    """
    mapping = {"IP": ip, "BGP_RIB_OPENCONFIG": "example-bgp-rib"}
    resposne = templated_requests.delete_templated_request(
        "data/templates/bgp_application_peer", mapping
    )

    return resposne


def delete_bgp_neighbour(ip: str) -> requests.Response:
    """Removes BGP neighbour from ODL using RESTCONF.

    Args:
        ip (str): Neighbour ip address.

    Returns:
        requests.Response: Requests library response
            as returned for DELETE call.
    """
    mapping = {"IP": ip, "BGP_RIB_OPENCONFIG": "example-bgp-rib"}
    resposne = templated_requests.delete_templated_request(
        "data/templates/bgp_peer", mapping
    )

    return resposne


def delete_bgp_neighbours(first_neigbout_ip: str, count: int):
    """Delete multiple BGP neighbours from ODL using RESTCONF.

    BGP neighbours pp addresses are expected to be sequentialy
    (Eg. 127.0.0.1, 127.0.0.2, ...)

    Args:
        first_neigbout_ip (str): First ip address from sequention
            belonging to the first BGP neighbour.
        count (int): Number of BGP neighours to be removed from ODL.

    Returns:
        None
    """
    for i in range(count):
        ip_address = str(ipaddr.IPAddress(first_neigbout_ip) + i)
        delete_bgp_neighbour(ip_address)


def delete_bgp_peer_group(peer_group_name: str):
    """Deletes BGP peer group from ODL using RESTCONF.

    Args:
        peer_group_name (str): Name of the peer group.

    Returns:
        requests.Response: Requests library response as returned for PUT call.
    """
    mapping = {
        "PEER_GROUP_NAME": peer_group_name,
        "BGP_RIB_OPENCONFIG": "example-bgp-rib",
    }
    resposne = templated_requests.delete_templated_request(
        "data/templates/bgp_peer_group", mapping
    )

    return resposne


def start_bgp_speaker(
    ammount: int,
    insert: int | None = None,
    withdraw: int | None = None,
    prefill: int | None = None,
    update: int | None = None,
    multiplicity: int | None = None,
    my_ip: str = "127.0.0.1",
    my_port: int = 17900,
    peer_ip: str = "127.0.0.1",
    peer_port: int = 1790,
    listen: bool | None = None,
    bgpls: bool | None = None,
    skipAttr: bool | None = None,
    allf: bool | None = None,
    evpn: bool | None = None,
    wfr: int | None = None,
    log_level: str | None = None,
) -> subprocess.Popen:
    """???Starts play.py script as BGP speaker.

    This script is used for simulating BGP neighbour. It allows to send
    and receive ipv4 prefixes to/from ODL.

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
        bgpls (bool): If set use LS-NLRI.
        skipAttr (bool): Skip well known attributes for update message
        log_level (str): Level of logs used by bgp app peer tool.

    Returns:
        subprocess.Popen: BGP speaker process handler.
    """
    command_parts = [
        f"python tools/play.py --amount {ammount}\
            --myip={my_ip} \
            --myport={my_port} \
            --peerip={peer_ip} \
            --peerport={peer_port}"
    ]

    if insert is not None:
        command_parts.append(f"--insert={insert}")
    if withdraw is not None:
        command_parts.append(f"--withdraw={withdraw}")
    if prefill is not None:
        command_parts.append(f"--prefill={prefill}")
    if update is not None:
        command_parts.append(f"--update={update}")
    if multiplicity is not None:
        command_parts.append(f"--multiplicity={multiplicity}")
    if listen:
        command_parts.append("--listen")
    if bgpls is not None:
        command_parts.append(f"--bgpls {bgpls}")
    if skipAttr:
        command_parts.append("--skipattr")
    if allf:
        command_parts.append("--allf")
    if evpn:
        command_parts.append(f"--evpn")
    if wfr is not None:
        command_parts.append(f"--wfr {wfr}")
    if log_level:
        command_parts.append(f"--{log_level}")

    command_parts.append("--results bgp.csv >tmp/play.py.out 2>&1")

    process = infra.shell(" ".join(command_parts), run_in_background=True)

    return process


def start_bgp_speaker_and_verify_connected(*args, **kwargs) -> subprocess.Popen:
    """Starts bgp speaker and verifies if was successfully conncted.

    Args:
        *args: Bgp speaker positional arguments.
        **kwargs: Bgp speaker keyword arguments.

    Returns:
        subprocess.Popen: BGP speaker process handler.
    """
    bgp_speaker_process = start_bgp_speaker(*args, **kwargs)
    try:
        utils.wait_until_function_pass(3, 3, verify_bgp_speaker_connected, connected=True)
    except Exception as e:
        stop_bgp_speaker(bgp_speaker_process, gracefully=False)
        raise e
    return bgp_speaker_process


def start_bgp_speaker_with_verify_and_retry(
    *args, retries: int = 3, **kwargs
) -> subprocess.Popen:
    """Retries to start BGP speaker multiple times until it is connected.

    Args:
        *args: Bgp speaker positional arguments.
        **kwargs: Bgp speaker keyword arguments.

    Returns:
        subprocess.Popen: BGP speaker process handler.
    """
    bgp_speaker_process = utils.wait_until_function_pass(
        retry_count=retries,
        interval=1,
        function=start_bgp_speaker_and_verify_connected,
        *args,
        **kwargs,
    )

    return bgp_speaker_process


def stop_bgp_speaker(process: subprocess.Popen, gracefully=True):
    """??? New param gracefully Stop BGP speaker process by sending SIGINT signal.

    Args:
        process (subprocess.Popen): BGP speaker process handler.

    Returns:
        None
    """
    log.info(f"Killing bgp speaker process with PID {process.pid}")
    infra.stop_process_by_pid(process.pid, gracefully=gracefully)
    # dump BGP speaker logs
    rc, stdout = infra.shell("cat tmp/play.py.out")
    log.debug(f"Bgp speaker output: {stdout=}")


def verify_bgp_speaker_connected(
    ip: str = TOOLS_IP, connected: bool = True
) -> requests.Response:
    """Check if BGP session has been successfylly established.

    Args:
        ip (str): BP speaker ip address
        connected (bool): It True, expect to be connected,
            if False expect to not be connected.

    Returns:
        requests.Response: Requests library response as returned for GET call.
    """
    expected_response_code = 200 if connected else 404
    response = templated_requests.get_request(
        f"http://{ODL_IP}:{RESTCONF_PORT}/rests/data/bgp-rib:bgp-rib/rib=example-bgp-rib/peer=bgp:%2F%2F{ip}?content=nonconfig",
        expected_code=expected_response_code,
    )

    return response


def start_bgp_app_peer(
    count: int | None = None,
    command: str = "post",
    prefix: str = "8.0.0.0",
    prefix_len: int = 28,
    rib_ip: str = "10.0.0.10",
    log_level: str = "info",
    timeout: int = 1200,
) -> tuple[int, str]:
    """??? NOt necessary always to have these parames and if not provided skip them 
    and not provide to the scrpt count, prefix: str = "8.0.0.0",
    prefix_len: int = 28,"""
    """Starts bgp_app_peer.py for manipulating BGP neighbour rib table.

    This script is used for injecting and removing ipv4 prefixes
    directly to/from BGP neighour rib table in ODL using RESTCONF.

    Args:
        count (int): Number of ipv4 prefixes to be changed in ODL.
        command (str): Operation to be executed on these prefixes. (Could be: ...)
        prefix (int): Value of first prefix from sequence.
        prefix_len (int): Lenght of ip prefixes.
        uri (str): URI which should be used in RESTCONF call
            for manipulating prefixes in ODL rib table.
        log_level (str): Level of logs used by bgp app peer tool.
        timoeout (int): Timeout in seconds after which the test tool
            should be stopped if it did not finish.

    Returns:
        tuple[int, str]: Return code and standart ouput generated by
            the bgp app peer test tool.
    """
    rc, output = infra.shell(
        f"python tools/bgp_app_peer.py \
            {'--count ' +  str(count) if count is not None else ''}\
            --host {ODL_IP} \
            --port {RESTCONF_PORT} \
            --command {command} \
            --prefix {prefix} \
            --prefixlen {prefix_len} \
            {'--' + log_level + ' ' if log_level is not None else ''} \
            --uri  'data/bgp-rib:application-rib={rib_ip}/tables=bgp-types%3Aipv4-address-family,bgp-types%3Aunicast-subsequent-address-family'\
            --xml tools/ipv4-routes-template.xml \
            >bgp_app_peer.log 2>&1",
        timeout=timeout,
        run_in_background=False,
        check_rc=True,
    )

    return rc, output


def start_exabgp(
    cfg_file: str
) -> subprocess.Popen:
    """Starts exabgp peer simulator.

    This python package is used for simulating BGP neighbour.

    Args:
        cfg_file (str): Exabgp configuration file path.

    Returns:
        subprocess.Popen: Exabgp process handler.
    """
    command = f"env exabgp.tcp.port=1790 exabgp --debug {cfg_file}"
    
    process = infra.shell(command, run_in_background=True)

    return process

def verify_exabgp_connection(exabgp_ip: str, expect_connected: bool = True):
    """Checks peer presence in operational datastore.
    Args:
        exabgp_ip (str): Exabgp ip address.
        expect_connected(bool): Flag indicating of it is expected that the BGP
            session between exbgp and ODL was successfully created.

    Returns:
        bool: Flag indicating if exabgp was successfully connected to ODL.
    """
    expected_code = 200 if expect_connected else 404
    uri = f"rests/data/bgp-rib:bgp-rib/rib=example-bgp-rib/peer=bgp%3A%2F%2F{exabgp_ip}?content=nonconfig"
    url = f"http://{ODL_IP}:{RESTCONF_PORT}/{uri}"
    templated_requests.get_request(url, expected_code=expected_code)


def start_exabgp_and_verify_connected(cfg_file: str, exabgp_ip: str, expect_connected=True) -> subprocess.Popen:
    """Starts exabgp and verifies if was successfully conncted.

    Args:
        cfg_file (str): Exabgp configuration file path.
        exabgp_ip (str): Exabgp ip address.
        expect_connected(bool): Flag indicating of it is expected that the BGP
            session between exbgp and ODL was successfully created.

    Returns:
        subprocess.Popen: Exabgp process handler.
    """
    exabgp_process = start_exabgp(cfg_file)
    if expect_connected:
        utils.wait_until_function_pass(3, 3, verify_exabgp_connection, exabgp_ip=exabgp_ip, expect_connected=True)
    else:
        utils.verify_function_never_passes_within_timeout(7,2 , verify_exabgp_connection, exabgp_ip=exabgp_ip, expect_connected=True)
    return exabgp_process

def stop_exabgp(process: subprocess.Popen):
    """Stop exabgp process by sending SIGINT signal.

    Args:
        process (subprocess.Popen): Exabgp process handler.

    Returns:
        None
    """
    stdout = process.stdout
    log.debug(f"Exabgp stdout:\n{stdout}")
    log.info(f"Killing exabgp process with PID {process.pid}")
    infra.stop_process_by_pid(process.pid, gracefully=True)

def start_gobgp(
    gobgp_path: str,
    cfg_file: str
) -> subprocess.Popen:
    """???
    """
    command = f"{gobgp_path} -l debug -f {cfg_file} > tmp/gobgp.log"
    
    process = infra.shell(command, run_in_background=True)

    return process

def verify_gobgp_connection(gobgp_ip: str, expect_connected: bool = True):
    """???Checks peer presence in operational datastore.
    Args:
        exabgp_ip (str): Exabgp ip address.
        expect_connected(bool): Flag indicating of it is expected that the BGP
            session between exbgp and ODL was successfully created.

    Returns:
        bool: Flag indicating if exabgp was successfully connected to ODL.
    """
    expected_code = ALLOWED_STATUS_CODE if expect_connected else DENIED_STATUS_CODE
    uri = f"rests/data/bgp-rib:bgp-rib/rib=example-bgp-rib/peer=bgp%3A%2F%2F{gobgp_ip}?content=nonconfig"
    url = f"http://{ODL_IP}:{RESTCONF_PORT}/{uri}"
    templated_requests.get_request(url, expected_code=expected_code)

def start_gobgp_and_verify_connected(gobgp_path: str, cfg_file: str, gobgp_ip: str, retries_count: int = 3) -> subprocess.Popen:
    """???Starts exabgp and verifies if was successfully conncted.

    Args:
        cfg_file (str): Exabgp configuration file path.
        exabgp_ip (str): Exabgp ip address.
        expect_connected(bool): Flag indicating of it is expected that the BGP
            session between exbgp and ODL was successfully created.

    Returns:
        subprocess.Popen: Exabgp process handler.
    """
    gobgp_process = start_gobgp(gobgp_path, cfg_file)
    utils.wait_until_function_pass(5, 5, verify_exabgp_connection, exabgp_ip=gobgp_ip, expect_connected=True)

    return gobgp_process

def stop_gobgp(process: subprocess.Popen):
    """???Stop exabgp process by sending SIGINT signal.

    Args:
        process (subprocess.Popen): Exabgp process handler.

    Returns:
        None
    """
    stdout = process.stdout
    log.debug(f"Gobgp stdout:\n{stdout}")
    log.info(f"Killing gobgp process with PID {process.pid}")
    infra.stop_process_by_pid(process.pid, gracefully=True)

def play_to_odl_non_removal_template(to_test, dir):
    announce_hex = infra.get_file_content(f"{dir}/{to_test}/announce_{to_test}.hex")
    bgp_rpc_client = BgpRpcClient(TOOLS_IP)
    bgp_rpc_client.play_clean()
    bgp_rpc_client.play_send(announce_hex)
    mapping = {"PATH": "loc-rib", "BGP_RIB": "example-bgp-rib"}
    utils.wait_until_function_pass(3, 2, templated_requests.get_templated_request, f"{dir}/{to_test}/rib", mapping, verify=True)

def odl_to_play_template(to_test, dir, remove=True):
    announce_hex = infra.get_file_content(f"{dir}/{to_test}/announce_{to_test}.hex")
    announce_hex = announce_hex.strip()
    withdraw_hex = infra.get_file_content(f"{dir}/{to_test}/withdraw_{to_test}.hex")
    withdraw_hex = withdraw_hex.strip()
    try:
        if remove:
            BGP_RPC_CLIENT.play_clean()
        mapping = {"IP": ODL_IP, "BGP_RIB": "example-bgp-rib"}
        templated_requests.post_templated_request(f"{dir}/{to_test}/app", mapping, json=False)
        update_messag = utils.wait_until_function_pass(3, 2, get_update_message)
        verify_two_hex_messages_are_equal(update_messag, announce_hex)
        BGP_RPC_CLIENT.play_clean()
        remove_configured_routes(to_test, dir)
        update_messag = utils.wait_until_function_pass(3, 2, get_update_message)
        verify_two_hex_messages_are_equal(update_messag, withdraw_hex)
    finally:
        utils.run_function_ignore_errors(remove_configured_routes, to_test, dir)

def get_update_message():
    """Returns hex update message."""
    update_message = BGP_RPC_CLIENT.play_get()
    assert update_message, "Expected update message to be returned"
    return update_message

def remove_configured_routes(to_test: str, dir: str):
    """Removes the route if present."""
    mapping = {"IP": ODL_IP, "BGP_RIB": "example-bgp-rib"}
    templated_requests.delete_templated_request(f"{dir}/{to_test}/app", mapping)

def verify_two_hex_messages_are_equal(hex1: str, hex2: str):
    """Verifies two hex messages are equal even in case, their arguments are misplaced.
    Compares length of the hex messages and sums hex messages arguments as integers and compares results."""
    assert len(hex1) == len(hex2), f"Hex values are different, they do not have the same lenght: {hex1=} {hex2=} {len(hex1)} != {len(hex2)}"
    bgp_rpc_client = BgpRpcClient(TOOLS_IP)
    hex1_sum = bgp_rpc_client.sum_hex_message(hex1)
    hex2_sum = bgp_rpc_client.sum_hex_message(hex2)
    assert hex1_sum == hex2_sum, f"Hex values are different, their sum values are different: {hex1=} {hex2=} {hex1_sum} != {hex2_sum}"

