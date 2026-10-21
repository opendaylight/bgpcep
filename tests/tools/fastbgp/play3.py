"""Utility for playing generated BGP data to ODL supporting RFC 9857, RFC 9552, and SRv6 SID (NLRI Type 6).

It needs to be run with sudo-able user when you want to use ports below 1024
as --myip. This utility is used to avoid excessive waiting times which EXABGP
exhibits when used with huge router tables and also avoid the memory cost of
EXABGP in this type of scenario."""

# Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

from copy import deepcopy
from xmlrpc.server import SimpleXMLRPCServer
import argparse
import binascii
import ipaddr
import logging
import queue
import select
import socket
import struct
import threading
import time


__author__ = "Vratko Polak"
__copyright__ = "Copyright(c) 2015, Cisco Systems, Inc."
__license__ = "Eclipse Public License v1.0"
__email__ = "vrpolak@cisco.com"


class SafeDict(dict):
    """Thread safe dictionary

    The object will serve as thread safe data storage.
    It should be used with "with" statement.
    """

    def __init__(self, *p_arg, **n_arg):
        super(SafeDict, self).__init__()
        self._lock = threading.Lock()

    def __enter__(self):
        self._lock.acquire()
        return self

    def __exit__(self, type, value, traceback):
        self._lock.release()


def parse_arguments():
    """Use argparse to get arguments,

    Returns:
        :return: args object.
    """
    parser = argparse.ArgumentParser()
    # TODO: Should we use --argument-names-with-spaces?
    str_help = "Autonomous System number use in the stream."
    parser.add_argument("--asnumber", default=64496, type=int, help=str_help)
    # FIXME: We are acting as iBGP peer,
    # we should mirror AS number from peer's open message.
    str_help = "Amount of IP prefixes to generate. (negative means infinite)."
    parser.add_argument("--amount", default="1", type=int, help=str_help)
    str_help = "Rpc server port."
    parser.add_argument("--port", default="8000", type=int, help=str_help)
    str_help = "Maximum number of IP prefixes to be announced in one iteration"
    parser.add_argument("--insert", default="1", type=int, help=str_help)
    str_help = "Maximum number of IP prefixes to be withdrawn in one iteration"
    parser.add_argument("--withdraw", default="0", type=int, help=str_help)
    str_help = "The number of prefixes to process without withdrawals"
    parser.add_argument("--prefill", default="0", type=int, help=str_help)
    str_help = "Single or two separate UPDATEs for NLRI and WITHDRAWN lists sent"
    parser.add_argument(
        "--updates", choices=["single", "separate"], default=["separate"], help=str_help
    )
    str_help = "Base prefix IP address for prefix generation"
    parser.add_argument(
        "--firstprefix", default="8.0.1.0", type=ipaddr.IPv4Address, help=str_help
    )
    str_help = "The prefix length."
    parser.add_argument("--prefixlen", default=28, type=int, help=str_help)
    str_help = "Listen for connection, instead of initiating it."
    parser.add_argument("--listen", action="store_true", help=str_help)
    str_help = (
        "Numeric IP Address to bind to and derive BGP ID from."
        + "Default value only suitable for listening."
    )
    parser.add_argument(
        "--myip", default="0.0.0.0", type=ipaddr.IPv4Address, help=str_help
    )
    str_help = (
        "TCP port to bind to when listening or initiating connection."
        + "Default only suitable for initiating."
    )
    parser.add_argument("--myport", default="0", type=int, help=str_help)
    str_help = "The IP of the next hop to be placed into the update messages."
    parser.add_argument(
        "--nexthop",
        default="192.0.2.1",
        type=ipaddr.IPv4Address,
        dest="nexthop",
        help=str_help,
    )
    str_help = "Identifier of the route originator."
    parser.add_argument(
        "--originator",
        default=None,
        type=ipaddr.IPv4Address,
        dest="originator",
        help=str_help,
    )
    str_help = "Cluster list item identifier."
    parser.add_argument(
        "--cluster",
        default=None,
        type=ipaddr.IPv4Address,
        dest="cluster",
        help=str_help,
    )
    str_help = (
        "Numeric IP Address to try to connect to."
        + "Currently no effect in listening mode."
    )
    parser.add_argument(
        "--peerip", default="127.0.0.2", type=ipaddr.IPv4Address, help=str_help
    )
    str_help = "TCP port to try to connect to. No effect in listening mode."
    parser.add_argument("--peerport", default="179", type=int, help=str_help)
    str_help = "Local hold time."
    parser.add_argument("--holdtime", default="180", type=int, help=str_help)
    str_help = "Log level (--error, --warning, --info, --debug)"
    parser.add_argument(
        "--error",
        dest="loglevel",
        action="store_const",
        const=logging.ERROR,
        default=logging.INFO,
        help=str_help,
    )
    parser.add_argument(
        "--warning",
        dest="loglevel",
        action="store_const",
        const=logging.WARNING,
        default=logging.INFO,
        help=str_help,
    )
    parser.add_argument(
        "--info",
        dest="loglevel",
        action="store_const",
        const=logging.INFO,
        default=logging.INFO,
        help=str_help,
    )
    parser.add_argument(
        "--debug",
        dest="loglevel",
        action="store_const",
        const=logging.DEBUG,
        default=logging.INFO,
        help=str_help,
    )
    str_help = "Log file name"
    parser.add_argument("--logfile", default="bgp_peer.log", help=str_help)
    str_help = "Trailing part of the csv result files for plotting purposes"
    parser.add_argument("--results", default="bgp.csv", type=str, help=str_help)
    str_help = "Minimum number of updates to reach to include result into csv."
    parser.add_argument("--threshold", default="1000", type=int, help=str_help)
    str_help = "RFC 4760 Multiprotocol Extensions for BGP-4 supported"
    parser.add_argument("--rfc4760", default=True, type=bool, help=str_help)
    str_help = "Using peerip instead of myip for xmlrpc server"
    parser.add_argument(
        "--usepeerip", default=False, action="store_true", help=str_help
    )
    str_help = "Link-State NLRI supported"
    parser.add_argument("--bgpls", default=False, type=bool, help=str_help)
    str_help = "Link-State NLRI: Identifier"
    parser.add_argument("-lsid", "--lsid", dest="lsid", default=1, type=int, help=str_help)
    str_help = "Link-State NLRI: Policy Color"
    parser.add_argument(
        "--color", "-lstid", dest="color", default=100, type=int, help=str_help
    )
    str_help = "Link-State NLRI: Discriminator"
    parser.add_argument(
        "--discriminator",
        "-lspid",
        dest="discriminator",
        default=1,
        type=int,
        help=str_help,
    )
    str_help = "Link-State NLRI: Originator IP Address"
    parser.add_argument(
        "--originator-ip",
        "--lstsaddr",
        dest="originator_ip",
        default=ipaddr.IPv4Address("1.2.3.4"),
        type=ipaddr.IPv4Address,
        help=str_help,
    )
    str_help = "Link-State NLRI: Endpoint IP Address"
    parser.add_argument(
        "--endpoint-ip",
        "--lsteaddr",
        dest="endpoint_ip",
        default=ipaddr.IPv4Address("5.6.7.8"),
        type=ipaddr.IPv4Address,
        help=str_help,
    )
    str_help = "Link-State NLRI: Identifier Step"
    parser.add_argument(
        "-lsidstep",
        "--lsidstep",
        "--lsid-step",
        dest="lsidstep",
        default=1,
        type=int,
        help=str_help,
    )
    str_help = "Link-State NLRI: Policy Color Step"
    parser.add_argument(
        "--colorstep",
        "--color-step",
        "-lstidstep",
        dest="colorstep",
        default=1,
        type=int,
        help=str_help,
    )
    str_help = "Link-State NLRI: Discriminator Step"
    parser.add_argument(
        "--discriminatorstep",
        "--discriminator-step",
        "-lspidstep",
        dest="discriminatorstep",
        default=1,
        type=int,
        help=str_help,
    )
    str_help = "Link-State NLRI: Originator IP Address Step"
    parser.add_argument(
        "--originator-ip-step",
        "--originatoripstep",
        "-lstsaddrstep",
        dest="originator_ip_step",
        default=16,
        type=int,
        help=str_help,
    )
    str_help = "Link-State NLRI: Endpoint IP Address Step"
    parser.add_argument(
        "--endpoint-ip-step",
        "--endpointipstep",
        "-lsteaddrstep",
        dest="endpoint_ip_step",
        default=1,
        type=int,
        help=str_help,
    )
    str_help = "Link-State NLRI: Protocol-Origin (range 0-255)"
    parser.add_argument(
        "--proto-origin",
        dest="proto_origin",
        default=3,
        type=int,
        help=str_help,
    )
    str_help = "Link-State NLRI: Originator AS Number"
    parser.add_argument(
        "--originator-asn",
        dest="originator_asn",
        default=None,
        type=int,
        help=str_help,
    )
    str_help = "How many play utilities are to be started."
    parser.add_argument("--multiplicity", default="1", type=int, help=str_help)
    str_help = "Open message includes multiprotocol extension capability l2vpn-evpn.\
    Enabling this flag makes the script not decoding the update mesage, because of not\
    supported decoding for these elements."
    parser.add_argument("--evpn", default=False, action="store_true", help=str_help)
    str_help = "Open message includes Multicast in MPLS/BGP IP VPNs arguments.\
    Enabling this flag makes the script not decoding the update mesage, because of not\
    supported decoding for these elements."
    parser.add_argument("--grace", default="8", type=int, help=str_help)
    str_help = "Open message includes Graceful-restart capability, containing AFI/SAFIS:\
    IPV4-Unicast, IPV6-Unicast, BGP-LS\
    Enabling this flag makes the script not decoding the update mesage, because of not\
    supported decoding for these elements."
    parser.add_argument("--mvpn", default=False, action="store_true", help=str_help)
    str_help = "Open message includes L3VPN-MULTICAST arguments.\
    Enabling this flag makes the script not decoding the update mesage, because of not\
    supported decoding for these elements."
    parser.add_argument(
        "--l3vpn_mcast", default=False, action="store_true", help=str_help
    )
    str_help = (
        "Open message includes L3VPN-UNICAST arguments, without message decoding."
    )
    parser.add_argument("--l3vpn", default=False, action="store_true", help=str_help)
    str_help = "Open message includes ROUTE-TARGET-CONSTRAIN arguments, without message decoding."
    parser.add_argument(
        "--rt_constrain", default=False, action="store_true", help=str_help
    )
    str_help = "Open message includes ipv6-unicast family, without message decoding."
    parser.add_argument("--ipv6", default=False, action="store_true", help=str_help)
    str_help = (
        "Open message includes IPv4 BGP Labeled Unicast (BGP-LU) capability, "
        "without message decoding."
    )
    parser.add_argument("--bgplu", default=False, action="store_true", help=str_help)
    str_help = "Add all supported families without message decoding."
    parser.add_argument("--allf", default=False, action="store_true", help=str_help)
    parser.add_argument("--wfr", default=10, type=int, help="Wait for read timeout")
    str_help = "Skipping well known attributes for update message"
    parser.add_argument("--skipattr", default=False, action="store_true", help=str_help)

    # CLI updates for NLRI Type 6 / Type 5 extensions
    str_help = "Link-State NLRI Type: 5 (SR Policy) or 6 (SRv6 SID)"
    parser.add_argument(
        "--nlri-type",
        default=6,
        choices=[5, 6],
        type=int,
        help=str_help,
    )
    str_help = "Link-State NLRI: Protocol-ID (default: 9 for Type 5, 4 for Type 6)"
    parser.add_argument(
        "--protocol-id",
        default=None,
        type=int,
        help=str_help,
    )
    str_help = "SRv6 SID base IPv6 address"
    parser.add_argument(
        "--srv6-sid",
        default=ipaddr.IPv6Address("2001:db8::1"),
        type=ipaddr.IPv6Address,
        help=str_help,
    )
    str_help = "SRv6 SID step"
    parser.add_argument(
        "--srv6-sid-step",
        "--srv6-sidstep",
        dest="srv6_sid_step",
        default=1,
        type=int,
        help=str_help,
    )
    str_help = "SRv6 Endpoint Behavior"
    parser.add_argument(
        "--srv6-endpoint-behavior",
        default=1,
        type=int,
        help=str_help,
    )
    str_help = "SRv6 Algorithm"
    parser.add_argument(
        "--srv6-algorithm",
        default=0,
        type=int,
        help=str_help,
    )
    str_help = "SRv6 Locator Block length"
    parser.add_argument(
        "--srv6-locator-block-len",
        default=48,
        type=int,
        help=str_help,
    )
    str_help = "SRv6 Locator Node length"
    parser.add_argument(
        "--srv6-locator-node-len",
        default=16,
        type=int,
        help=str_help,
    )
    str_help = "SRv6 Function length"
    parser.add_argument(
        "--srv6-function-len",
        default=16,
        type=int,
        help=str_help,
    )
    str_help = "SRv6 Argument length"
    parser.add_argument(
        "--srv6-argument-len",
        default=0,
        type=int,
        help=str_help,
    )

    arguments = parser.parse_args()
    if arguments.multiplicity < 1:
        print("Multiplicity", arguments.multiplicity, "is not positive.")
        raise SystemExit(1)
    if arguments.originator_asn is None:
        arguments.originator_asn = arguments.asnumber
    if not (0 <= arguments.proto_origin <= 255):
        print("proto-origin", arguments.proto_origin, "is not in range 0-255.")
        raise SystemExit(1)

    arguments.color_step = arguments.colorstep
    arguments.discriminator_step = arguments.discriminatorstep
    arguments.lsid_step = arguments.lsidstep
    arguments.originator_ip_step = arguments.originator_ip_step
    arguments.endpoint_ip_step = arguments.endpoint_ip_step

    # Set default protocol_id based on nlri_type if not specified
    if arguments.protocol_id is None:
        arguments.protocol_id = 9 if arguments.nlri_type == 5 else 4

    # Length validation for Type 6
    if arguments.nlri_type == 6:
        total_len = (
            arguments.srv6_locator_block_len
            + arguments.srv6_locator_node_len
            + arguments.srv6_function_len
            + arguments.srv6_argument_len
        )
        if total_len > 128:
            print(
                f"Sum of SRv6 locator block, node, function, and argument lengths ({total_len}) exceeds 128."
            )
            raise SystemExit(1)

    return arguments


def establish_connection(arguments):
    """Establish connection to BGP peer.

    Arguments:
        :arguments: following command-line arguments are used
            - arguments.myip: local IP address
            - arguments.myport: local port
            - arguments.peerip: remote IP address
            - arguments.peerport: remote port
    Returns:
        :return: socket.
    """
    if arguments.listen:
        logger.info("Connecting in the listening mode.")
        logger.debug("Local IP address: " + str(arguments.myip))
        logger.debug("Local port: " + str(arguments.myport))
        listening_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        listening_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        listening_socket.bind((str(arguments.myip), arguments.myport))
        listening_socket.listen(1)
        bgp_socket, _ = listening_socket.accept()
        listening_socket.close()
    else:
        logger.info("Connecting in the talking mode.")
        logger.debug("Local IP address: " + str(arguments.myip))
        logger.debug("Local port: " + str(arguments.myport))
        logger.debug("Remote IP address: " + str(arguments.peerip))
        logger.debug("Remote port: " + str(arguments.peerport))
        talking_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        talking_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        talking_socket.bind((str(arguments.myip), arguments.myport))
        talking_socket.connect((str(arguments.peerip), arguments.peerport))
        bgp_socket = talking_socket
    logger.info("Connected to ODL.")
    return bgp_socket


def get_short_int_from_message(message, offset=16):
    """Extract 2-bytes number from provided message.

    Arguments:
        :message: given message
        :offset: offset of the short_int inside the message
    Returns:
        :return: required short_int value.
    Notes:
        default offset value is the BGP message size offset.
    """
    high_byte_int = message[offset]
    low_byte_int = message[offset + 1]
    short_int = high_byte_int * 256 + low_byte_int
    return short_int


def get_prefix_list_from_hex(prefixes_hex):
    """Get decoded list of prefixes (rfc4271#section-4.3)

    Arguments:
        :prefixes_hex: list of prefixes to be decoded in hex
    Returns:
        :return: list of prefixes in the form of ip address (X.X.X.X/X)
    """
    prefix_list = []
    offset = 0
    while offset < len(prefixes_hex):
        prefix_bit_len_hex = prefixes_hex[offset : offset + 1]
        prefix_bit_len = int(binascii.b2a_hex(prefix_bit_len_hex), 16)
        prefix_len = int((prefix_bit_len - 1) / 8) + 1
        prefix_hex = prefixes_hex[offset + 1 : offset + 1 + prefix_len]
        prefix = ".".join(str(i) for i in struct.unpack("BBBB", prefix_hex))
        offset += 1 + prefix_len
        prefix_list.append(prefix + "/" + str(prefix_bit_len))
    return prefix_list


class MessageError(ValueError):
    """Value error with logging optimized for hexlified messages."""

    def __init__(self, text, message, *args):
        """Initialisation.

        Store and call super init for textual comment,
        store raw message which caused it.
        """
        self.text = text
        self.msg = message
        super(MessageError, self).__init__(text, message, *args)

    def __str__(self):
        """Generate human readable error message.

        Returns:
            :return: human readable message as string
        Notes:
            Use a placeholder string if the message is to be empty.
        """
        message = binascii.hexlify(self.msg).decode()
        if message == "":
            message = "(empty message)"
        return self.text + ": " + message


def read_open_message(bgp_socket):
    """Receive peer's OPEN message

    Arguments:
        :bgp_socket: the socket to be read
    Returns:
        :return: received OPEN message.
    Notes:
        Performs just basic incoming message checks
    """
    msg_in = bgp_socket.recv(65535)
    if len(msg_in) < 37:
        error_msg = (
            "Message length (" + str(len(msg_in)) + ") is smaller than "
            "minimal length of OPEN message with 4-byte AS number (37)"
        )
        logger.error(error_msg + ": " + binascii.hexlify(msg_in).decode())
        raise MessageError(error_msg, msg_in)
    reported_length = get_short_int_from_message(msg_in)
    if len(msg_in) != reported_length:
        error_msg = (
            "Expected message length ("
            + str(reported_length)
            + ") does not match actual length ("
            + str(len(msg_in))
            + ")"
        )
        logger.error(error_msg + binascii.hexlify(msg_in).decode())
        raise MessageError(error_msg, msg_in)
    logger.info("Open message received.")
    return msg_in


class MessageGenerator(object):
    """Class which generates messages, holds states and configuration values."""

    def __init__(self, args):
        """Initialisation according to command-line args.

        Arguments:
            :args: argsparser's Namespace object which contains command-line
                options for MessageGenerator initialisation
        Notes:
            Calculates and stores default values used later on for
            message generation.
        """
        self.total_prefix_amount = args.amount
        self.remaining_prefixes = self.total_prefix_amount

        self.port = args.port
        self.iteration = 0
        self.prefix_base_default = args.firstprefix
        self.prefix_length_default = args.prefixlen
        self.wr_prefixes_default = []
        self.nlri_prefixes_default = []
        self.version_default = 4
        self.my_autonomous_system_default = args.asnumber
        self.hold_time_default = args.holdtime
        self.bgp_identifier_default = int(args.myip)
        self.next_hop_default = args.nexthop
        self.originator_id_default = args.originator
        self.cluster_list_item_default = args.cluster
        self.single_update_default = args.updates == "single"
        self.randomize_updates_default = args.updates == "random"
        self.prefix_count_to_add_default = args.insert
        self.prefix_count_to_del_default = args.withdraw
        if self.prefix_count_to_del_default < 0:
            self.prefix_count_to_del_default = 0
        if self.prefix_count_to_add_default <= self.prefix_count_to_del_default:
            self.prefix_count_to_add_default = self.prefix_count_to_del_default + 1
        self.slot_size_default = self.prefix_count_to_add_default
        self.remaining_prefixes_threshold = self.total_prefix_amount - args.prefill
        self.results_file_name_default = args.results
        self.performance_threshold_default = args.threshold
        self.rfc4760 = args.rfc4760
        self.bgpls = args.bgpls
        self.evpn = args.evpn
        self.mvpn = args.mvpn
        self.l3vpn_mcast = args.l3vpn_mcast
        self.l3vpn = args.l3vpn
        self.rt_constrain = args.rt_constrain
        self.ipv6 = args.ipv6
        self.bgplu = args.bgplu
        self.allf = args.allf
        self.skipattr = args.skipattr
        self.grace = args.grace

        # Default values when BGP-LS Attributes are used
        if self.bgpls:
            self.prefix_count_to_add_default = 1
            self.prefix_count_to_del_default = 0

        # Store NLRI type and protocol ID
        self.nlri_type = args.nlri_type
        self.protocol_id = args.protocol_id

        # Stepping steps
        self.lsid_step = getattr(args, "lsid_step", getattr(args, "lsidstep", 1))
        self.srv6_sid_step = getattr(args, "srv6_sid_step", 1)

        # Store base values
        self.base_identifier = args.lsid
        self.srv6_base_identifier = args.lsid
        self.base_sid = getattr(args, "srv6_sid", None)
        self.srv6_base_sid = getattr(args, "srv6_sid", None)

        if self.nlri_type == 6:
            self.srv6_nlri_default = {
                "Behavior": args.srv6_endpoint_behavior,
                "Algorithm": args.srv6_algorithm,
                "LB_len": args.srv6_locator_block_len,
                "LN_len": args.srv6_locator_node_len,
                "Fun_len": args.srv6_function_len,
                "Arg_len": args.srv6_argument_len,
                "NodeASN": args.asnumber,
                "BgpRouterID": int(args.myip),
            }
        else:
            originator_asn = (
                args.originator_asn
                if getattr(args, "originator_asn", None) is not None
                else args.asnumber
            )
            self.ls_nlri_default = {
                "Identifier": args.lsid,
                "ProtocolOrigin": args.proto_origin,
                "Endpoint": args.endpoint_ip,
                "Color": args.color,
                "OriginatorASN": originator_asn,
                "OriginatorAddress": args.originator_ip,
                "Discriminator": args.discriminator,
                "NodeASN": args.asnumber,
                "BgpRouterID": int(args.myip),
            }
            self.color_step = getattr(args, "color_step", getattr(args, "colorstep", 1))
            self.discriminator_step = getattr(
                args, "discriminator_step", getattr(args, "discriminatorstep", 1)
            )
            self.originator_ip_step = getattr(
                args, "originator_ip_step", getattr(args, "originatoripstep", 16)
            )
            self.endpoint_ip_step = getattr(
                args, "endpoint_ip_step", getattr(args, "endpointipstep", 1)
            )

        # Default values used for randomized part
        s1_slots = (
            int(
                (self.total_prefix_amount - self.remaining_prefixes_threshold - 1)
                / self.prefix_count_to_add_default
            )
            + 1
        )
        s2_slots = (
            int(
                (self.remaining_prefixes_threshold - 1)
                / (self.prefix_count_to_add_default - self.prefix_count_to_del_default)
            )
            + 1
        )
        s2_first_index = s1_slots * self.prefix_count_to_add_default
        s2_last_index = (
            s2_first_index
            + s2_slots
            * (self.prefix_count_to_add_default - self.prefix_count_to_del_default)
            - 1
        )
        self.slot_gap_default = (
            int(
                (self.total_prefix_amount - self.remaining_prefixes_threshold - 1)
                / self.prefix_count_to_add_default
            )
            + 1
        )
        self.randomize_lowest_default = s2_first_index
        self.randomize_highest_default = s2_last_index

        self.phase1_start_time = 0
        self.phase1_stop_time = 0
        self.phase2_start_time = 0
        self.phase2_stop_time = 0
        self.phase1_updates_sent = 0
        self.phase2_updates_sent = 0
        self.updates_sent = 0

        self.log_info = args.loglevel <= logging.INFO
        self.log_debug = args.loglevel <= logging.DEBUG

        logger.info("Generator initialisation")
        logger.info(
            "  Target total number of prefixes to be introduced: "
            + str(self.total_prefix_amount)
        )
        logger.info(
            "  Prefix base: "
            + str(self.prefix_base_default)
            + "/"
            + str(self.prefix_length_default)
        )
        logger.info(
            "  My Autonomous System number: " + str(self.my_autonomous_system_default)
        )
        logger.info("  My Hold Time: " + str(self.hold_time_default))
        logger.info("  My BGP Identifier: " + str(self.bgp_identifier_default))
        logger.info("  Next Hop: " + str(self.next_hop_default))
        logger.info("  Originator ID: " + str(self.originator_id_default))
        logger.info("  Cluster list: " + str(self.cluster_list_item_default))
        logger.info(
            "  Prefix count to be inserted at once: "
            + str(self.prefix_count_to_add_default)
        )
        logger.info(
            "  Prefix count to be withdrawn at once: "
            + str(self.prefix_count_to_del_default)
        )
        logger.info(
            "  Fast pre-fill up to "
            + str(self.total_prefix_amount - self.remaining_prefixes_threshold)
            + " prefixes"
        )
        logger.info(
            "  Remaining number of prefixes to be processed "
            + "in parallel with withdrawals: "
            + str(self.remaining_prefixes_threshold)
        )
        logger.debug(
            "  Prefix index range used after pre-fill procedure ["
            + str(self.randomize_lowest_default)
            + ", "
            + str(self.randomize_highest_default)
            + "]"
        )
        if self.single_update_default:
            logger.info(
                "  Common single UPDATE will be generated "
                + "for both NLRI & WITHDRAWN lists"
            )
        else:
            logger.info(
                "  Two separate UPDATEs will be generated "
                + "for each NLRI & WITHDRAWN lists"
            )
        if self.randomize_updates_default:
            logger.info("  Generation of UPDATE messages will be randomized")
        logger.info("  Let's go ...\n")

    def store_results(self, file_name=None, threshold=None):
        """Stores specified results into files based on file_name value.

        Arguments:
            :param file_name: Trailing (common) part of result file names
            :param threshold: Minimum number of sent updates needed for each
                              result to be included into result csv file
        Returns:
            :return: n/a
        """
        if file_name is None:
            file_name = self.results_file_name_default
        if threshold is None:
            threshold = self.performance_threshold_default
        if self.phase1_updates_sent >= threshold:
            totals1 = self.phase1_updates_sent
            performance1 = int(
                self.phase1_updates_sent
                / (self.phase1_stop_time - self.phase1_start_time)
            )
        else:
            totals1 = None
            performance1 = None
        if self.phase2_updates_sent >= threshold:
            totals2 = self.phase2_updates_sent
            performance2 = int(
                self.phase2_updates_sent
                / (self.phase2_stop_time - self.phase2_start_time)
            )
        else:
            totals2 = None
            performance2 = None

        logger.info("#" * 10 + " Final results " + "#" * 10)
        logger.info("Number of iterations: " + str(self.iteration))
        logger.info(
            "Number of UPDATE messages sent in the pre-fill phase: "
            + str(self.phase1_updates_sent)
        )
        logger.info(
            "The pre-fill phase duration: "
            + str(self.phase1_stop_time - self.phase1_start_time)
            + "s"
        )
        logger.info(
            "Number of UPDATE messages sent in the 2nd test phase: "
            + str(self.phase2_updates_sent)
        )
        logger.info(
            "The 2nd test phase duration: "
            + str(self.phase2_stop_time - self.phase2_start_time)
            + "s"
        )
        logger.info("Threshold for performance reporting: " + str(threshold))

        phase1_label = (
            "pre-fill " + str(self.prefix_count_to_add_default) + " route(s) per UPDATE"
        )
        if self.single_update_default:
            phase2_label = "+" + (
                str(self.prefix_count_to_add_default)
                + "/-"
                + str(self.prefix_count_to_del_default)
                + " routes per UPDATE"
            )
        else:
            phase2_label = "+" + (
                str(self.prefix_count_to_add_default)
                + "/-"
                + str(self.prefix_count_to_del_default)
                + " routes in two UPDATEs"
            )
        totals = {}
        performance = {}
        if totals1 is not None:
            totals[phase1_label] = totals1
            performance[phase1_label] = performance1
        if totals2 is not None:
            totals[phase2_label] = totals2
            performance[phase2_label] = performance2
        self.write_results_to_file(totals, "totals-" + file_name)
        self.write_results_to_file(performance, "performance-" + file_name)

    def write_results_to_file(self, results, file_name):
        """Writes results to the csv plot file consumable by Jenkins.

        Arguments:
            :param file_name: Name of the (csv) file to be created
        Returns:
            :return: none
        """
        first_line = ""
        second_line = ""
        f = open(file_name, "wt")
        try:
            for key in sorted(results):
                first_line += key + ", "
                second_line += str(results[key]) + ", "
            first_line = first_line[:-2]
            second_line = second_line[:-2]
            f.write(first_line + "\n")
            f.write(second_line + "\n")
            logger.info(
                "Message generator performance results stored in " + file_name + ":"
            )
            logger.info("  " + first_line)
            logger.info("  " + second_line)
        finally:
            f.close()

    def randomize_index(self, index, lowest=None, highest=None):
        """Calculates pseudo-randomized index from selected range.

        Arguments:
            :param index: input index
            :param lowest: the lowest index from the randomized area
            :param highest: the highest index from the randomized area
        Returns:
            :return: the (pseudo)randomized index
        """
        if lowest is None:
            lowest = self.randomize_lowest_default
        if highest is None:
            highest = self.randomize_highest_default
        if (index >= lowest) and (index <= highest):
            new_index = highest - (index - lowest)
        else:
            new_index = index
        return new_index

    def get_ls_nlri_values(self, index):
        """Generates LS-NLRI parameters for SR Policy Candidate Path (RFC 9857 / RFC 9552)
        or SRv6 SID (NLRI Type 6).

        Arguments:
            :param index: index (iteration)
        Returns:
            :return: dictionary of LS NLRI parameters and values
        """
        if self.nlri_type == 6:
            identifier = self.base_identifier + int(index / self.lsid_step)
            sid = ipaddr.IPv6Address(int(self.base_sid) + int(index / self.srv6_sid_step))
            srv6_nlri_values = self.srv6_nlri_default.copy()
            srv6_nlri_values["Identifier"] = identifier
            srv6_nlri_values["SID"] = sid
            return srv6_nlri_values

        identifier = self.ls_nlri_default["Identifier"] + int(
            index / self.lsid_step
        )
        color = self.ls_nlri_default["Color"] + int(
            index / self.color_step
        )
        discriminator = self.ls_nlri_default["Discriminator"] + int(
            index / self.discriminator_step
        )
        originator_address = self.ls_nlri_default["OriginatorAddress"] + (
            int(index / self.originator_ip_step) * 16
        )
        endpoint = self.ls_nlri_default["Endpoint"] + (
            int(index / self.endpoint_ip_step) * 1
        )
        ls_nlri_values = {
            "Identifier": identifier,
            "Color": color,
            "Discriminator": discriminator,
            "OriginatorAddress": originator_address,
            "Endpoint": endpoint,
            "ProtocolOrigin": self.ls_nlri_default["ProtocolOrigin"],
            "OriginatorASN": self.ls_nlri_default["OriginatorASN"],
            "NodeASN": self.ls_nlri_default["NodeASN"],
            "BgpRouterID": self.ls_nlri_default["BgpRouterID"],
        }
        return ls_nlri_values

    def get_prefix_list(
        self,
        slot_index,
        slot_size=None,
        prefix_base=None,
        prefix_len=None,
        prefix_count=None,
        randomize=None,
    ):
        """Generates list of IP address prefixes.

        Arguments:
            :param slot_index: index of group of prefix addresses
            :param slot_size: size of group of prefix addresses
                in [number of included prefixes]
            :param prefix_base: IP address of the first prefix
                (slot_index = 0, prefix_index = 0)
            :param prefix_len: length of the prefix in bits
                (the same as size of netmask)
            :param prefix_count: number of prefixes to be returned
                from the specified slot
        Returns:
            :return: list of generated IP address prefixes
        """
        if slot_size is None:
            slot_size = self.slot_size_default
        if prefix_base is None:
            prefix_base = self.prefix_base_default
        if prefix_len is None:
            prefix_len = self.prefix_length_default
        if prefix_count is None:
            prefix_count = slot_size
        if randomize is None:
            randomize = self.randomize_updates_default

        indexes = []
        prefixes = []
        prefix_gap = 2 ** (32 - prefix_len)
        for i in range(prefix_count):
            prefix_index = slot_index * slot_size + i
            if randomize:
                prefix_index = self.randomize_index(prefix_index)
            indexes.append(prefix_index)
            prefixes.append(prefix_base + prefix_index * prefix_gap)
        if self.log_debug:
            logger.debug("  Prefix slot index: " + str(slot_index))
            logger.debug("  Prefix slot size: " + str(slot_size))
            logger.debug("  Prefix count: " + str(prefix_count))
            logger.debug("  Prefix indexes: " + str(indexes))
            logger.debug("  Prefix list: " + str(prefixes))
        return prefixes

    def compose_update_message(
        self, prefix_count_to_add=None, prefix_count_to_del=None
    ):
        """Composes an UPDATE message

        Arguments:
            :param prefix_count_to_add: # of prefixes to put into NLRI list
            :param prefix_count_to_del: # of prefixes to put into WITHDRAWN list
        Returns:
            :return: encoded UPDATE message in HEX
        """
        if prefix_count_to_add is None:
            prefix_count_to_add = self.prefix_count_to_add_default
        if prefix_count_to_del is None:
            prefix_count_to_del = self.prefix_count_to_del_default

        if self.log_info and not (self.iteration % 1000):
            logger.info(
                "Iteration: "
                + str(self.iteration)
                + " - total remaining prefixes: "
                + str(self.remaining_prefixes)
            )
        if self.log_debug:
            logger.debug(
                "#" * 10 + " Iteration: " + str(self.iteration) + " " + "#" * 10
            )
            logger.debug("Remaining prefixes: " + str(self.remaining_prefixes))

        straightforward_scenario = (
            self.remaining_prefixes > self.remaining_prefixes_threshold
        )
        if straightforward_scenario:
            prefix_count_to_del = 0
            if self.log_debug:
                logger.debug("--- STRAIGHTFORWARD SCENARIO ---")
            if not self.phase1_start_time:
                self.phase1_start_time = time.time()
        else:
            if self.log_debug:
                logger.debug("--- COMBINED SCENARIO ---")
            if not self.phase2_start_time:
                self.phase2_start_time = time.time()

        prefix_count_to_add = prefix_count_to_del + min(
            prefix_count_to_add - prefix_count_to_del, self.remaining_prefixes
        )

        slot_index_to_add = self.iteration
        slot_index_to_del = slot_index_to_add - self.slot_gap_default

        if self.log_debug:
            logger.debug("Prefixes to be inserted in this iteration:")
        prefix_list_to_add = self.get_prefix_list(
            slot_index_to_add, prefix_count=prefix_count_to_add
        )

        if self.log_debug:
            logger.debug("Prefixes to be withdrawn in this iteration:")
        prefix_list_to_del = self.get_prefix_list(
            slot_index_to_del, prefix_count=prefix_count_to_del
        )

        if self.bgpls:
            ls_nlri = self.get_ls_nlri_values(self.iteration)
            msg_out = self.update_message(wr_prefixes=[], nlri_prefixes=[], **ls_nlri)
        else:
            if self.single_update_default:
                msg_out = self.update_message(
                    wr_prefixes=prefix_list_to_del, nlri_prefixes=prefix_list_to_add
                )
            else:
                msg_out = self.update_message(
                    wr_prefixes=[], nlri_prefixes=prefix_list_to_add
                )
                if prefix_count_to_del:
                    msg_out += self.update_message(
                        wr_prefixes=prefix_list_to_del, nlri_prefixes=[]
                    )

        if straightforward_scenario:
            self.phase1_stop_time = time.time()
            self.phase1_updates_sent = self.updates_sent
        else:
            self.phase2_stop_time = time.time()
            self.phase2_updates_sent = self.updates_sent - self.phase1_updates_sent

        self.iteration += 1
        self.remaining_prefixes -= prefix_count_to_add - prefix_count_to_del
        return msg_out

    def open_message(
        self,
        version=None,
        my_autonomous_system=None,
        hold_time=None,
        bgp_identifier=None,
    ):
        """Generates an OPEN Message (rfc4271#section-4.2)

        Arguments:
            :param version: see the rfc4271#section-4.2
            :param my_autonomous_system: see the rfc4271#section-4.2
            :param hold_time: see the rfc4271#section-4.2
            :param bgp_identifier: see the rfc4271#section-4.2
        Returns:
            :return: encoded OPEN message in HEX
        """
        if version is None:
            version = self.version_default
        if my_autonomous_system is None:
            my_autonomous_system = self.my_autonomous_system_default
        if hold_time is None:
            hold_time = self.hold_time_default
        if bgp_identifier is None:
            bgp_identifier = self.bgp_identifier_default

        marker_hex = b"\xff" * 16
        type = 1
        type_hex = struct.pack("B", type)
        version_hex = struct.pack("B", version)

        my_autonomous_system_2_bytes = 23456
        if my_autonomous_system < 65536:
            my_autonomous_system_2_bytes = my_autonomous_system
        my_autonomous_system_hex_2_bytes = struct.pack(">H", my_autonomous_system)

        hold_time_hex = struct.pack(">H", hold_time)
        bgp_identifier_hex = struct.pack(">I", bgp_identifier)

        optional_parameters_hex = b""
        if self.rfc4760 or self.allf:
            optional_parameter_hex = (
                b"\x02"
                b"\x06"
                b"\x01"
                b"\x04"
                b"\x00\x01"
                b"\x00"
                b"\x01"
            )
            optional_parameters_hex += optional_parameter_hex

        if self.ipv6 or self.allf:
            optional_parameter_hex = (
                b"\x02"
                b"\x06"
                b"\x01"
                b"\x04"
                b"\x00\x02"
                b"\x00"
                b"\x01"
            )
            optional_parameters_hex += optional_parameter_hex

        if self.bgpls or self.allf:
            optional_parameter_hex = (
                b"\x02"
                b"\x06"
                b"\x01"
                b"\x04"
                b"\x40\x04"
                b"\x00"
                b"\x47"
            )
            optional_parameters_hex += optional_parameter_hex

        if self.evpn or self.allf:
            optional_parameter_hex = (
                b"\x02"
                b"\x06"
                b"\x01"
                b"\x04"
                b"\x00\x19"
                b"\x00"
                b"\x46"
            )
            optional_parameters_hex += optional_parameter_hex

        if self.mvpn or self.allf:
            optional_parameter_hex = (
                b"\x02"
                b"\x06"
                b"\x01"
                b"\x04"
                b"\x00\x01"
                b"\x00"
                b"\x05"
            )
            optional_parameters_hex += optional_parameter_hex
            optional_parameter_hex = (
                b"\x02"
                b"\x06"
                b"\x01"
                b"\x04"
                b"\x00\x02"
                b"\x00"
                b"\x05"
            )
            optional_parameters_hex += optional_parameter_hex

        if self.l3vpn_mcast or self.allf:
            optional_parameter_hex = (
                b"\x02"
                b"\x06"
                b"\x01"
                b"\x04"
                b"\x00\x01"
                b"\x00"
                b"\x81"
            )
            optional_parameters_hex += optional_parameter_hex
            optional_parameter_hex = (
                b"\x02"
                b"\x06"
                b"\x01"
                b"\x04"
                b"\x00\x02"
                b"\x00"
                b"\x81"
            )
            optional_parameters_hex += optional_parameter_hex

        if self.l3vpn or self.allf:
            optional_parameter_hex = (
                b"\x02"
                b"\x06"
                b"\x01"
                b"\x04"
                b"\x00\x01"
                b"\x00"
                b"\x80"
            )
            optional_parameters_hex += optional_parameter_hex
            optional_parameter_hex = (
                b"\x02"
                b"\x06"
                b"\x01"
                b"\x04"
                b"\x00\x02"
                b"\x00"
                b"\x80"
            )
            optional_parameters_hex += optional_parameter_hex

        if self.rt_constrain or self.allf:
            optional_parameter_hex = (
                b"\x02"
                b"\x06"
                b"\x01"
                b"\x04"
                b"\x00\x01"
                b"\x00"
                b"\x84"
            )
            optional_parameters_hex += optional_parameter_hex

        if self.bgplu or self.allf:
            optional_parameter_hex = (
                b"\x02"
                b"\x06"
                b"\x01"
                b"\x04"
                b"\x00\x01"
                b"\x00"
                b"\x04"
            )
            optional_parameters_hex += optional_parameter_hex

        optional_parameter_hex = (
            b"\x02"
            b"\x06"
            b"\x41"
            b"\x04"
        )
        optional_parameter_hex += struct.pack(
            ">I", my_autonomous_system
        )
        optional_parameters_hex += optional_parameter_hex

        if self.grace != 8:
            b = list(bin(self.grace)[2:])
            b = b + [0] * (3 - len(b))
            length = b"\x08"
            if b[1] == "1":
                restart_flag = b"\x80\x05"
            else:
                restart_flag = b"\x00\x05"
            if b[2] == "1":
                ipv4_flag = b"\x80"
            else:
                ipv4_flag = b"\x00"
            if b[0] == "1":
                ll_gr = b"\x47\x07\x00\x01\x01\x00\x00\x00\x1e"
                length = b"\x11"
            else:
                ll_gr = b""
            logger.debug("Grace parameters list: {}".format(b))
            optional_parameter_hex = (
                f"\x02{length}\x40\x06{restart_flag}\x00\x01\x01{ipv4_flag}{ll_gr}"
            )
            optional_parameters_hex += optional_parameter_hex

        optional_parameters_length = len(optional_parameters_hex)
        optional_parameters_length_hex = struct.pack("B", optional_parameters_length)

        length = (
            len(marker_hex)
            + 2
            + len(type_hex)
            + len(version_hex)
            + len(my_autonomous_system_hex_2_bytes)
            + len(hold_time_hex)
            + len(bgp_identifier_hex)
            + len(optional_parameters_length_hex)
            + len(optional_parameters_hex)
        )
        length_hex = struct.pack(">H", length)

        message_hex = (
            marker_hex
            + length_hex
            + type_hex
            + version_hex
            + my_autonomous_system_hex_2_bytes
            + hold_time_hex
            + bgp_identifier_hex
            + optional_parameters_length_hex
            + optional_parameters_hex
        )

        if self.log_debug:
            logger.debug("OPEN message encoding")
            logger.debug("  Marker=0x" + binascii.hexlify(marker_hex).decode())
            logger.debug(
                "  Length="
                + str(length)
                + " (0x"
                + binascii.hexlify(length_hex).decode()
                + ")"
            )
            logger.debug(
                "  Type="
                + str(type)
                + " (0x"
                + binascii.hexlify(type_hex).decode()
                + ")"
            )
            logger.debug(
                "  Version="
                + str(version)
                + " (0x"
                + binascii.hexlify(version_hex).decode()
                + ")"
            )
            logger.debug(
                "  My Autonomous System="
                + str(my_autonomous_system_2_bytes)
                + " (0x"
                + binascii.hexlify(my_autonomous_system_hex_2_bytes).decode()
                + ")"
            )
            logger.debug(
                "  Hold Time="
                + str(hold_time)
                + " (0x"
                + binascii.hexlify(hold_time_hex).decode()
                + ")"
            )
            logger.debug(
                "  BGP Identifier="
                + str(bgp_identifier)
                + " (0x"
                + binascii.hexlify(bgp_identifier_hex).decode()
                + ")"
            )
            logger.debug(
                "  Optional Parameters Length="
                + str(optional_parameters_length)
                + " (0x"
                + binascii.hexlify(optional_parameters_length_hex).decode()
                + ")"
            )
            logger.debug(
                "  Optional Parameters=0x"
                + binascii.hexlify(optional_parameters_hex).decode()
            )
            logger.debug("OPEN message encoded: 0x%s", binascii.b2a_hex(message_hex))

        return message_hex

    def update_message(
        self,
        wr_prefixes=None,
        nlri_prefixes=None,
        wr_prefix_length=None,
        nlri_prefix_length=None,
        my_autonomous_system=None,
        next_hop=None,
        originator_id=None,
        cluster_list_item=None,
        end_of_rib=False,
        **ls_nlri_params,
    ):
        """Generates an UPDATE Message (rfc4271#section-4.3)

        Arguments:
            :param wr_prefixes: see the rfc4271#section-4.3
            :param nlri_prefixes: see the rfc4271#section-4.3
            :param wr_prefix_length: see the rfc4271#section-4.3
            :param nlri_prefix_length: see the rfc4271#section-4.3
            :param my_autonomous_system: see the rfc4271#section-4.3
            :param next_hop: see the rfc4271#section-4.3
        Returns:
            :return: encoded UPDATE message in HEX
        """
        if wr_prefixes is None:
            wr_prefixes = self.wr_prefixes_default
        if nlri_prefixes is None:
            nlri_prefixes = self.nlri_prefixes_default
        if wr_prefix_length is None:
            wr_prefix_length = self.prefix_length_default
        if nlri_prefix_length is None:
            nlri_prefix_length = self.prefix_length_default
        if my_autonomous_system is None:
            my_autonomous_system = self.my_autonomous_system_default
        if next_hop is None:
            next_hop = self.next_hop_default
        if originator_id is None:
            originator_id = self.originator_id_default
        if cluster_list_item is None:
            cluster_list_item = self.cluster_list_item_default

        if self.nlri_type == 6:
            ls_nlri = self.srv6_nlri_default.copy()
            ls_nlri["Identifier"] = self.base_identifier
            ls_nlri["SID"] = self.base_sid
        else:
            ls_nlri = self.ls_nlri_default.copy()
        ls_nlri.update(ls_nlri_params)

        marker_hex = b"\xff" * 16
        type = 2
        type_hex = struct.pack("B", type)

        withdrawn_routes_hex = b""
        if not self.bgpls:
            bytes_count = int((wr_prefix_length - 1) / 8) + 1
            for prefix in wr_prefixes:
                withdrawn_route_hex = (
                    struct.pack("B", wr_prefix_length)
                    + struct.pack(">I", int(prefix))[:bytes_count]
                )
                withdrawn_routes_hex += withdrawn_route_hex

        withdrawn_routes_length = len(withdrawn_routes_hex)
        withdrawn_routes_length_hex = struct.pack(">H", withdrawn_routes_length)

        path_attributes_hex = b""
        if not self.skipattr:
            path_attributes_hex += (
                b"\x40"
                b"\x01"
                b"\x01"
                b"\x00"
            )
            path_attributes_hex += (
                b"\x40"
                b"\x02"
                b"\x06"
                b"\x02"
                b"\x01"
            )
            my_as_hex = struct.pack(">I", my_autonomous_system)
            path_attributes_hex += my_as_hex
            path_attributes_hex += (
                b"\x40"
                b"\x05"
                b"\x04"
                b"\x00\x00\x00\x64"
            )
        if nlri_prefixes != []:
            path_attributes_hex += (
                b"\x40"
                b"\x03"
                b"\x04"
            )
            next_hop_hex = struct.pack(">I", int(next_hop))
            path_attributes_hex += next_hop_hex
            if originator_id is not None:
                path_attributes_hex += (
                    b"\x80"
                    b"\x09"
                    b"\x04"
                )
                path_attributes_hex += struct.pack(">I", int(originator_id))
            if cluster_list_item is not None:
                path_attributes_hex += (
                    b"\x80"
                    b"\x0a"
                    b"\x04"
                )
                path_attributes_hex += struct.pack(">I", int(cluster_list_item))

        if self.bgpls and not end_of_rib:
            if self.nlri_type == 5:
                # Branch A (Type 5): Preserved play2.py logic verbatim
                # Sub-TLVs for Local Node Descriptors TLV (TLV 256)
                sub_tlv_512 = struct.pack(">HH", 512, 4) + struct.pack(
                    ">I", int(ls_nlri["NodeASN"])
                )
                sub_tlv_516 = struct.pack(">HH", 516, 4) + struct.pack(
                    ">I", int(ls_nlri["BgpRouterID"])
                )
                sub_tlv_1028 = struct.pack(">HH", 1028, 4) + struct.pack(
                    ">I", int(ls_nlri["OriginatorAddress"])
                )
                tlv_256_val = sub_tlv_512 + sub_tlv_516 + sub_tlv_1028
                tlv_256 = struct.pack(">HH", 256, len(tlv_256_val)) + tlv_256_val

                # SR Policy Candidate Path Descriptor TLV (TLV 554)
                tlv_554_val = (
                    struct.pack("B", int(ls_nlri["ProtocolOrigin"]))
                    + b"\x00"
                    + b"\x00\x00"
                    + struct.pack(">I", int(ls_nlri["Endpoint"]))
                    + struct.pack(">I", int(ls_nlri["Color"]))
                    + struct.pack(">I", int(ls_nlri["OriginatorASN"]))
                    + struct.pack(">I", int(ls_nlri["OriginatorAddress"]))
                    + struct.pack(">I", int(ls_nlri["Discriminator"]))
                )
                tlv_554 = struct.pack(">HH", 554, len(tlv_554_val)) + tlv_554_val

                # Link-State NLRI Framing (Type 5)
                nlri_content = (
                    struct.pack("B", self.protocol_id)
                    + struct.pack(">Q", int(ls_nlri["Identifier"]))
                    + tlv_256
                    + tlv_554
                )
                nlri = struct.pack(">HH", 5, len(nlri_content)) + nlri_content

                # MP_REACH_NLRI attribute content
                mp_reach_content = (
                    b"\x40\x04"
                    b"\x47"
                    b"\x04"
                    + struct.pack(">I", int(next_hop))
                    + b"\x00"
                    + nlri
                )

                path_attributes_hex += (
                    b"\x80"
                    b"\x0e"
                    + struct.pack("B", len(mp_reach_content))
                    + mp_reach_content
                )

            elif self.nlri_type == 6:
                # Branch B (Type 6): SRv6 SID
                # 1. TLV 256 (Local Node Descriptors): Sub-TLV 512 + Sub-TLV 516 (no Sub-TLV 1028)
                sub_tlv_512 = struct.pack(">HH", 512, 4) + struct.pack(
                    ">I", int(ls_nlri["NodeASN"])
                )
                sub_tlv_516 = struct.pack(">HH", 516, 4) + struct.pack(
                    ">I", int(ls_nlri["BgpRouterID"])
                )
                tlv_256_val = sub_tlv_512 + sub_tlv_516
                tlv_256 = struct.pack(">HH", 256, len(tlv_256_val)) + tlv_256_val

                # 2. TLV 518 (SRv6 SID Info): 16-byte IPv6 SID
                tlv_518_val = int(ls_nlri["SID"]).to_bytes(16, "big")
                tlv_518 = struct.pack(">HH", 518, len(tlv_518_val)) + tlv_518_val

                # 3. Link-State NLRI Framing: Protocol-ID + Identifier + TLV 256 + TLV 518
                nlri_content = (
                    struct.pack("B", self.protocol_id)
                    + struct.pack(">Q", int(ls_nlri["Identifier"]))
                    + tlv_256
                    + tlv_518
                )
                nlri = struct.pack(">HH", 6, len(nlri_content)) + nlri_content

                # 4. MP_REACH_NLRI (Attribute 14)
                mp_reach_content = (
                    b"\x40\x04"  # AFI (BGP-LS = 16388)
                    b"\x47"  # SAFI (BGP-LS = 71)
                    b"\x04"  # Length of Next Hop Network Address (4 octets)
                    + struct.pack(">I", int(next_hop))  # Next Hop IPv4 Address
                    + b"\x00"  # Reserved
                    + nlri  # 53 octets (4-octet header + 49-octet content)
                )

                path_attributes_hex += (
                    b"\x80"
                    b"\x0e"
                    + struct.pack("B", len(mp_reach_content))
                    + mp_reach_content
                )

                # 5. BGP-LS Attribute (Attribute 29)
                # TLV 1250 (Endpoint Behavior)
                tlv_1250_val = (
                    struct.pack(">H", int(ls_nlri["Behavior"]))
                    + b"\x00"
                    + struct.pack("B", int(ls_nlri["Algorithm"]))
                )
                tlv_1250 = struct.pack(">HH", 1250, len(tlv_1250_val)) + tlv_1250_val

                # TLV 1252 (SID Structure)
                tlv_1252_val = struct.pack(
                    "BBBB",
                    int(ls_nlri["LB_len"]),
                    int(ls_nlri["LN_len"]),
                    int(ls_nlri["Fun_len"]),
                    int(ls_nlri["Arg_len"]),
                )
                tlv_1252 = struct.pack(">HH", 1252, len(tlv_1252_val)) + tlv_1252_val

                # Wrap both TLVs in Attribute 29 header
                attr_29_content = tlv_1250 + tlv_1252
                path_attributes_hex += (
                    b"\x80"
                    b"\x1d"
                    + struct.pack("B", len(attr_29_content))
                    + attr_29_content
                )

        total_path_attributes_length = len(path_attributes_hex)
        total_path_attributes_length_hex = struct.pack(
            ">H", total_path_attributes_length
        )

        nlri_hex = b""
        if not self.bgpls:
            bytes_count = int((nlri_prefix_length - 1) / 8) + 1
            for prefix in nlri_prefixes:
                nlri_prefix_hex = (
                    struct.pack("B", nlri_prefix_length)
                    + struct.pack(">I", int(prefix))[:bytes_count]
                )
                nlri_hex += nlri_prefix_hex

        length = (
            len(marker_hex)
            + 2
            + len(type_hex)
            + len(withdrawn_routes_length_hex)
            + len(withdrawn_routes_hex)
            + len(total_path_attributes_length_hex)
            + len(path_attributes_hex)
            + len(nlri_hex)
        )
        length_hex = struct.pack(">H", length)

        message_hex = (
            marker_hex
            + length_hex
            + type_hex
            + withdrawn_routes_length_hex
            + withdrawn_routes_hex
            + total_path_attributes_length_hex
            + path_attributes_hex
            + nlri_hex
        )

        if self.grace != 8 and self.grace != 0 and end_of_rib:
            message_hex = marker_hex + binascii.unhexlify("00170200000000")

        if self.log_debug:
            logger.debug("UPDATE message encoding")
            logger.debug("  Marker=0x" + binascii.hexlify(marker_hex).decode())
            logger.debug(
                "  Length="
                + str(length)
                + " (0x"
                + binascii.hexlify(length_hex).decode()
                + ")"
            )
            logger.debug(
                "  Type="
                + str(type)
                + " (0x"
                + binascii.hexlify(type_hex).decode()
                + ")"
            )
            logger.debug(
                "  withdrawn_routes_length="
                + str(withdrawn_routes_length)
                + " (0x"
                + binascii.hexlify(withdrawn_routes_length_hex).decode()
                + ")"
            )
            logger.debug(
                "  Withdrawn_Routes="
                + str(wr_prefixes)
                + "/"
                + str(wr_prefix_length)
                + " (0x"
                + binascii.hexlify(withdrawn_routes_hex).decode()
                + ")"
            )
            if total_path_attributes_length:
                logger.debug(
                    "  Total Path Attributes Length="
                    + str(total_path_attributes_length)
                    + " (0x"
                    + binascii.hexlify(total_path_attributes_length_hex).decode()
                    + ")"
                )
                logger.debug(
                    "  Path Attributes="
                    + "(0x"
                    + binascii.hexlify(path_attributes_hex).decode()
                    + ")"
                )
                logger.debug("    Origin=IGP")
                logger.debug("    AS path=" + str(my_autonomous_system))
                logger.debug("    Next hop=" + str(next_hop))
                if originator_id is not None:
                    logger.debug("    Originator id=" + str(originator_id))
                if cluster_list_item is not None:
                    logger.debug("    Cluster list=" + str(cluster_list_item))
                if self.bgpls:
                    if self.nlri_type == 6:
                        logger.debug(
                            "    MP_REACH_NLRI: Identifier: %s, SID: %s",
                            ls_nlri.get("Identifier"),
                            ls_nlri.get("SID"),
                        )
                    else:
                        logger.debug("    MP_REACH_NLRI: %s", ls_nlri)
            logger.debug(
                "  Network Layer Reachability Information="
                + str(nlri_prefixes)
                + "/"
                + str(nlri_prefix_length)
                + " (0x"
                + binascii.hexlify(nlri_hex).decode()
                + ")"
            )
            logger.debug(
                "UPDATE message encoded: 0x" + binascii.b2a_hex(message_hex).decode()
            )

        self.updates_sent += 1
        return message_hex

    def notification_message(self, error_code, error_subcode, data_hex=b""):
        """Generates a NOTIFICATION Message (rfc4271#section-4.5)

        Arguments:
            :param error_code: see the rfc4271#section-4.5
            :param error_subcode: see the rfc4271#section-4.5
            :param data_hex: see the rfc4271#section-4.5
        Returns:
            :return: encoded NOTIFICATION message in HEX
        """
        marker_hex = b"\xff" * 16
        type = 3
        type_hex = struct.pack("B", type)
        error_code_hex = struct.pack("B", error_code)
        error_subcode_hex = struct.pack("B", error_subcode)

        length = (
            len(marker_hex)
            + 2
            + len(type_hex)
            + len(error_code_hex)
            + len(error_subcode_hex)
            + len(data_hex)
        )
        length_hex = struct.pack(">H", length)

        message_hex = (
            marker_hex
            + length_hex
            + type_hex
            + error_code_hex
            + error_subcode_hex
            + data_hex
        )

        if self.log_debug:
            logger.debug("NOTIFICATION message encoding")
            logger.debug("  Marker=0x" + binascii.hexlify(marker_hex).decode())
            logger.debug(
                "  Length="
                + str(length)
                + " (0x"
                + binascii.hexlify(length_hex).decode()
                + ")"
            )
            logger.debug(
                "  Type="
                + str(type)
                + " (0x"
                + binascii.hexlify(type_hex).decode()
                + ")"
            )
            logger.debug(
                "  Error Code="
                + str(error_code)
                + " (0x"
                + binascii.hexlify(error_code_hex).decode()
                + ")"
            )
            logger.debug(
                "  Error Subcode="
                + str(error_subcode)
                + " (0x"
                + binascii.hexlify(error_subcode_hex).decode()
                + ")"
            )
            logger.debug("  Data=" + " (0x" + binascii.hexlify(data_hex).decode() + ")")
            logger.debug(
                "NOTIFICATION message encoded: 0x%s", binascii.b2a_hex(message_hex)
            )

        return message_hex

    def keepalive_message(self):
        """Generates a KEEP ALIVE Message (rfc4271#section-4.4)

        Returns:
            :return: encoded KEEP ALIVE message in HEX
        """
        marker_hex = b"\xff" * 16
        type = 4
        type_hex = struct.pack("B", type)
        length = len(marker_hex) + 2 + len(type_hex)
        length_hex = struct.pack(">H", length)
        message_hex = marker_hex + length_hex + type_hex

        if self.log_debug:
            logger.debug("KEEP ALIVE message encoding")
            logger.debug("  Marker=0x" + binascii.hexlify(marker_hex).decode())
            logger.debug(
                "  Length="
                + str(length)
                + " (0x"
                + binascii.hexlify(length_hex).decode()
                + ")"
            )
            logger.debug(
                "  Type="
                + str(type)
                + " (0x"
                + binascii.hexlify(type_hex).decode()
                + ")"
            )
            logger.debug(
                "KEEP ALIVE message encoded: 0x%s",
                binascii.b2a_hex(message_hex).decode(),
            )

        return message_hex


class TimeTracker(object):
    """Class for tracking timers, both for my keepalives and peer's hold time."""

    def __init__(self, msg_in):
        """Initialisation based on defaults and OPEN message from peer.

        Arguments:
            msg_in: the OPEN message received from peer.
        """
        self.report_timedelta = 1.0
        hold_timedelta = 180
        peer_hold_timedelta = get_short_int_from_message(msg_in, offset=22)
        if hold_timedelta > peer_hold_timedelta:
            hold_timedelta = peer_hold_timedelta
        if hold_timedelta != 0 and hold_timedelta < 3:
            logger.error("Invalid hold timedelta value: " + str(hold_timedelta))
            raise ValueError("Invalid hold timedelta value: ", hold_timedelta)
        self.hold_timedelta = hold_timedelta
        self.keepalive_timedelta = int(hold_timedelta / 3.0)
        self.snapshot_time = time.time()
        self.peer_hold_time = self.snapshot_time + self.hold_timedelta
        self.my_keepalive_time = None

    def snapshot(self):
        """Store current time in instance data to use later."""
        self.snapshot_time = time.time()

    def reset_peer_hold_time(self):
        """Move hold time to future as peer has just proven it still lives."""
        self.peer_hold_time = time.time() + self.hold_timedelta

    def reset_my_keepalive_time(self, keepalive_time):
        """Calculate and set the next my KEEP ALIVE timeout time

        Arguments:
            :keepalive_time: the initial value of the KEEP ALIVE timer
        """
        self.my_keepalive_time = keepalive_time + self.keepalive_timedelta

    def is_time_for_my_keepalive(self):
        """Check for my KEEP ALIVE timeout occurrence"""
        if self.hold_timedelta == 0:
            return False
        return self.snapshot_time >= self.my_keepalive_time

    def get_next_event_time(self):
        """Set the time of the next expected or to be sent KEEP ALIVE"""
        if self.hold_timedelta == 0:
            return self.snapshot_time + 86400
        return min(self.my_keepalive_time, self.peer_hold_time)

    def check_peer_hold_time(self, snapshot_time):
        """Raise error if nothing was read from peer until specified time."""
        if self.hold_timedelta != 0:
            if snapshot_time > self.peer_hold_time:
                logger.error("Peer has overstepped the hold timer.")
                raise RuntimeError("Peer has overstepped the hold timer.")


class ReadTracker(object):
    """Class for tracking read of messages chunk by chunk and for idle waiting."""

    def __init__(
        self,
        bgp_socket,
        timer,
        storage,
        evpn=False,
        mvpn=False,
        l3vpn_mcast=False,
        allf=False,
        l3vpn=False,
        rt_constrain=False,
        ipv6=False,
        bgplu=False,
        grace=8,
        wait_for_read=10,
    ):
        """The reader initialisation.

        Arguments:
            bgp_socket: socket to be used for sending
            timer: timer to be used for scheduling
            storage: thread safe dict
            evpn: flag that evpn functionality is tested
            mvpn: flag that mvpn functionality is tested
            grace: flag that grace-restart functionality is tested
            l3vpn_mcast: flag that l3vpn_mcast functionality is tested
            l3vpn: flag that l3vpn unicast functionality is tested
            rt_constrain: flag that rt-constrain functionality is tested
            bgplu: flag that BGP-LU functionality is tested
            allf: flag for all family testing.
        """
        self.socket = bgp_socket
        self.timer = timer
        self.header_length = 18
        self.reading_header = True
        self.bytes_to_read = self.header_length
        self.msg_in = b""
        self.updates_received = 0
        self.prefixes_introduced = 0
        self.prefixes_withdrawn = 0
        self.rx_idle_time = 0
        self.rx_activity_detected = True
        self.storage = storage
        self.evpn = evpn
        self.mvpn = mvpn
        self.l3vpn_mcast = l3vpn_mcast
        self.l3vpn = l3vpn
        self.rt_constrain = rt_constrain
        self.ipv6 = ipv6
        self.bgplu = bgplu
        self.allf = allf
        self.wfr = wait_for_read
        self.grace = grace

    def read_message_chunk(self):
        """Read up to one message"""
        chunk_message = self.socket.recv(self.bytes_to_read)
        self.msg_in += chunk_message
        self.bytes_to_read -= len(chunk_message)
        if not self.bytes_to_read:
            if self.reading_header:
                self.reading_header = False
                self.bytes_to_read = (
                    get_short_int_from_message(self.msg_in) - self.header_length
                )
            else:
                self.timer.reset_peer_hold_time()
                message_type_hex = self.msg_in[
                    self.header_length : self.header_length + 1
                ]
                if message_type_hex == b"\x01":
                    logger.info(
                        "OPEN message received: 0x%s",
                        binascii.b2a_hex(self.msg_in).decode(),
                    )
                elif message_type_hex == b"\x02":
                    logger.debug(
                        "UPDATE message received: 0x%s",
                        binascii.b2a_hex(self.msg_in).decode(),
                    )
                    self.decode_update_message(self.msg_in)
                elif message_type_hex == b"\x03":
                    logger.info(
                        "NOTIFICATION message received: 0x%s",
                        binascii.b2a_hex(self.msg_in).decode(),
                    )
                elif message_type_hex == b"\x04":
                    logger.info(
                        "KEEP ALIVE message received: 0x%s",
                        binascii.b2a_hex(self.msg_in).decode(),
                    )
                else:
                    logger.warning(
                        "Unexpected message received: 0x%s",
                        binascii.b2a_hex(self.msg_in).decode(),
                    )
                self.msg_in = b""
                self.reading_header = True
                self.bytes_to_read = self.header_length
        return

    def decode_path_attributes(self, path_attributes_hex):
        """Decode the Path Attributes field (rfc4271#section-4.3)

        Arguments:
            :path_attributes_hex: path_attributes field to be decoded in hex
        Returns:
            :return: None
        """
        hex_to_decode = path_attributes_hex

        while len(hex_to_decode):
            attr_flags_hex = hex_to_decode[0:1]
            attr_flags = int(binascii.b2a_hex(attr_flags_hex), 16)
            attr_extended_length_bit = attr_flags & 16

            attr_type_code_hex = hex_to_decode[1:2]
            attr_type_code = int(binascii.b2a_hex(attr_type_code_hex), 16)

            if attr_extended_length_bit:
                attr_length_hex = hex_to_decode[2:4]
                attr_length = int(binascii.b2a_hex(attr_length_hex), 16)
                attr_value_hex = hex_to_decode[4 : 4 + attr_length]
                hex_to_decode = hex_to_decode[4 + attr_length :]
            else:
                attr_length_hex = hex_to_decode[2:3]
                attr_length = int(binascii.b2a_hex(attr_length_hex), 16)
                attr_value_hex = hex_to_decode[3 : 3 + attr_length]
                hex_to_decode = hex_to_decode[3 + attr_length :]

            if attr_type_code == 1:
                logger.debug(
                    "Attribute type=1 (ORIGIN, flags:0x%s)",
                    binascii.b2a_hex(attr_flags_hex),
                )
                logger.debug("Attribute value=0x%s", binascii.b2a_hex(attr_value_hex))
            elif attr_type_code == 2:
                logger.debug(
                    "Attribute type=2 (AS_PATH, flags:0x%s)",
                    binascii.b2a_hex(attr_flags_hex),
                )
                logger.debug("Attribute value=0x%s", binascii.b2a_hex(attr_value_hex))
            elif attr_type_code == 3:
                logger.debug(
                    "Attribute type=3 (NEXT_HOP, flags:0x%s)",
                    binascii.b2a_hex(attr_flags_hex),
                )
                logger.debug("Attribute value=0x%s", binascii.b2a_hex(attr_value_hex))
            elif attr_type_code == 4:
                logger.debug(
                    "Attribute type=4 (MULTI_EXIT_DISC, flags:0x%s)",
                    binascii.b2a_hex(attr_flags_hex),
                )
                logger.debug("Attribute value=0x%s", binascii.b2a_hex(attr_value_hex))
            elif attr_type_code == 5:
                logger.debug(
                    "Attribute type=5 (LOCAL_PREF, flags:0x%s)",
                    binascii.b2a_hex(attr_flags_hex),
                )
                logger.debug("Attribute value=0x%s", binascii.b2a_hex(attr_value_hex))
            elif attr_type_code == 6:
                logger.debug(
                    "Attribute type=6 (ATOMIC_AGGREGATE, flags:0x%s)",
                    binascii.b2a_hex(attr_flags_hex),
                )
                logger.debug("Attribute value=0x%s", binascii.b2a_hex(attr_value_hex))
            elif attr_type_code == 7:
                logger.debug(
                    "Attribute type=7 (AGGREGATOR, flags:0x%s)",
                    binascii.b2a_hex(attr_flags_hex),
                )
                logger.debug("Attribute value=0x%s", binascii.b2a_hex(attr_value_hex))
            elif attr_type_code == 9:
                logger.debug(
                    "Attribute type=9 (ORIGINATOR_ID, flags:0x%s)",
                    binascii.b2a_hex(attr_flags_hex),
                )
                logger.debug("Attribute value=0x%s", binascii.b2a_hex(attr_value_hex))
            elif attr_type_code == 10:
                logger.debug(
                    "Attribute type=10 (CLUSTER_LIST, flags:0x%s)",
                    binascii.b2a_hex(attr_flags_hex),
                )
                logger.debug("Attribute value=0x%s", binascii.b2a_hex(attr_value_hex))
            elif attr_type_code == 14:
                logger.debug(
                    "Attribute type=14 (MP_REACH_NLRI, flags:0x%s)",
                    binascii.b2a_hex(attr_flags_hex),
                )
                logger.debug("Attribute value=0x%s", binascii.b2a_hex(attr_value_hex))
                address_family_identifier_hex = attr_value_hex[0:2]
                logger.debug(
                    "  Address Family Identifier=0x%s",
                    binascii.b2a_hex(address_family_identifier_hex),
                )
                subsequent_address_family_identifier_hex = attr_value_hex[2:3]
                logger.debug(
                    "  Subsequent Address Family Identifier=0x%s",
                    binascii.b2a_hex(subsequent_address_family_identifier_hex),
                )
                next_hop_netaddr_len_hex = attr_value_hex[3:4]
                next_hop_netaddr_len = int(
                    binascii.b2a_hex(next_hop_netaddr_len_hex), 16
                )
                logger.debug(
                    "  Length of Next Hop Network Address=%s (0x%s)",
                    next_hop_netaddr_len,
                    binascii.b2a_hex(next_hop_netaddr_len_hex),
                )
                next_hop_netaddr_hex = attr_value_hex[4 : 4 + next_hop_netaddr_len]
                next_hop_netaddr = ".".join(
                    str(i) for i in struct.unpack("BBBB", next_hop_netaddr_hex)
                )
                logger.debug(
                    "  Network Address of Next Hop=%s (0x%s)",
                    next_hop_netaddr,
                    binascii.b2a_hex(next_hop_netaddr_hex),
                )
                reserved_byte_pos = 4 + next_hop_netaddr_len
                reserved_hex = attr_value_hex[reserved_byte_pos : reserved_byte_pos + 1]
                logger.debug("  Reserved=0x%s", binascii.b2a_hex(reserved_hex))
                nlri_hex = attr_value_hex[4 + next_hop_netaddr_len + 1 :]
                logger.debug(
                    "  Network Layer Reachability Information=0x%s",
                    binascii.b2a_hex(nlri_hex),
                )
                nlri_prefix_list = get_prefix_list_from_hex(nlri_hex)
                logger.debug("  NLRI prefix list: %s", nlri_prefix_list)
                for prefix in nlri_prefix_list:
                    logger.debug("  nlri_prefix_received: %s", prefix)
                self.prefixes_introduced += len(nlri_prefix_list)
            elif attr_type_code == 15:
                logger.debug(
                    "Attribute type=15 (MP_UNREACH_NLRI, flags:0x%s)",
                    binascii.b2a_hex(attr_flags_hex),
                )
                logger.debug("Attribute value=0x%s", binascii.b2a_hex(attr_value_hex))
                address_family_identifier_hex = attr_value_hex[0:2]
                logger.debug(
                    "  Address Family Identifier=0x%s",
                    binascii.b2a_hex(address_family_identifier_hex),
                )
                subsequent_address_family_identifier_hex = attr_value_hex[2:3]
                logger.debug(
                    "  Subsequent Address Family Identifier=0x%s",
                    binascii.b2a_hex(subsequent_address_family_identifier_hex),
                )
                wd_hex = attr_value_hex[3:]
                logger.debug("  Withdrawn Routes=0x%s", binascii.b2a_hex(wd_hex))
                wdr_prefix_list = get_prefix_list_from_hex(wd_hex)
                logger.debug("  Withdrawn routes prefix list: %s", wdr_prefix_list)
                for prefix in wdr_prefix_list:
                    logger.debug("  withdrawn_prefix_received: %s", prefix)
                self.prefixes_withdrawn += len(wdr_prefix_list)
            else:
                logger.debug(
                    "Unknown attribute type=%s, flags:0x%s)",
                    attr_type_code,
                    binascii.b2a_hex(attr_flags_hex),
                )
                logger.debug(
                    "Unknown attribute value=0x%s", binascii.b2a_hex(attr_value_hex)
                )
        return None

    def decode_update_message(self, msg):
        """Decode an UPDATE message (rfc4271#section-4.3)

        Arguments:
            :msg: message to be decoded in hex
        Returns:
            :return: None
        """
        logger.debug("Decoding update message:")
        marker_hex = msg[:16]
        logger.debug("Message header marker: 0x%s", binascii.b2a_hex(marker_hex))
        msg_length_hex = msg[16:18]
        msg_length = int(binascii.b2a_hex(msg_length_hex), 16)
        logger.debug(
            "Message length: 0x%s (%s)", binascii.b2a_hex(msg_length_hex), msg_length
        )
        msg_type_hex = msg[18:19]
        msg_type = int(binascii.b2a_hex(msg_type_hex), 16)

        with self.storage as stor:
            stor["update"] = binascii.hexlify(msg).decode()

        logger.debug("Evpn {}".format(self.evpn))
        if self.evpn:
            logger.debug("Skipping update decoding due to evpn data expected")
            return

        logger.debug("Graceful-restart {}".format(self.grace))
        if self.grace != 8:
            logger.debug(
                "Skipping update decoding due to graceful-restart data expected"
            )
            return

        logger.debug("Mvpn {}".format(self.mvpn))
        if self.mvpn:
            logger.debug("Skipping update decoding due to mvpn data expected")
            return

        logger.debug("L3vpn-mcast {}".format(self.l3vpn_mcast))
        if self.l3vpn_mcast:
            logger.debug("Skipping update decoding due to l3vpn_mcast data expected")
            return

        logger.debug("L3vpn-unicast {}".format(self.l3vpn))
        if self.l3vpn_mcast:
            logger.debug("Skipping update decoding due to l3vpn-unicast data expected")
            return

        logger.debug("Route-Target-Constrain {}".format(self.rt_constrain))
        if self.rt_constrain:
            logger.debug(
                "Skipping update decoding due to Route-Target-Constrain data expected"
            )
            return

        logger.debug("Ipv6-Unicast {}".format(self.ipv6))
        if self.ipv6:
            logger.debug("Skipping update decoding due to Ipv6 data expected")
            return

        logger.debug("BGP-LU {}".format(self.bgplu))
        if self.bgplu:
            logger.debug("Skipping update decoding due to BGP-LU data expected")
            return

        logger.debug("Allf {}".format(self.allf))
        if self.allf:
            logger.debug("Skipping update decoding")
            return

        if msg_type == 2:
            logger.debug("Message type: 0x%s (update)", binascii.b2a_hex(msg_type_hex))
            wdr_length_hex = msg[19:21]
            wdr_length = int(binascii.b2a_hex(wdr_length_hex), 16)
            logger.debug(
                "Withdrawn routes length: 0x%s (%s)",
                binascii.b2a_hex(wdr_length_hex),
                wdr_length,
            )
            wdr_hex = msg[21 : 21 + wdr_length]
            logger.debug("Withdrawn routes: 0x%s", binascii.b2a_hex(wdr_hex))
            wdr_prefix_list = get_prefix_list_from_hex(wdr_hex)
            logger.debug("Withdrawn routes prefix list: %s", wdr_prefix_list)
            for prefix in wdr_prefix_list:
                logger.debug("withdrawn_prefix_received: %s", prefix)
            total_pa_length_offset = 21 + wdr_length
            total_pa_length_hex = msg[
                total_pa_length_offset : total_pa_length_offset + 2
            ]
            total_pa_length = int(binascii.b2a_hex(total_pa_length_hex), 16)
            logger.debug(
                "Total path attribute length: 0x%s (%s)",
                binascii.b2a_hex(total_pa_length_hex),
                total_pa_length,
            )
            pa_offset = total_pa_length_offset + 2
            pa_hex = msg[pa_offset : pa_offset + total_pa_length]
            logger.debug("Path attributes: 0x%s", binascii.b2a_hex(pa_hex))
            self.decode_path_attributes(pa_hex)
            nlri_length = msg_length - 23 - total_pa_length - wdr_length
            logger.debug("Calculated NLRI length: %s", nlri_length)
            nlri_offset = pa_offset + total_pa_length
            nlri_hex = msg[nlri_offset : nlri_offset + nlri_length]
            logger.debug("NLRI: 0x%s", binascii.b2a_hex(nlri_hex))
            nlri_prefix_list = get_prefix_list_from_hex(nlri_hex)
            logger.debug("NLRI prefix list: %s", nlri_prefix_list)
            for prefix in nlri_prefix_list:
                logger.debug("nlri_prefix_received: %s", prefix)
            self.updates_received += 1
            self.prefixes_introduced += len(nlri_prefix_list)
            self.prefixes_withdrawn += len(wdr_prefix_list)
        else:
            logger.error(
                "Unexpected message type 0x%s in 0x%s",
                binascii.b2a_hex(msg_type_hex),
                binascii.b2a_hex(msg),
            )

    def wait_for_read(self):
        """Read message until timeout (next expected event)."""
        event_time = self.timer.get_next_event_time()
        wait_timedelta = min(event_time - time.time(), self.wfr)
        if wait_timedelta < 0:
            wait_timedelta = 0

        if not self.rx_activity_detected or not (self.updates_received % 100):
            logger.info(
                "total_received_update_message_counter: %s", self.updates_received
            )
            logger.info(
                "total_received_nlri_prefix_counter: %s", self.prefixes_introduced
            )
            logger.info(
                "total_received_withdrawn_prefix_counter: %s", self.prefixes_withdrawn
            )

        start_time = time.time()
        select.select([self.socket], [], [self.socket], wait_timedelta)
        timedelta = time.time() - start_time
        self.rx_idle_time += timedelta
        self.rx_activity_detected = timedelta < 1

        if not self.rx_activity_detected or not (self.updates_received % 100):
            logger.info("... idle for %.3fs", timedelta)
            logger.info("total_rx_idle_time_counter: %.3fs", self.rx_idle_time)
        return


class WriteTracker(object):
    """Class tracking enqueueing messages and sending chunks of them."""

    def __init__(self, bgp_socket, generator, timer):
        """The writer initialisation.

        Arguments:
            bgp_socket: socket to be used for sending
            generator: generator to be used for message generation
            timer: timer to be used for scheduling
        """
        self.socket = bgp_socket
        self.generator = generator
        self.timer = timer
        self.sending_message = False
        self.bytes_to_send = 0
        self.msg_out = b""

    def enqueue_message_for_sending(self, message):
        """Enqueue message and change state.

        Arguments:
            message: message to be enqueued into the msg_out buffer
        """
        self.msg_out += message
        self.bytes_to_send += len(message)
        self.sending_message = True

    def send_message_chunk_is_whole(self):
        """Send enqueued data from msg_out buffer

        Returns:
            :return: true if no remaining data to send
        """
        self.timer.snapshot()
        bytes_sent = self.socket.send(self.msg_out)
        self.msg_out = self.msg_out[bytes_sent:]
        self.bytes_to_send -= bytes_sent
        if not self.bytes_to_send:
            self.sending_message = False
            self.timer.reset_my_keepalive_time(self.timer.snapshot_time)
            return True
        return False


class StateTracker(object):
    """Main loop has state so complex it warrants this separate class."""

    def __init__(self, bgp_socket, generator, timer, inqueue, storage, cliargs):
        """The state tracker initialisation.

        Arguments:
            bgp_socket: socket to be used for sending / receiving
            generator: generator to be used for message generation
            timer: timer to be used for scheduling
            inqueue: user initiated messages queue
            storage: thread safe dict to store data for the rpc server
            cliargs: cli args from the user
        """
        self.socket = bgp_socket
        self.generator = generator
        self.timer = timer
        self.reader = ReadTracker(
            bgp_socket,
            timer,
            storage,
            evpn=cliargs.evpn,
            mvpn=cliargs.mvpn,
            l3vpn_mcast=cliargs.l3vpn_mcast,
            l3vpn=cliargs.l3vpn,
            allf=cliargs.allf,
            rt_constrain=cliargs.rt_constrain,
            ipv6=cliargs.ipv6,
            bgplu=cliargs.bgplu,
            grace=cliargs.grace,
            wait_for_read=cliargs.wfr,
        )
        self.writer = WriteTracker(bgp_socket, generator, timer)
        self.prioritize_writing = False
        self.inqueue = inqueue

    def perform_one_loop_iteration(self):
        """The main loop iteration

        Notes:
            Calculates priority, resolves all conditions, calls
            appropriate method and returns to caller to repeat.
        """
        self.timer.snapshot()
        if not self.prioritize_writing:
            if self.timer.is_time_for_my_keepalive():
                if not self.writer.sending_message:
                    self.writer.enqueue_message_for_sending(
                        self.generator.keepalive_message()
                    )
                    logger.info("KEEP ALIVE is sent.")
                self.prioritize_writing = True

        try:
            msg = self.inqueue.get_nowait()
            logger.info("Received message: {}".format(msg))
            msgbin = binascii.unhexlify(msg)
            self.writer.enqueue_message_for_sending(msgbin)
        except queue.Empty:
            pass

        list_list = select.select(
            [self.socket], [self.socket], [self.socket], self.timer.report_timedelta
        )
        read_list, write_list, except_list = list_list
        if except_list:
            logger.error("Exceptional state on the socket.")
            raise RuntimeError("Exceptional state on socket", self.socket)

        if not (self.prioritize_writing and write_list):
            if read_list:
                self.reader.read_message_chunk()
                return
            self.timer.check_peer_hold_time(self.timer.snapshot_time)

        if write_list:
            if self.writer.sending_message:
                whole = self.writer.send_message_chunk_is_whole()
                if self.prioritize_writing and whole:
                    self.prioritize_writing = False
                return
            if self.generator.remaining_prefixes:
                msg_out = self.generator.compose_update_message()
                if not self.generator.remaining_prefixes:
                    logger.info("All update messages generated.")
                    logger.info("Storing performance results.")
                    self.generator.store_results()
                    logger.info("Finally an END-OF-RIB is sent.")
                    msg_out += self.generator.update_message(
                        wr_prefixes=[], nlri_prefixes=[], end_of_rib=True
                    )
                self.writer.enqueue_message_for_sending(msg_out)
                return
            self.reader.wait_for_read()
            return

        logger.warning(
            "Input and output both blocked for "
            + str(self.timer.report_timedelta)
            + " seconds."
        )
        return


def create_logger(loglevel, logfile):
    """Create logger object

    Arguments:
        :loglevel: log level
        :logfile: log file name
    Returns:
        :return: logger object
    """
    logger = logging.getLogger("logger")
    log_formatter = logging.Formatter(
        "%(asctime)s %(levelname)s BGP-%(threadName)s: %(message)s"
    )
    console_handler = logging.StreamHandler()
    file_handler = logging.FileHandler(logfile, mode="w")
    console_handler.setFormatter(log_formatter)
    file_handler.setFormatter(log_formatter)
    logger.addHandler(console_handler)
    logger.addHandler(file_handler)
    logger.setLevel(loglevel)
    return logger


def job(arguments, inqueue, storage):
    """One time initialisation and iterations looping.

    Arguments:
        :arguments: Command line arguments
        :inqueue: Data to be sent from play3.py
        :storage: Shared dict for rpc server
    Returns:
        :return: None
    """
    bgp_socket = establish_connection(arguments)
    msg_in = read_open_message(bgp_socket)
    logger.info(binascii.hexlify(msg_in).decode())
    storage["open"] = binascii.hexlify(msg_in).decode()
    timer = TimeTracker(msg_in)
    generator = MessageGenerator(arguments)
    msg_out = generator.open_message()
    logger.debug("Sending the OPEN message: " + binascii.hexlify(msg_out).decode())
    bgp_socket.send(msg_out)

    msg_in = bgp_socket.recv(19)
    if msg_in != generator.keepalive_message():
        error_msg = "Open not confirmed by keepalive, instead got"
        logger.error(error_msg + ": " + binascii.hexlify(msg_in).decode())
        raise MessageError(error_msg, msg_in)
    timer.reset_peer_hold_time()
    timer.snapshot()
    msg_out = generator.keepalive_message()
    logger.debug("Sending a KEEP ALIVE message: " + binascii.hexlify(msg_out).decode())
    bgp_socket.send(msg_out)
    timer.reset_my_keepalive_time(timer.snapshot_time)

    state = StateTracker(bgp_socket, generator, timer, inqueue, storage, arguments)
    while True:
        state.perform_one_loop_iteration()


class Rpcs:
    """Handler for SimpleXMLRPCServer"""

    def __init__(self, sendqueues, storages):
        """Init method

        Arguments:
            :sendqueues: queues for data to be sent towards odl
            :storages: thread safe dicts
        """
        self.queues = sendqueues
        self.storages = storages

    def send(self, text, peer_id=0):
        """Data to be sent

        Arguments:
            :text: hex string of the data to be sent
            :peer_id: peer order number
        """
        self.queues[peer_id].put(text)

    def get(self, text="", peer_id=0):
        """Reads data from the storage

        Arguments:
            :text: a key to the storage to get the data
            :peer_id: peer order number
        Returns:
            :data: stored data
        """
        with self.storages[peer_id] as stor:
            return stor.get(text, "")

    def clean(self, text="", peer_id=None):
        """Cleans data from the storage

        Arguments:
            :text: a key to the storage to clean the data
            :peer_id: peer order number
        """
        if peer_id is not None:
            with self.storages[peer_id] as stor:
                if text in stor:
                    del stor[text]
        else:
            for storage in self.storages:
                with storage as stor:
                    if text in stor:
                        del stor[text]


def threaded_job(arguments):
    """Run the job threaded

    Arguments:
        :arguments: Command line arguments
    Returns:
        :return: None
    """
    amount_left = arguments.amount
    utils_left = arguments.multiplicity
    prefix_current = arguments.firstprefix
    myip_current = arguments.myip
    peerip_current = arguments.peerip
    port = arguments.port
    thread_args = []
    rpcqueues = []
    peers_storages = []

    while 1:
        if amount_left <= 0:
            amount_per_util = 0
        else:
            amount_per_util = int((amount_left - 1) / utils_left) + 1
        amount_left -= amount_per_util
        utils_left -= 1

        args = deepcopy(arguments)
        args.amount = amount_per_util
        args.firstprefix = prefix_current
        args.myip = myip_current
        args.peerip = peerip_current
        thread_args.append(args)

        if not utils_left:
            break

        count_to_be_added_in_iterations = amount_per_util - args.prefill
        number_of_iterations = (
            int((count_to_be_added_in_iterations - 1) / (args.insert - args.withdraw))
            + 1
        )
        prefix_current += ((number_of_iterations * args.insert) + args.prefill) * 16
        myip_current += 1
        if args.listen:
            peerip_current += 1

    try:
        for index, t in enumerate(thread_args, start=1):
            storage = SafeDict()
            peers_storages.append(storage)
            rpcqueue = queue.Queue()
            rpcqueues.append(rpcqueue)
            threading.Thread(
                target=job,
                args=(t, rpcqueue, storage),
                name=f"Peer-{index}",
                daemon=True,
            ).start()
    except Exception:
        print("Error: unable to start thread.")
        raise SystemExit(2)

    if arguments.usepeerip:
        ip = arguments.peerip
    else:
        ip = arguments.myip
    rpcserver = SimpleXMLRPCServer((ip.compressed, port), allow_none=True)
    rpcserver.register_instance(Rpcs(rpcqueues, peers_storages))
    rpcserver.serve_forever()


if __name__ == "__main__":
    arguments = parse_arguments()
    logger = create_logger(arguments.loglevel, arguments.logfile)
    threaded_job(arguments)