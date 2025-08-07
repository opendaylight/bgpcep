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

def put_request(url, data, expected_code=None):
    log.info(f"Sending to {url} this data: {data}")
    response = requests.put(url=url, data=data, auth=requests.auth.HTTPBasicAuth('admin', 'admin'))
    log.debug(response.text)
    log.debug(response.status_code)

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

def put_templated_request(temlate, mapping):
    url = resolve_templated_text(temlate + "/location.uri", mapping)
    data = resolve_templated_text(temlate + "/data.json", mapping)
    response = put_request(f"http://{ODL_IP}:{RESTCONF_PORT}/{url}", data)

    return response

def delete_templated_request(temlate, mapping):
    url = resolve_templated_text(temlate + "/location.uri", mapping)
    response = delete_request(f"http://{ODL_IP}:{RESTCONF_PORT}/{url}")

    return response