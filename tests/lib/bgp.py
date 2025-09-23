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

from lib import infra
from lib import templated_requests
from lib import utils

ODL_IP = os.environ["ODL_IP"]
RESTCONF_PORT = os.environ["RESTCONF_PORT"]

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
    listen: bool | None = None,
    log_level: str | None = None,
) -> subprocess.Popen:
    """Starts play.py script as BGP speaker.

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
        log_level (str): Level of logs used by bgp app peer tool.

    Returns:
        subprocess.Popen: BGP speaker process handler.
    """
    command_parts = [
        f"python tools/play.py --amount {ammount}\
            --myip=127.0.0.1 \
            --myport=17900 \
            --peerip=127.0.0.1 \
            --peerport=1790"
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
    utils.wait_until_function_pass(3, 3, verify_bgp_speaker_connected, connected=True)
    return bgp_speaker_process


def start_bgp_speaker_with_verify_and_retry(
    retries: int = 3, *args, **kwargs
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


def stop_bgp_speaker(process: subprocess.Popen):
    """Stop BGP speaker process by sending SIGINT signal.

    Args:
        process (subprocess.Popen): BGP speaker process handler.

    Returns:
        None
    """
    log.info(f"Killing bgp speaker process with PID {process.pid}")
    infra.stop_process(process, gracefully=True)
    # dump BGP speaker logs
    rc, stdout = infra.shell("cat tmp/play.py.out")
    log.debug(f"Bgp speaker output: {stdout=}")


def verify_bgp_speaker_connected(
    ip: str = "127.0.0.1", connected: bool = True
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
    uri: str = "data/bgp-rib:application-rib=10.0.0.10/tables=bgp-types%3Aipv4-address-family,bgp-types%3Aunicast-subsequent-address-family",
    log_level: str = "info",
    timeout: int = 1200,
) -> tuple[int, str]:
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
            --uri {uri} \
            --xml tools/ipv4-routes-template.xml \
            >bgp_app_peer.log 2>&1",
        timeout=timeout,
        run_in_background=False,
        check_rc=True,
    )

    return rc, output
