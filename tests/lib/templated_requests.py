#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import difflib
import logging
import os
import requests
import string

from lib import norm_json

ODL_IP = os.environ["ODL_IP"]
RESTCONF_PORT = os.environ["RESTCONF_PORT"]

log = logging.getLogger(__name__)


def get_request(url: str, expected_code: int | None = None) -> requests.Response:
    """Sends HTTP GET request to ODL.

    Args:
        url (str): URL address.
        expected_code (int | None): Expected resposne code returned by ODL. If not provided requests standard logic for evaluating failure response code is used.

    Returns:
        requests.Response: Response returned by ODL for GET call.
    """
    log.info(f"Sending GET request to {url}")
    response = requests.get(url, auth=requests.auth.HTTPBasicAuth("admin", "admin"))

    try:
        if expected_code is not None:
            if expected_code != response.status_code:
                raise AssertionError(
                    f"Unexpected resonse code {response.status_code}, expected {expected_code}"
                )
        else:
            response.raise_for_status()
    except (requests.HTTPError, AssertionError):
        log.error(f"Response: {response.text}")
        log.error(f"Response code: {response.status_code}")
        log.error(f"Response headers: {response.headers}")
        raise

    log.debug(f"Response: {response.text}")
    log.info(f"Response code: {response.status_code}")
    log.debug(f"Response headers: {response.headers}")

    return response


def put_request(
    url: str, headers: dict, data: dict | str, expected_code: int | None = None
) -> requests.Response:
    """Sends HTTP PUT request to ODL using provided data.

    Args:
        url (str): URL address.
        expected_code (int | None): Expected resposne code returned by ODL. If not provided requests standard logic for evaluating failure response code is used.
        data (dict | str): payload to be sent within the PUT request to ODL

    Returns:
        requests.Response: Response returned by ODL for PUT call.
    """
    log.info(f"Sending to {url} this data: {data}")
    response = requests.put(
        url=url,
        data=data,
        headers=headers,
        auth=requests.auth.HTTPBasicAuth("admin", "admin"),
    )

    try:
        if expected_code is not None:
            if expected_code != response.status_code:
                raise AssertionError(
                    f"Unexpected resonse code {response.status_code}, expected {expected_code}"
                )
        else:
            response.raise_for_status()
    except (requests.HTTPError, AssertionError):
        log.error(f"Response: {response.text}")
        log.error(f"Response code: {response.status_code}")
        log.error(f"Response headers: {response.headers}")
        raise

    log.debug(f"Response: {response.text}")
    log.info(f"Response code: {response.status_code}")
    log.debug(f"Response headers: {response.headers}")

    return response


def delete_request(url: str, expected_code: int | None = None) -> requests.Response:
    """Sends HTTP DELETE request to ODL.

    Args:
        url (str): URL address.
        expected_code (int | None): Expected resposne code returned by ODL. If not provided requests standard logic for evaluating failure response code is used.

    Returns:
        requests.Response: Response returned by ODL for GET call.
    """
    log.info(f"Sending request to delete {url}")
    response = requests.delete(
        url=url, auth=requests.auth.HTTPBasicAuth("admin", "admin")
    )

    try:
        if expected_code is not None:
            if expected_code != response.status_code:
                raise AssertionError(
                    f"Unexpected resonse code {response.status_code}, expected {expected_code}"
                )
        else:
            response.raise_for_status()
    except (requests.HTTPError, AssertionError):
        log.error(f"Response: {response.text}")
        log.error(f"Response code: {response.status_code}")
        log.error(f"Response headers: {response.headers}")
        raise

    log.debug(f"Response: {response.text}")
    log.info(f"Response code: {response.status_code}")
    log.debug(f"Response headers: {response.headers}")

    return response


def resolve_templated_text(template_location: str, mapping: dict) -> str:
    """Evaluates templated text using provided value mapping.

    Args:
        template_location (str): Path to template text file.
        mapping (dict): Dictionary with all value mapping between placeholder values specified in template and expected value.

    Returns:
        str: Evaluated template file.
    """
    with open(template_location) as template_file:
        template = template_file.read()
    resolved_tempate = string.Template(template.rstrip()).safe_substitute(mapping)

    return resolved_tempate


def get_templated_request(
    temlate_dir: str, mapping: dict, json: bool = True, verify: bool = False
) -> requests.Response:
    """Evaluates and sends GET request using template file from provided template dir and value mapping.

    Args:
        temlate_dir (str): Path to directory containing template text file(s).
        mapping (dict): Dictionary with all value mapping between placeholder values specified in template and expected value.
        json (bool): If true, use json template (file name with .json suffix), otherwise use xml tempale (file anem with .xml suffix).
        verify (bool): If true, verify returned response with stored template file.

    Returns:
        requests.Response: Response returned by ODL for GET call.
    """
    url = resolve_templated_text(temlate_dir + "/location.uri", mapping)
    response = get_request(f"http://{ODL_IP}:{RESTCONF_PORT}/{url}")

    if verify:
        file_name_suffix = "json" if json else "xml"
        expected_response = resolve_templated_text(
            temlate_dir + "/data." + file_name_suffix, mapping
        )
        normalized_expected_response = norm_json.normalize_json_text(expected_response)
        normalized_response = norm_json.normalize_json_text(response.text)
        visual_diff = "\n".join(
            difflib.unified_diff(
                normalized_response.splitlines(),
                normalized_expected_response.splitlines(),
                fromfile="received response",
                tofile="expected response",
                lineterm="",
            )
        )
        assert (
            normalized_response == normalized_expected_response
        ), f"Received response does not match expected response: \n{visual_diff}"

    return response


def put_templated_request(
    temlate_dir: str, mapping: dict, json=True
) -> requests.Response:
    """Evaluates and sends PUT request using template file from provided template dir and value mapping.

    Args:
        temlate_dir (str): Path to directory containing template text file(s).
        mapping (dict): Dictionary with all value mapping between placeholder values specified in template and expected value.
        json (bool): If true, use json template (file name with .json suffix), otherwise use xml tempale (file anem with .xml suffix).

    Returns:
        requests.Response: Response returned by ODL for PUT call.
    """
    if json:
        data_file_name = "data.json"
        headers = {"Content-Type": "application/yang-data+json"}
    else:
        data_file_name = "data.xml"
        headers = {"Accept": "application/xml", "Content-Type": "application/xml"}
    url = resolve_templated_text(temlate_dir + "/location.uri", mapping)
    data = resolve_templated_text(temlate_dir + "/" + data_file_name, mapping)
    response = put_request(f"http://{ODL_IP}:{RESTCONF_PORT}/{url}", headers, data)

    return response


def delete_templated_request(temlate_dir: str, mapping: dict) -> requests.Response:
    """Evaluates and sends DELETE request using template file from provided template dir and value mapping.

    Args:
        temlate_dir (str): Path to directory containing template text file(s).
        mapping (dict): Dictionary with all value mapping between placeholder values specified in template and expected value.

    Returns:
        requests.Response: Response returned by ODL for DELETE call.
    """
    url = resolve_templated_text(temlate_dir + "/location.uri", mapping)
    response = delete_request(f"http://{ODL_IP}:{RESTCONF_PORT}/{url}")

    return response
