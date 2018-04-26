"""This program performs required BGP application peer operations."""

# Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import requests
import ipaddr
import argparse
import logging
import time
import xml.dom.minidom as md


__author__ = "Radovan Sajben"
__copyright__ = "Copyright(c) 2015, Cisco Systems, Inc."
__license__ = "Eclipse Public License v1.0"
__email__ = "rsajben@cisco.com"


def _build_url(odl_ip, port, uri):
    """Compose URL from generic IP, port and URI fragment.

    Args:
        :param odl_ip: controller's ip address or hostname

        :param port: controller's restconf port

        :param uri: URI without /restconf/ to complete URL

    Returns:
        :returns url: full restconf url corresponding to params
    """

    url = "http://" + str(odl_ip) + ":" + port + "/restconf/" + uri
    return url


def _stream_data(xml_template, prefix_base, prefix_len, count, route_key=False, element="ipv4-routes"):
    """Stream list of routes based on xml template. Memory non-consumable
    data generation (on the fly).

    Args:
        :xml_template: xml template for routes

        :prefix_base: first prefix IP address

        :prefix_len: prefix length in bits

        :count: number of routes to be generated

        :route_key: bool deciding route-key tag existence

        :element: element to be returned

    Returns:
        :yield xml_data: requested data by elements as xml data
    """
    global total_build_data_time_counter

    routes = md.parse(xml_template)

    routes_node = routes.getElementsByTagName("ipv4-routes")[0]
    route_node = routes.getElementsByTagName("ipv4-route")[0]
    routes_node.removeChild(route_node)
    route_prefix = route_node.getElementsByTagName("prefix")[0]
    if route_key:
        key = route_node.getElementsByTagName("route-key")[0]
    prefix_gap = 2 ** (32 - prefix_len)
    prefix_index_list = range(count)
    if element == routes_node.tagName:
        lines = routes_node.toxml().splitlines()
        xml_head = lines[0] + "\n"
        xml_tail = "\n".join(lines[1:])
    elif element == route_node.tagName:
        xml_head = ""
        xml_tail = ""
        route_node.setAttribute("xmlns", route_node.namespaceURI)
    else:
        prefix_index_list = range(0)

    for prefix_index in prefix_index_list:
        build_data_timestamp = time.time()
        prefix = prefix_base + prefix_index * prefix_gap
        prefix_str = str(prefix) + "/" + str(prefix_len)
        route_prefix.childNodes[0].nodeValue = prefix_str
        if route_key:
            key.childNodes[0].nodeValue = prefix_str
        xml_data = route_node.toxml()
        if prefix_index == 0:
            xml_data = xml_head + xml_data
        if prefix_index == len(prefix_index_list) - 1:
            xml_data = xml_data + xml_tail
        chunk = prefix_index + 1
        if not (chunk % 1000):
            logger.info("... streaming chunk %s (prefix: %s)", chunk, prefix_str)
        else:
            logger.debug("...streaming chunk %s (prefix: %s)", chunk, prefix_str)
        logger.debug("xml data\n%s", xml_data)
        total_build_data_time_counter += time.time() - build_data_timestamp
        yield xml_data


def send_request(operation, odl_ip, port, uri, auth, xml_data=None, expect_status_code=200):
    """Send a http request.

    Args:
        :operation: GET, POST, PUT, DELETE

        :param odl_ip: controller's ip address or hostname

        :param port: controller's restconf port

        :param uri: URI without /restconf/ to complete URL

        :param auth: authentication credentials

        :param xml_data: list of routes as xml data

    Returns:
        :returns http response object
    """
    global total_response_time_counter
    global total_number_of_responses_counter

    ses = requests.Session()

    url = _build_url(odl_ip, port, uri)
    header = {"Content-Type": "application/xml"}
    req = requests.Request(operation, url, headers=header, data=xml_data, auth=auth)
    prep = req.prepare()
    try:
        send_request_timestamp = time.time()
        rsp = ses.send(prep, timeout=60)
        total_response_time_counter += time.time() - send_request_timestamp
        total_number_of_responses_counter += 1
    except requests.exceptions.Timeout:
        logger.error("No response from %s", odl_ip)
    else:
        if rsp.status_code == expect_status_code:
            logger.debug("%s %s", rsp.request, rsp.request.url)
            logger.debug("Request headers: %s:", rsp.request.headers)
            logger.debug("Response: %s", rsp.text)
            logger.debug("%s %s", rsp, rsp.reason)
        else:
            logger.error("%s %s", rsp.request, rsp.request.url)
            logger.error("Request headers: %s:", rsp.request.headers)
            logger.error("Response: %s", rsp.text)
            logger.error("%s %s", rsp, rsp.reason)
        return rsp


def get_prefixes(odl_ip, port, uri, auth, prefix_base=None, prefix_len=None,
                 count=None, xml_template=None):
    """Send a http GET request for getting all prefixes.

    Args:
        :param odl_ip: controller's ip address or hostname

        :param port: controller's restconf port

        :param uri: URI without /restconf/ to complete URL

        :param auth: authentication tupple as (user, password)

        :param prefix_base: IP address of the first prefix

        :prefix_len: length of the prefix in bites (specifies the increment as well)

        :param count: number of prefixes to be processed

        :param xml_template: xml template for building the xml data

    Returns:
        :returns None
    """

    logger.info("Get all prefixes from %s:%s/restconf/%s", odl_ip, port, uri)
    rsp = send_request("GET", odl_ip, port, uri, auth)
    if rsp is not None:
        s = rsp.text
        s = s.replace("{", "")
        s = s.replace("}", "")
        s = s.replace("[", "")
        s = s.replace("]", "")
        prefixes = ''
        prefix_count = 0
        for item in s.split(","):
            if "prefix" in item:
                prefixes += item + ","
                prefix_count += 1
        prefixes = prefixes[:len(prefixes) - 1]
        logger.debug("prefix_list=%s", prefixes)
        logger.info("prefix_count=%s", prefix_count)


def post_prefixes(odl_ip, port, uri, auth, prefix_base=None, prefix_len=None,
                  count=0, route_key=False, xml_template=None):
    """Send a http POST request for creating a prefix list.

    Args:
        :param odl_ip: controller's ip address or hostname

        :param port: controller's restconf port

        :param uri: URI without /restconf/ to complete URL

        :param auth: authentication tupple as (user, password)

        :param prefix_base: IP address of the first prefix

        :prefix_len: length of the prefix in bites (specifies the increment as well)

        :param count: number of prefixes to be processed

        :route_key: bool deciding route-key tag existence

        :param xml_template: xml template for building the xml data (not used)

    Returns:
        :returns None
    """
    logger.info("Post %s prefix(es) in a single request (starting from %s/%s) into %s:%s/restconf/%s",
                count, prefix_base, prefix_len, odl_ip, port, uri)
    xml_stream = _stream_data(xml_template, prefix_base, prefix_len, count, route_key)
    send_request("POST", odl_ip, port, uri, auth, xml_data=xml_stream, expect_status_code=204)


def put_prefixes(odl_ip, port, uri, auth, prefix_base, prefix_len, count,
                 route_key, xml_template=None):
    """Send a http PUT request for updating the prefix list.

    Args:
        :param odl_ip: controller's ip address or hostname

        :param port: controller's restconf port

        :param uri: URI without /restconf/ to complete URL

        :param auth: authentication tupple as (user, password)

        :param prefix_base: IP address of the first prefix

        :prefix_len: length of the prefix in bites (specifies the increment as well)

        :param count: number of prefixes to be processed

        :param xml_template: xml template for building the xml data (not used)

    Returns:
        :returns None
    """
    uri_add_prefix = uri + _uri_suffix_ipv4_routes
    logger.info("Put %s prefix(es) in a single request (starting from %s/%s) into %s:%s/restconf/%s",
                count, prefix_base, prefix_len, odl_ip, port, uri_add_prefix)
    xml_stream = _stream_data(xml_template, prefix_base, prefix_len, count, route_key)
    send_request("PUT", odl_ip, port, uri_add_prefix, auth, xml_data=xml_stream)


def add_prefixes(odl_ip, port, uri, auth, prefix_base, prefix_len, count,
                 route_key, xml_template=None):
    """Send a consequent http POST request for adding prefixes.

    Args:
        :param odl_ip: controller's ip address or hostname

        :param port: controller's restconf port

        :param uri: URI without /restconf/ to complete URL

        :param auth: authentication tupple as (user, password)

        :param prefix_base: IP address of the first prefix

        :prefix_len: length of the prefix in bites (specifies the increment as well)

        :param count: number of prefixes to be processed

        :param xml_template: xml template for building the xml data (not used)

    Returns:
        :returns None
    """
    logger.info("Add %s prefixes (starting from %s/%s) into %s:%s/restconf/%s",
                count, prefix_base, prefix_len, odl_ip, port, uri)
    uri_add_prefix = uri + _uri_suffix_ipv4_routes
    prefix_gap = 2 ** (32 - prefix_len)
    for prefix_index in range(count):
        prefix = prefix_base + prefix_index * prefix_gap
        logger.info("Adding prefix %s/%s to %s:%s/restconf/%s",
                    prefix, prefix_len, odl_ip, port, uri)
        xml_stream = _stream_data(xml_template, prefix, prefix_len, 1, route_key,
                                  element="ipv4-route")
        send_request("POST", odl_ip, port, uri_add_prefix, auth,
                     xml_data=xml_stream, expect_status_code=204)


def delete_prefixes(odl_ip, port, uri, auth, prefix_base, prefix_len, count,
                    xml_template=None):
    """Send a http DELETE requests for deleting prefixes.

    Args:
        :param odl_ip: controller's ip address or hostname

        :param port: controller's restconf port

        :param uri: URI without /restconf/ to complete URL

        :param auth: authentication tupple as (user, password)

        :param prefix_base: IP address of the first prefix

        :prefix_len: length of the prefix in bites (specifies the increment as well)

        :param count: number of prefixes to be processed

        :param xml_template: xml template for building the xml data (not used)

    Returns:
        :returns None
    """
    logger.info("Delete %s prefix(es) (starting from %s/%s) from %s:%s/restconf/%s",
                count, prefix_base, prefix_len, odl_ip, port, uri)
    partkey = "/0"
    uri_del_prefix = uri + _uri_suffix_ipv4_routes + _uri_suffix_ipv4_route
    prefix_gap = 2 ** (32 - prefix_len)
    for prefix_index in range(count):
        prefix = prefix_base + prefix_index * prefix_gap
        logger.info("Deleting prefix %s/%s/%s from %s:%s/restconf/%s",
                    prefix, prefix_len, partkey, odl_ip, port, uri)
        send_request("DELETE", odl_ip, port,
                     uri_del_prefix + str(prefix) + "%2F" + str(prefix_len) + partkey, auth)


def delete_all_prefixes(odl_ip, port, uri, auth, prefix_base=None,
                        prefix_len=None, count=None, xml_template=None):
    """Send a http DELETE request for deleting all prefixes.

    Args:
        :param odl_ip: controller's ip address or hostname

        :param port: controller's restconf port

        :param uri: URI without /restconf/ to complete URL

        :param auth: authentication tupple as (user, password)

        :param prefix_base: IP address of the first prefix (not used)

        :prefix_len: length of the prefix in bites (not used)

        :param count: number of prefixes to be processed (not used)

        :param xml_template: xml template for building the xml data (not used)

    Returns:
        :returns None
    """
    logger.info("Delete all prefixes from %s:%s/restconf/%s", odl_ip, port, uri)
    uri_del_all_prefixes = uri + _uri_suffix_ipv4_routes
    send_request("DELETE", odl_ip, port, uri_del_all_prefixes, auth)


_commands = ["post", "put", "add", "delete", "delete-all", "get"]
_uri_suffix_ipv4_routes = "bgp-inet:ipv4-routes/"
_uri_suffix_ipv4_route = "bgp-inet:ipv4-route/"   # followed by IP address like 1.1.1.1%2F32

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="BGP application peer script")
    parser.add_argument("--host", type=ipaddr.IPv4Address, default="127.0.0.1",
                        help="ODL controller IP address")
    parser.add_argument("--port", default="8181",
                        help="ODL RESTCONF port")
    parser.add_argument("--command", choices=_commands, metavar="command",
                        help="Command to be performed."
                        "post, put, add, delete, delete-all, get")
    parser.add_argument("--prefix", type=ipaddr.IPv4Address, default="8.0.1.0",
                        help="First prefix IP address")
    parser.add_argument("--prefixlen", type=int, help="Prefix length in bites",
                        default=28)
    parser.add_argument("--count", type=int, help="Number of prefixes",
                        default=1)
    parser.add_argument("--user", help="Restconf user name", default="admin")
    parser.add_argument("--password", help="Restconf password", default="admin")
    parser.add_argument("--uri", help="The uri part of requests",
                        default="config/bgp-rib:application-rib/example-app-rib/"
                                "tables/bgp-types:ipv4-address-family/"
                                "bgp-types:unicast-subsequent-address-family/")
    parser.add_argument("--xml", help="File name of the xml data template",
                        default="ipv4-routes-template.xml")
    parser.add_argument("--error", dest="loglevel", action="store_const",
                        const=logging.ERROR, default=logging.INFO,
                        help="Set log level to error (default is info)")
    parser.add_argument("--warning", dest="loglevel", action="store_const",
                        const=logging.WARNING, default=logging.INFO,
                        help="Set log level to warning (default is info)")
    parser.add_argument("--info", dest="loglevel", action="store_const",
                        const=logging.INFO, default=logging.INFO,
                        help="Set log level to info (default is info)")
    parser.add_argument("--debug", dest="loglevel", action="store_const",
                        const=logging.DEBUG, default=logging.INFO,
                        help="Set log level to debug (default is info)")
    parser.add_argument("--logfile", default="bgp_app_peer.log", help="Log file name")
    parser.add_argument("--stream", default="", help="Stream - oxygen, fluorine ...")

    args = parser.parse_args()

    logger = logging.getLogger("logger")
    log_formatter = logging.Formatter("%(asctime)s %(levelname)s: %(message)s")
    console_handler = logging.StreamHandler()
    file_handler = logging.FileHandler(args.logfile, mode="w")
    console_handler.setFormatter(log_formatter)
    file_handler.setFormatter(log_formatter)
    logger.addHandler(console_handler)
    logger.addHandler(file_handler)
    logger.setLevel(args.loglevel)

    auth = (args.user, args.password)

    odl_ip = args.host
    port = args.port
    command = args.command
    prefix_base = args.prefix
    prefix_len = args.prefixlen
    count = args.count
    auth = (args.user, args.password)
    uri = args.uri
    # From Fluorine onward route-key argument is mandatory for identification.
    route_key_stream = ["fluorine"]
    [xml_template, route_key] = ["{}.{}".format(args.xml, args.stream), True] \
        if args.stream in route_key_stream else [args.xml, False]

    test_start_time = time.time()
    total_build_data_time_counter = 0
    total_response_time_counter = 0
    total_number_of_responses_counter = 0

    if command == "post":
        post_prefixes(odl_ip, port, uri, auth, prefix_base, prefix_len, count,
                      route_key, xml_template)
    if command == "put":
        put_prefixes(odl_ip, port, uri, auth, prefix_base, prefix_len, count,
                     route_key, xml_template)
    if command == "add":
        add_prefixes(odl_ip, port, uri, auth, prefix_base, prefix_len, count,
                     route_key, xml_template)
    elif command == "delete":
        delete_prefixes(odl_ip, port, uri, auth, prefix_base, prefix_len, count)
    elif command == "delete-all":
        delete_all_prefixes(odl_ip, port, uri, auth)
    elif command == "get":
        get_prefixes(odl_ip, port, uri, auth)

    total_test_execution_time = time.time() - test_start_time

    logger.info("Total test execution time: %.3fs", total_test_execution_time)
    logger.info("Total build data time: %.3fs", total_build_data_time_counter)
    logger.info("Total response time: %.3fs", total_response_time_counter)
    logger.info("Total number of response(s): %s", total_number_of_responses_counter)
    file_handler.close()
