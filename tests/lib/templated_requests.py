#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import logging
import requests
import string

ODL_IP = "127.0.0.1"
RESTCONF_PORT = "8181"

log = logging.getLogger(__name__)

def get_request(url, expected_code=None):
    log.info(f"Sending request to get {url}")
    response = requests.get(url, auth=requests.auth.HTTPBasicAuth('admin', 'admin'))
    log.debug(response.text)
    log.debug(response.status_code)

    return response

def put_request(url, headers, data, expected_code=None):
    log.info(f"Sending to {url} this data: {data}")
    log.debug(f"{headers=}")
    response = requests.put(url=url, data=data, headers=headers, auth=requests.auth.HTTPBasicAuth('admin', 'admin'))
    log.info(f"{response.request.headers=}")
    log.info(f"{response.text=}")
    log.info(f"{response.status_code=}")

    return response

def delete_request(url, expected_code=None):
    log.info(f"Sending request to delete {url}")
    response = requests.delete(url=url, auth=requests.auth.HTTPBasicAuth('admin', 'admin'))
    log.info(response.text)
    log.info(response.status_code)

    return response

def resolve_templated_text(template, mapping):
    with open(template) as template_file:
        template = template_file.read()
    resolved_tempate = string.Template(template.rstrip()).safe_substitute(mapping)

    return resolved_tempate

def put_templated_request(temlate, mapping, json=True):
    if json:
        data_file_name = "data.json"
        headers = {"Content-Type": "application/yang-data+json"}
    else:
        data_file_name = "data.xml"
        headers = {"Accept": "application/xml", 'Content-Type': 'application/xml'}
    url = resolve_templated_text(temlate + "/location.uri", mapping)
    data = resolve_templated_text(temlate + "/" + data_file_name, mapping)
    response = put_request(f"http://{ODL_IP}:{RESTCONF_PORT}/{url}",  headers, data)

    return response

def delete_templated_request(temlate, mapping):
    url = resolve_templated_text(temlate + "/location.uri", mapping)
    response = delete_request(f"http://{ODL_IP}:{RESTCONF_PORT}/{url}")

    return response