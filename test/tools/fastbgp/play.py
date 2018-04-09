"""Utility for playing generated BGP data to ODL.

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
from SimpleXMLRPCServer import SimpleXMLRPCServer
import argparse
import binascii
import ipaddr
import logging
import Queue
import select
import socket
import struct
import thread
import threading
import time


__author__ = "Vratko Polak"
__copyright__ = "Copyright(c) 2015, Cisco Systems, Inc."
__license__ = "Eclipse Public License v1.0"
__email__ = "vrpolak@cisco.com"


class SafeDict(dict):
    '''Thread safe dictionary

    The object will serve as thread safe data storage.
    It should be used with "with" statement.
    '''

    def __init__(self, * p_arg, ** n_arg):
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
    str_help = "Amount of IP prefixes to generate. (negative means ""infinite"")."
    parser.add_argument("--amount", default="1", type=int, help=str_help)
    str_help = "Maximum number of IP prefixes to be announced in one iteration"
    parser.add_argument("--insert", default="1", type=int, help=str_help)
    str_help = "Maximum number of IP prefixes to be withdrawn in one iteration"
    parser.add_argument("--withdraw", default="0", type=int, help=str_help)
    str_help = "The number of prefixes to process without withdrawals"
    parser.add_argument("--prefill", default="0", type=int, help=str_help)
    str_help = "Single or two separate UPDATEs for NLRI and WITHDRAWN lists sent"
    parser.add_argument("--updates", choices=["single", "separate"],
                        default=["separate"], help=str_help)
    str_help = "Base prefix IP address for prefix generation"
    parser.add_argument("--firstprefix", default="8.0.1.0",
                        type=ipaddr.IPv4Address, help=str_help)
    str_help = "The prefix length."
    parser.add_argument("--prefixlen", default=28, type=int, help=str_help)
    str_help = "Listen for connection, instead of initiating it."
    parser.add_argument("--listen", action="store_true", help=str_help)
    str_help = ("Numeric IP Address to bind to and derive BGP ID from." +
                "Default value only suitable for listening.")
    parser.add_argument("--myip", default="0.0.0.0",
                        type=ipaddr.IPv4Address, help=str_help)
    str_help = ("TCP port to bind to when listening or initiating connection." +
                "Default only suitable for initiating.")
    parser.add_argument("--myport", default="0", type=int, help=str_help)
    str_help = "The IP of the next hop to be placed into the update messages."
    parser.add_argument("--nexthop", default="192.0.2.1",
                        type=ipaddr.IPv4Address, dest="nexthop", help=str_help)
    str_help = "Identifier of the route originator."
    parser.add_argument("--originator", default=None,
                        type=ipaddr.IPv4Address, dest="originator", help=str_help)
    str_help = "Cluster list item identifier."
    parser.add_argument("--cluster", default=None,
                        type=ipaddr.IPv4Address, dest="cluster", help=str_help)
    str_help = ("Numeric IP Address to try to connect to." +
                "Currently no effect in listening mode.")
    parser.add_argument("--peerip", default="127.0.0.2",
                        type=ipaddr.IPv4Address, help=str_help)
    str_help = "TCP port to try to connect to. No effect in listening mode."
    parser.add_argument("--peerport", default="179", type=int, help=str_help)
    str_help = "Local hold time."
    parser.add_argument("--holdtime", default="180", type=int, help=str_help)
    str_help = "Log level (--error, --warning, --info, --debug)"
    parser.add_argument("--error", dest="loglevel", action="store_const",
                        const=logging.ERROR, default=logging.INFO,
                        help=str_help)
    parser.add_argument("--warning", dest="loglevel", action="store_const",
                        const=logging.WARNING, default=logging.INFO,
                        help=str_help)
    parser.add_argument("--info", dest="loglevel", action="store_const",
                        const=logging.INFO, default=logging.INFO,
                        help=str_help)
    parser.add_argument("--debug", dest="loglevel", action="store_const",
                        const=logging.DEBUG, default=logging.INFO,
                        help=str_help)
    str_help = "Log file name"
    parser.add_argument("--logfile", default="bgp_peer.log", help=str_help)
    str_help = "Trailing part of the csv result files for plotting purposes"
    parser.add_argument("--results", default="bgp.csv", type=str, help=str_help)
    str_help = "Minimum number of updates to reach to include result into csv."
    parser.add_argument("--threshold", default="1000", type=int, help=str_help)
    str_help = "RFC 4760 Multiprotocol Extensions for BGP-4 supported"
    parser.add_argument("--rfc4760", default=True, type=bool, help=str_help)
    str_help = "Link-State NLRI supported"
    parser.add_argument("--bgpls", default=False, type=bool, help=str_help)
    str_help = "Link-State NLRI: Identifier"
    parser.add_argument("-lsid", default="1", type=int, help=str_help)
    str_help = "Link-State NLRI: Tunnel ID"
    parser.add_argument("-lstid", default="1", type=int, help=str_help)
    str_help = "Link-State NLRI: LSP ID"
    parser.add_argument("-lspid", default="1", type=int, help=str_help)
    str_help = "Link-State NLRI: IPv4 Tunnel Sender Address"
    parser.add_argument("--lstsaddr", default="1.2.3.4",
                        type=ipaddr.IPv4Address, help=str_help)
    str_help = "Link-State NLRI: IPv4 Tunnel End Point Address"
    parser.add_argument("--lsteaddr", default="5.6.7.8",
                        type=ipaddr.IPv4Address, help=str_help)
    str_help = "Link-State NLRI: Identifier Step"
    parser.add_argument("-lsidstep", default="1", type=int, help=str_help)
    str_help = "Link-State NLRI: Tunnel ID Step"
    parser.add_argument("-lstidstep", default="2", type=int, help=str_help)
    str_help = "Link-State NLRI: LSP ID Step"
    parser.add_argument("-lspidstep", default="4", type=int, help=str_help)
    str_help = "Link-State NLRI: IPv4 Tunnel Sender Address Step"
    parser.add_argument("-lstsaddrstep", default="16", type=int, help=str_help)
    str_help = "Link-State NLRI: IPv4 Tunnel End Point Address Step"
    parser.add_argument("-lsteaddrstep", default="1", type=int, help=str_help)
    str_help = "How many play utilities are to be started."
    parser.add_argument("--multiplicity", default="1", type=int, help=str_help)
    str_help = "Open message includes multiprotocol extension capability l2vpn-evpn.\
Enabling this flag makes the script not decoding the update mesage, because of not\
supported decoding for these elements."
    parser.add_argument("--evpn", default=False, action="store_true", help=str_help)
    parser.add_argument("--wfr", default=10, type=int, help="Wait for read timeout")
    str_help = "Skipping well known attributes for update message"
    parser.add_argument("--skipattr", default=False, action="store_true", help=str_help)
    arguments = parser.parse_args()
    if arguments.multiplicity < 1:
        print "Multiplicity", arguments.multiplicity, "is not positive."
        raise SystemExit(1)
    # TODO: Are sanity checks (such as asnumber>=0) required?
    return arguments


def establish_connection(arguments):
    """Establish connection to BGP peer.

    Arguments:
        :arguments: following command-line argumets are used
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
        # bind need single tuple as argument
        listening_socket.bind((str(arguments.myip), arguments.myport))
        listening_socket.listen(1)
        bgp_socket, _ = listening_socket.accept()
        # TODO: Verify client IP is cotroller IP.
        listening_socket.close()
    else:
        logger.info("Connecting in the talking mode.")
        logger.debug("Local IP address: " + str(arguments.myip))
        logger.debug("Local port: " + str(arguments.myport))
        logger.debug("Remote IP address: " + str(arguments.peerip))
        logger.debug("Remote port: " + str(arguments.peerport))
        talking_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        talking_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        # bind to force specified address and port
        talking_socket.bind((str(arguments.myip), arguments.myport))
        # socket does not spead ipaddr, hence str()
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
        :return: required short_inf value.
    Notes:
        default offset value is the BGP message size offset.
    """
    high_byte_int = ord(message[offset])
    low_byte_int = ord(message[offset + 1])
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
        prefix_bit_len_hex = prefixes_hex[offset]
        prefix_bit_len = int(binascii.b2a_hex(prefix_bit_len_hex), 16)
        prefix_len = ((prefix_bit_len - 1) / 8) + 1
        prefix_hex = prefixes_hex[offset + 1: offset + 1 + prefix_len]
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
        message = binascii.hexlify(self.msg)
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
        Performs just basic incomming message checks
    """
    msg_in = bgp_socket.recv(65535)  # TODO: Is smaller buffer size safe?
    # TODO: Can the incoming open message be split in more than one packet?
    # Some validation.
    if len(msg_in) < 37:
        # 37 is minimal length of open message with 4-byte AS number.
        error_msg = (
            "Message length (" + str(len(msg_in)) + ") is smaller than "
            "minimal length of OPEN message with 4-byte AS number (37)"
        )
        logger.error(error_msg + ": " + binascii.hexlify(msg_in))
        raise MessageError(error_msg, msg_in)
    # TODO: We could check BGP marker, but it is defined only later;
    # decide what to do.
    reported_length = get_short_int_from_message(msg_in)
    if len(msg_in) != reported_length:
        error_msg = (
            "Expected message length (" + reported_length +
            ") does not match actual length (" + str(len(msg_in)) + ")"
        )
        logger.error(error_msg + binascii.hexlify(msg_in))
        raise MessageError(error_msg, msg_in)
    logger.info("Open message received.")
    return msg_in


class MessageGenerator(object):
    """Class which generates messages, holds states and configuration values."""

    # TODO: Define bgp marker as a class (constant) variable.
    def __init__(self, args):
        """Initialisation according to command-line args.

        Arguments:
            :args: argsparser's Namespace object which contains command-line
                options for MesageGenerator initialisation
        Notes:
            Calculates and stores default values used later on for
            message geeration.
        """
        self.total_prefix_amount = args.amount
        # Number of update messages left to be sent.
        self.remaining_prefixes = self.total_prefix_amount

        # New parameters initialisation
        self.iteration = 0
        self.prefix_base_default = args.firstprefix
        self.prefix_length_default = args.prefixlen
        self.wr_prefixes_default = []
        self.nlri_prefixes_default = []
        self.version_default = 4
        self.my_autonomous_system_default = args.asnumber
        self.hold_time_default = args.holdtime  # Local hold time.
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
            # total number of prefixes must grow to avoid infinite test loop
            self.prefix_count_to_add_default = self.prefix_count_to_del_default + 1
        self.slot_size_default = self.prefix_count_to_add_default
        self.remaining_prefixes_threshold = self.total_prefix_amount - args.prefill
        self.results_file_name_default = args.results
        self.performance_threshold_default = args.threshold
        self.rfc4760 = args.rfc4760
        self.bgpls = args.bgpls
        self.evpn = args.evpn
        self.skipattr = args.skipattr
        # Default values when BGP-LS Attributes are used
        if self.bgpls:
            self.prefix_count_to_add_default = 1
            self.prefix_count_to_del_default = 0
        self.ls_nlri_default = {"Identifier": args.lsid,
                                "TunnelID": args.lstid,
                                "LSPID": args.lspid,
                                "IPv4TunnelSenderAddress": args.lstsaddr,
                                "IPv4TunnelEndPointAddress": args.lsteaddr}
        self.lsid_step = args.lsidstep
        self.lstid_step = args.lstidstep
        self.lspid_step = args.lspidstep
        self.lstsaddr_step = args.lstsaddrstep
        self.lsteaddr_step = args.lsteaddrstep
        # Default values used for randomized part
        s1_slots = ((self.total_prefix_amount -
                     self.remaining_prefixes_threshold - 1) /
                    self.prefix_count_to_add_default + 1)
        s2_slots = ((self.remaining_prefixes_threshold - 1) /
                    (self.prefix_count_to_add_default -
                     self.prefix_count_to_del_default) + 1)
        # S1_First_Index = 0
        # S1_Last_Index = s1_slots * self.prefix_count_to_add_default - 1
        s2_first_index = s1_slots * self.prefix_count_to_add_default
        s2_last_index = (s2_first_index +
                         s2_slots * (self.prefix_count_to_add_default -
                                     self.prefix_count_to_del_default) - 1)
        self.slot_gap_default = ((self.total_prefix_amount -
                                  self.remaining_prefixes_threshold - 1) /
                                 self.prefix_count_to_add_default + 1)
        self.randomize_lowest_default = s2_first_index
        self.randomize_highest_default = s2_last_index
        # Initialising counters
        self.phase1_start_time = 0
        self.phase1_stop_time = 0
        self.phase2_start_time = 0
        self.phase2_stop_time = 0
        self.phase1_updates_sent = 0
        self.phase2_updates_sent = 0
        self.updates_sent = 0

        self.log_info = args.loglevel <= logging.INFO
        self.log_debug = args.loglevel <= logging.DEBUG
        """
        Flags needed for the MessageGenerator performance optimization.
        Calling logger methods each iteration even with proper log level set
        slows down significantly the MessageGenerator performance.
        Measured total generation time (1M updates, dry run, error log level):
        - logging based on basic logger features: 36,2s
        - logging based on advanced logger features (lazy logging): 21,2s
        - conditional calling of logger methods enclosed inside condition: 8,6s
        """

        logger.info("Generator initialisation")
        logger.info("  Target total number of prefixes to be introduced: " +
                    str(self.total_prefix_amount))
        logger.info("  Prefix base: " + str(self.prefix_base_default) + "/" +
                    str(self.prefix_length_default))
        logger.info("  My Autonomous System number: " +
                    str(self.my_autonomous_system_default))
        logger.info("  My Hold Time: " + str(self.hold_time_default))
        logger.info("  My BGP Identifier: " + str(self.bgp_identifier_default))
        logger.info("  Next Hop: " + str(self.next_hop_default))
        logger.info("  Originator ID: " + str(self.originator_id_default))
        logger.info("  Cluster list: " + str(self.cluster_list_item_default))
        logger.info("  Prefix count to be inserted at once: " +
                    str(self.prefix_count_to_add_default))
        logger.info("  Prefix count to be withdrawn at once: " +
                    str(self.prefix_count_to_del_default))
        logger.info("  Fast pre-fill up to " +
                    str(self.total_prefix_amount -
                        self.remaining_prefixes_threshold) + " prefixes")
        logger.info("  Remaining number of prefixes to be processed " +
                    "in parallel with withdrawals: " +
                    str(self.remaining_prefixes_threshold))
        logger.debug("  Prefix index range used after pre-fill procedure [" +
                     str(self.randomize_lowest_default) + ", " +
                     str(self.randomize_highest_default) + "]")
        if self.single_update_default:
            logger.info("  Common single UPDATE will be generated " +
                        "for both NLRI & WITHDRAWN lists")
        else:
            logger.info("  Two separate UPDATEs will be generated " +
                        "for each NLRI & WITHDRAWN lists")
        if self.randomize_updates_default:
            logger.info("  Generation of UPDATE messages will be randomized")
        logger.info("  Let\'s go ...\n")

        # TODO: Notification for hold timer expiration can be handy.

    def store_results(self, file_name=None, threshold=None):
        """ Stores specified results into files based on file_name value.

        Arguments:
            :param file_name: Trailing (common) part of result file names
            :param threshold: Minimum number of sent updates needed for each
                              result to be included into result csv file
                              (mainly needed because of the result accuracy)
        Returns:
            :return: n/a
        """
        # default values handling
        # TODO optimize default values handling (use e.g. dicionary.update() approach)
        if file_name is None:
            file_name = self.results_file_name_default
        if threshold is None:
            threshold = self.performance_threshold_default
        # performance calculation
        if self.phase1_updates_sent >= threshold:
            totals1 = self.phase1_updates_sent
            performance1 = int(self.phase1_updates_sent /
                               (self.phase1_stop_time - self.phase1_start_time))
        else:
            totals1 = None
            performance1 = None
        if self.phase2_updates_sent >= threshold:
            totals2 = self.phase2_updates_sent
            performance2 = int(self.phase2_updates_sent /
                               (self.phase2_stop_time - self.phase2_start_time))
        else:
            totals2 = None
            performance2 = None

        logger.info("#" * 10 + " Final results " + "#" * 10)
        logger.info("Number of iterations: " + str(self.iteration))
        logger.info("Number of UPDATE messages sent in the pre-fill phase: " +
                    str(self.phase1_updates_sent))
        logger.info("The pre-fill phase duration: " +
                    str(self.phase1_stop_time - self.phase1_start_time) + "s")
        logger.info("Number of UPDATE messages sent in the 2nd test phase: " +
                    str(self.phase2_updates_sent))
        logger.info("The 2nd test phase duration: " +
                    str(self.phase2_stop_time - self.phase2_start_time) + "s")
        logger.info("Threshold for performance reporting: " + str(threshold))

        # making labels
        phase1_label = ("pre-fill " + str(self.prefix_count_to_add_default) +
                        " route(s) per UPDATE")
        if self.single_update_default:
            phase2_label = "+" + (str(self.prefix_count_to_add_default) +
                                  "/-" + str(self.prefix_count_to_del_default) +
                                  " routes per UPDATE")
        else:
            phase2_label = "+" + (str(self.prefix_count_to_add_default) +
                                  "/-" + str(self.prefix_count_to_del_default) +
                                  " routes in two UPDATEs")
        # collecting capacity and performance results
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
            logger.info("Message generator performance results stored in " +
                        file_name + ":")
            logger.info("  " + first_line)
            logger.info("  " + second_line)
        finally:
            f.close()

    # Return pseudo-randomized (reproducible) index for selected range
    def randomize_index(self, index, lowest=None, highest=None):
        """Calculates pseudo-randomized index from selected range.

        Arguments:
            :param index: input index
            :param lowest: the lowes index from the randomized area
            :param highest: the highest index from the randomized area
        Returns:
            :return: the (pseudo)randomized index
        Notes:
            Created just as a fame for future generator enhancement.
        """
        # default values handling
        # TODO optimize default values handling (use e.g. dicionary.update() approach)
        if lowest is None:
            lowest = self.randomize_lowest_default
        if highest is None:
            highest = self.randomize_highest_default
        # randomize
        if (index >= lowest) and (index <= highest):
            # we are in the randomized range -> shuffle it inside
            # the range (now just reverse the order)
            new_index = highest - (index - lowest)
        else:
            # we are out of the randomized range -> nothing to do
            new_index = index
        return new_index

    def get_ls_nlri_values(self, index):
        """Generates LS-NLRI parameters.
        http://tools.ietf.org/html/draft-ietf-idr-te-lsp-distribution-03

        Arguments:
            :param index: index (iteration)
        Returns:
            :return: dictionary of LS NLRI parameters and values
        """
        # generating list of LS NLRI parameters
        identifier = self.ls_nlri_default["Identifier"] + index / self.lsid_step
        ipv4_tunnel_sender_address = self.ls_nlri_default["IPv4TunnelSenderAddress"] + index / self.lstsaddr_step
        tunnel_id = self.ls_nlri_default["TunnelID"] + index / self.lstid_step
        lsp_id = self.ls_nlri_default["LSPID"] + index / self.lspid_step
        ipv4_tunnel_endpoint_address = self.ls_nlri_default["IPv4TunnelEndPointAddress"] + index / self.lsteaddr_step
        ls_nlri_values = {"Identifier": identifier,
                          "IPv4TunnelSenderAddress": ipv4_tunnel_sender_address,
                          "TunnelID": tunnel_id, "LSPID": lsp_id,
                          "IPv4TunnelEndPointAddress": ipv4_tunnel_endpoint_address}
        return ls_nlri_values

    def get_prefix_list(self, slot_index, slot_size=None, prefix_base=None,
                        prefix_len=None, prefix_count=None, randomize=None):
        """Generates list of IP address prefixes.

        Arguments:
            :param slot_index: index of group of prefix addresses
            :param slot_size: size of group of prefix addresses
                in [number of included prefixes]
            :param prefix_base: IP address of the first prefix
                (slot_index = 0, prefix_index = 0)
            :param prefix_len: length of the prefix in bites
                (the same as size of netmask)
            :param prefix_count: number of prefixes to be returned
                from the specified slot
        Returns:
            :return: list of generated IP address prefixes
        """
        # default values handling
        # TODO optimize default values handling (use e.g. dicionary.update() approach)
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
        # generating list of prefixes
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

    def compose_update_message(self, prefix_count_to_add=None,
                               prefix_count_to_del=None):
        """Composes an UPDATE message

        Arguments:
            :param prefix_count_to_add: # of prefixes to put into NLRI list
            :param prefix_count_to_del: # of prefixes to put into WITHDRAWN list
        Returns:
            :return: encoded UPDATE message in HEX
        Notes:
            Optionally generates separate UPDATEs for NLRI and WITHDRAWN
            lists or common message wich includes both prefix lists.
            Updates global counters.
        """
        # default values handling
        # TODO optimize default values handling (use e.g. dicionary.update() approach)
        if prefix_count_to_add is None:
            prefix_count_to_add = self.prefix_count_to_add_default
        if prefix_count_to_del is None:
            prefix_count_to_del = self.prefix_count_to_del_default
        # logging
        if self.log_info and not (self.iteration % 1000):
            logger.info("Iteration: " + str(self.iteration) +
                        " - total remaining prefixes: " +
                        str(self.remaining_prefixes))
        if self.log_debug:
            logger.debug("#" * 10 + " Iteration: " +
                         str(self.iteration) + " " + "#" * 10)
            logger.debug("Remaining prefixes: " +
                         str(self.remaining_prefixes))
        # scenario type & one-shot counter
        straightforward_scenario = (self.remaining_prefixes >
                                    self.remaining_prefixes_threshold)
        if straightforward_scenario:
            prefix_count_to_del = 0
            if self.log_debug:
                logger.debug("--- STARAIGHTFORWARD SCENARIO ---")
            if not self.phase1_start_time:
                self.phase1_start_time = time.time()
        else:
            if self.log_debug:
                logger.debug("--- COMBINED SCENARIO ---")
            if not self.phase2_start_time:
                self.phase2_start_time = time.time()
        # tailor the number of prefixes if needed
        prefix_count_to_add = (prefix_count_to_del +
                               min(prefix_count_to_add - prefix_count_to_del,
                                   self.remaining_prefixes))
        # prefix slots selection for insertion and withdrawal
        slot_index_to_add = self.iteration
        slot_index_to_del = slot_index_to_add - self.slot_gap_default
        # getting lists of prefixes for insertion in this iteration
        if self.log_debug:
            logger.debug("Prefixes to be inserted in this iteration:")
        prefix_list_to_add = self.get_prefix_list(slot_index_to_add,
                                                  prefix_count=prefix_count_to_add)
        # getting lists of prefixes for withdrawal in this iteration
        if self.log_debug:
            logger.debug("Prefixes to be withdrawn in this iteration:")
        prefix_list_to_del = self.get_prefix_list(slot_index_to_del,
                                                  prefix_count=prefix_count_to_del)
        # generating the UPDATE mesage with LS-NLRI only
        if self.bgpls:
            ls_nlri = self.get_ls_nlri_values(self.iteration)
            msg_out = self.update_message(wr_prefixes=[], nlri_prefixes=[],
                                          **ls_nlri)
        else:
            # generating the UPDATE message with prefix lists
            if self.single_update_default:
                # Send prefixes to be introduced and withdrawn
                # in one UPDATE message
                msg_out = self.update_message(wr_prefixes=prefix_list_to_del,
                                              nlri_prefixes=prefix_list_to_add)
            else:
                # Send prefixes to be introduced and withdrawn
                # in separate UPDATE messages (if needed)
                msg_out = self.update_message(wr_prefixes=[],
                                              nlri_prefixes=prefix_list_to_add)
                if prefix_count_to_del:
                    msg_out += self.update_message(wr_prefixes=prefix_list_to_del,
                                                   nlri_prefixes=[])
        # updating counters - who knows ... maybe I am last time here ;)
        if straightforward_scenario:
            self.phase1_stop_time = time.time()
            self.phase1_updates_sent = self.updates_sent
        else:
            self.phase2_stop_time = time.time()
            self.phase2_updates_sent = (self.updates_sent -
                                        self.phase1_updates_sent)
        # updating totals for the next iteration
        self.iteration += 1
        self.remaining_prefixes -= (prefix_count_to_add - prefix_count_to_del)
        # returning the encoded message
        return msg_out

    # Section of message encoders

    def open_message(self, version=None, my_autonomous_system=None,
                     hold_time=None, bgp_identifier=None):
        """Generates an OPEN Message (rfc4271#section-4.2)

        Arguments:
            :param version: see the rfc4271#section-4.2
            :param my_autonomous_system: see the rfc4271#section-4.2
            :param hold_time: see the rfc4271#section-4.2
            :param bgp_identifier: see the rfc4271#section-4.2
        Returns:
            :return: encoded OPEN message in HEX
        """

        # default values handling
        # TODO optimize default values handling (use e.g. dicionary.update() approach)
        if version is None:
            version = self.version_default
        if my_autonomous_system is None:
            my_autonomous_system = self.my_autonomous_system_default
        if hold_time is None:
            hold_time = self.hold_time_default
        if bgp_identifier is None:
            bgp_identifier = self.bgp_identifier_default

        # Marker
        marker_hex = "\xFF" * 16

        # Type
        type = 1
        type_hex = struct.pack("B", type)

        # version
        version_hex = struct.pack("B", version)

        # my_autonomous_system
        # AS_TRANS value, 23456 decadic.
        my_autonomous_system_2_bytes = 23456
        # AS number is mappable to 2 bytes
        if my_autonomous_system < 65536:
            my_autonomous_system_2_bytes = my_autonomous_system
        my_autonomous_system_hex_2_bytes = struct.pack(">H",
                                                       my_autonomous_system)

        # Hold Time
        hold_time_hex = struct.pack(">H", hold_time)

        # BGP Identifier
        bgp_identifier_hex = struct.pack(">I", bgp_identifier)

        # Optional Parameters
        optional_parameters_hex = ""
        if self.rfc4760:
            optional_parameter_hex = (
                "\x02"  # Param type ("Capability Ad")
                "\x06"  # Length (6 bytes)
                "\x01"  # Capability type (NLRI Unicast),
                        # see RFC 4760, secton 8
                "\x04"  # Capability value length
                "\x00\x01"  # AFI (Ipv4)
                "\x00"  # (reserved)
                "\x01"  # SAFI (Unicast)
            )
            optional_parameters_hex += optional_parameter_hex

        if self.bgpls:
            optional_parameter_hex = (
                "\x02"  # Param type ("Capability Ad")
                "\x06"  # Length (6 bytes)
                "\x01"  # Capability type (NLRI Unicast),
                        # see RFC 4760, secton 8
                "\x04"  # Capability value length
                "\x40\x04"  # AFI (BGP-LS)
                "\x00"  # (reserved)
                "\x47"  # SAFI (BGP-LS)
            )
            optional_parameters_hex += optional_parameter_hex

        if self.evpn:
            optional_parameter_hex = (
                "\x02"  # Param type ("Capability Ad")
                "\x06"  # Length (6 bytes)
                "\x01"  # Multiprotocol extetension capability,
                "\x04"  # Capability value length
                "\x00\x19"  # AFI (L2-VPN)
                "\x00"  # (reserved)
                "\x46"  # SAFI (EVPN)
            )
            optional_parameters_hex += optional_parameter_hex

        optional_parameter_hex = (
            "\x02"  # Param type ("Capability Ad")
            "\x06"  # Length (6 bytes)
            "\x41"  # "32 bit AS Numbers Support"
                    # (see RFC 6793, section 3)
            "\x04"  # Capability value length
        )
        optional_parameter_hex += (
            struct.pack(">I", my_autonomous_system)  # My AS in 32 bit format
        )
        optional_parameters_hex += optional_parameter_hex

        # Optional Parameters Length
        optional_parameters_length = len(optional_parameters_hex)
        optional_parameters_length_hex = struct.pack("B",
                                                     optional_parameters_length)

        # Length (big-endian)
        length = (
            len(marker_hex) + 2 + len(type_hex) + len(version_hex) +
            len(my_autonomous_system_hex_2_bytes) +
            len(hold_time_hex) + len(bgp_identifier_hex) +
            len(optional_parameters_length_hex) +
            len(optional_parameters_hex)
        )
        length_hex = struct.pack(">H", length)

        # OPEN Message
        message_hex = (
            marker_hex +
            length_hex +
            type_hex +
            version_hex +
            my_autonomous_system_hex_2_bytes +
            hold_time_hex +
            bgp_identifier_hex +
            optional_parameters_length_hex +
            optional_parameters_hex
        )

        if self.log_debug:
            logger.debug("OPEN message encoding")
            logger.debug("  Marker=0x" + binascii.hexlify(marker_hex))
            logger.debug("  Length=" + str(length) + " (0x" +
                         binascii.hexlify(length_hex) + ")")
            logger.debug("  Type=" + str(type) + " (0x" +
                         binascii.hexlify(type_hex) + ")")
            logger.debug("  Version=" + str(version) + " (0x" +
                         binascii.hexlify(version_hex) + ")")
            logger.debug("  My Autonomous System=" +
                         str(my_autonomous_system_2_bytes) + " (0x" +
                         binascii.hexlify(my_autonomous_system_hex_2_bytes) +
                         ")")
            logger.debug("  Hold Time=" + str(hold_time) + " (0x" +
                         binascii.hexlify(hold_time_hex) + ")")
            logger.debug("  BGP Identifier=" + str(bgp_identifier) +
                         " (0x" + binascii.hexlify(bgp_identifier_hex) + ")")
            logger.debug("  Optional Parameters Length=" +
                         str(optional_parameters_length) + " (0x" +
                         binascii.hexlify(optional_parameters_length_hex) +
                         ")")
            logger.debug("  Optional Parameters=0x" +
                         binascii.hexlify(optional_parameters_hex))
            logger.debug("OPEN message encoded: 0x%s",
                         binascii.b2a_hex(message_hex))

        return message_hex

    def update_message(self, wr_prefixes=None, nlri_prefixes=None,
                       wr_prefix_length=None, nlri_prefix_length=None,
                       my_autonomous_system=None, next_hop=None,
                       originator_id=None, cluster_list_item=None,
                       end_of_rib=False, **ls_nlri_params):
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

        # default values handling
        # TODO optimize default values handling (use e.g. dicionary.update() approach)
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
        ls_nlri = self.ls_nlri_default.copy()
        ls_nlri.update(ls_nlri_params)

        # Marker
        marker_hex = "\xFF" * 16

        # Type
        type = 2
        type_hex = struct.pack("B", type)

        # Withdrawn Routes
        withdrawn_routes_hex = ""
        if not self.bgpls:
            bytes = ((wr_prefix_length - 1) / 8) + 1
            for prefix in wr_prefixes:
                withdrawn_route_hex = (struct.pack("B", wr_prefix_length) +
                                       struct.pack(">I", int(prefix))[:bytes])
                withdrawn_routes_hex += withdrawn_route_hex

        # Withdrawn Routes Length
        withdrawn_routes_length = len(withdrawn_routes_hex)
        withdrawn_routes_length_hex = struct.pack(">H", withdrawn_routes_length)

        # TODO: to replace hardcoded string by encoding?
        # Path Attributes
        path_attributes_hex = ""
        if not self.skipattr:
            path_attributes_hex += (
                "\x40"  # Flags ("Well-Known")
                "\x01"  # Type (ORIGIN)
                "\x01"  # Length (1)
                "\x00"  # Origin: IGP
            )
            path_attributes_hex += (
                "\x40"  # Flags ("Well-Known")
                "\x02"  # Type (AS_PATH)
                "\x06"  # Length (6)
                "\x02"  # AS segment type (AS_SEQUENCE)
                "\x01"  # AS segment length (1)
            )
            my_as_hex = struct.pack(">I", my_autonomous_system)
            path_attributes_hex += my_as_hex  # AS segment (4 bytes)
            path_attributes_hex += (
                "\x40"  # Flags ("Well-Known")
                "\x05"  # Type (LOCAL_PREF)
                "\x04"  # Length (4)
                "\x00\x00\x00\x64"  # (100)
            )
        if nlri_prefixes != []:
            path_attributes_hex += (
                "\x40"  # Flags ("Well-Known")
                "\x03"  # Type (NEXT_HOP)
                "\x04"  # Length (4)
            )
            next_hop_hex = struct.pack(">I", int(next_hop))
            path_attributes_hex += (
                next_hop_hex  # IP address of the next hop (4 bytes)
            )
            if originator_id is not None:
                path_attributes_hex += (
                    "\x80"  # Flags ("Optional, non-transitive")
                    "\x09"  # Type (ORIGINATOR_ID)
                    "\x04"  # Length (4)
                )           # ORIGINATOR_ID (4 bytes)
                path_attributes_hex += struct.pack(">I", int(originator_id))
            if cluster_list_item is not None:
                path_attributes_hex += (
                    "\x80"  # Flags ("Optional, non-transitive")
                    "\x0a"  # Type (CLUSTER_LIST)
                    "\x04"  # Length (4)
                )           # one CLUSTER_LIST item (4 bytes)
                path_attributes_hex += struct.pack(">I", int(cluster_list_item))

        if self.bgpls and not end_of_rib:
            path_attributes_hex += (
                "\x80"  # Flags ("Optional, non-transitive")
                "\x0e"  # Type (MP_REACH_NLRI)
                "\x22"  # Length (34)
                "\x40\x04"  # AFI (BGP-LS)
                "\x47"  # SAFI (BGP-LS)
                "\x04"  # Next Hop Length (4)
            )
            path_attributes_hex += struct.pack(">I", int(next_hop))
            path_attributes_hex += "\x00"           # Reserved
            path_attributes_hex += (
                "\x00\x05"  # LS-NLRI.NLRIType (IPv4 TE LSP NLRI)
                "\x00\x15"  # LS-NLRI.TotalNLRILength (21)
                "\x07"      # LS-NLRI.Variable.ProtocolID (RSVP-TE)
            )
            path_attributes_hex += struct.pack(">Q", int(ls_nlri["Identifier"]))
            path_attributes_hex += struct.pack(">I", int(ls_nlri["IPv4TunnelSenderAddress"]))
            path_attributes_hex += struct.pack(">H", int(ls_nlri["TunnelID"]))
            path_attributes_hex += struct.pack(">H", int(ls_nlri["LSPID"]))
            path_attributes_hex += struct.pack(">I", int(ls_nlri["IPv4TunnelEndPointAddress"]))

        # Total Path Attributes Length
        total_path_attributes_length = len(path_attributes_hex)
        total_path_attributes_length_hex = struct.pack(">H", total_path_attributes_length)

        # Network Layer Reachability Information
        nlri_hex = ""
        if not self.bgpls:
            bytes = ((nlri_prefix_length - 1) / 8) + 1
            for prefix in nlri_prefixes:
                nlri_prefix_hex = (struct.pack("B", nlri_prefix_length) +
                                   struct.pack(">I", int(prefix))[:bytes])
                nlri_hex += nlri_prefix_hex

        # Length (big-endian)
        length = (
            len(marker_hex) + 2 + len(type_hex) +
            len(withdrawn_routes_length_hex) + len(withdrawn_routes_hex) +
            len(total_path_attributes_length_hex) + len(path_attributes_hex) +
            len(nlri_hex))
        length_hex = struct.pack(">H", length)

        # UPDATE Message
        message_hex = (
            marker_hex +
            length_hex +
            type_hex +
            withdrawn_routes_length_hex +
            withdrawn_routes_hex +
            total_path_attributes_length_hex +
            path_attributes_hex +
            nlri_hex
        )

        if self.log_debug:
            logger.debug("UPDATE message encoding")
            logger.debug("  Marker=0x" + binascii.hexlify(marker_hex))
            logger.debug("  Length=" + str(length) + " (0x" +
                         binascii.hexlify(length_hex) + ")")
            logger.debug("  Type=" + str(type) + " (0x" +
                         binascii.hexlify(type_hex) + ")")
            logger.debug("  withdrawn_routes_length=" +
                         str(withdrawn_routes_length) + " (0x" +
                         binascii.hexlify(withdrawn_routes_length_hex) + ")")
            logger.debug("  Withdrawn_Routes=" + str(wr_prefixes) + "/" +
                         str(wr_prefix_length) + " (0x" +
                         binascii.hexlify(withdrawn_routes_hex) + ")")
            if total_path_attributes_length:
                logger.debug("  Total Path Attributes Length=" +
                             str(total_path_attributes_length) + " (0x" +
                             binascii.hexlify(total_path_attributes_length_hex) + ")")
                logger.debug("  Path Attributes=" + "(0x" +
                             binascii.hexlify(path_attributes_hex) + ")")
                logger.debug("    Origin=IGP")
                logger.debug("    AS path=" + str(my_autonomous_system))
                logger.debug("    Next hop=" + str(next_hop))
                if originator_id is not None:
                    logger.debug("    Originator id=" + str(originator_id))
                if cluster_list_item is not None:
                    logger.debug("    Cluster list=" + str(cluster_list_item))
                if self.bgpls:
                    logger.debug("    MP_REACH_NLRI: %s", ls_nlri)
            logger.debug("  Network Layer Reachability Information=" +
                         str(nlri_prefixes) + "/" + str(nlri_prefix_length) +
                         " (0x" + binascii.hexlify(nlri_hex) + ")")
            logger.debug("UPDATE message encoded: 0x" +
                         binascii.b2a_hex(message_hex))

        # updating counter
        self.updates_sent += 1
        # returning encoded message
        return message_hex

    def notification_message(self, error_code, error_subcode, data_hex=""):
        """Generates a NOTIFICATION Message (rfc4271#section-4.5)

        Arguments:
            :param error_code: see the rfc4271#section-4.5
            :param error_subcode: see the rfc4271#section-4.5
            :param data_hex: see the rfc4271#section-4.5
        Returns:
            :return: encoded NOTIFICATION message in HEX
        """

        # Marker
        marker_hex = "\xFF" * 16

        # Type
        type = 3
        type_hex = struct.pack("B", type)

        # Error Code
        error_code_hex = struct.pack("B", error_code)

        # Error Subode
        error_subcode_hex = struct.pack("B", error_subcode)

        # Length (big-endian)
        length = (len(marker_hex) + 2 + len(type_hex) + len(error_code_hex) +
                  len(error_subcode_hex) + len(data_hex))
        length_hex = struct.pack(">H", length)

        # NOTIFICATION Message
        message_hex = (
            marker_hex +
            length_hex +
            type_hex +
            error_code_hex +
            error_subcode_hex +
            data_hex
        )

        if self.log_debug:
            logger.debug("NOTIFICATION message encoding")
            logger.debug("  Marker=0x" + binascii.hexlify(marker_hex))
            logger.debug("  Length=" + str(length) + " (0x" +
                         binascii.hexlify(length_hex) + ")")
            logger.debug("  Type=" + str(type) + " (0x" +
                         binascii.hexlify(type_hex) + ")")
            logger.debug("  Error Code=" + str(error_code) + " (0x" +
                         binascii.hexlify(error_code_hex) + ")")
            logger.debug("  Error Subode=" + str(error_subcode) + " (0x" +
                         binascii.hexlify(error_subcode_hex) + ")")
            logger.debug("  Data=" + " (0x" + binascii.hexlify(data_hex) + ")")
            logger.debug("NOTIFICATION message encoded: 0x%s",
                         binascii.b2a_hex(message_hex))

        return message_hex

    def keepalive_message(self):
        """Generates a KEEP ALIVE Message (rfc4271#section-4.4)

        Returns:
            :return: encoded KEEP ALIVE message in HEX
        """

        # Marker
        marker_hex = "\xFF" * 16

        # Type
        type = 4
        type_hex = struct.pack("B", type)

        # Length (big-endian)
        length = len(marker_hex) + 2 + len(type_hex)
        length_hex = struct.pack(">H", length)

        # KEEP ALIVE Message
        message_hex = (
            marker_hex +
            length_hex +
            type_hex
        )

        if self.log_debug:
            logger.debug("KEEP ALIVE message encoding")
            logger.debug("  Marker=0x" + binascii.hexlify(marker_hex))
            logger.debug("  Length=" + str(length) + " (0x" +
                         binascii.hexlify(length_hex) + ")")
            logger.debug("  Type=" + str(type) + " (0x" +
                         binascii.hexlify(type_hex) + ")")
            logger.debug("KEEP ALIVE message encoded: 0x%s",
                         binascii.b2a_hex(message_hex))

        return message_hex


class TimeTracker(object):
    """Class for tracking timers, both for my keepalives and
    peer's hold time.
    """

    def __init__(self, msg_in):
        """Initialisation. based on defaults and OPEN message from peer.

        Arguments:
            msg_in: the OPEN message received from peer.
        """
        # Note: Relative time is always named timedelta, to stress that
        # the (non-delta) time is absolute.
        self.report_timedelta = 1.0  # In seconds. TODO: Configurable?
        # Upper bound for being stuck in the same state, we should
        # at least report something before continuing.
        # Negotiate the hold timer by taking the smaller
        # of the 2 values (mine and the peer's).
        hold_timedelta = 180  # Not an attribute of self yet.
        # TODO: Make the default value configurable,
        # default value could mirror what peer said.
        peer_hold_timedelta = get_short_int_from_message(msg_in, offset=22)
        if hold_timedelta > peer_hold_timedelta:
            hold_timedelta = peer_hold_timedelta
        if hold_timedelta != 0 and hold_timedelta < 3:
            logger.error("Invalid hold timedelta value: " + str(hold_timedelta))
            raise ValueError("Invalid hold timedelta value: ", hold_timedelta)
        self.hold_timedelta = hold_timedelta
        # If we do not hear from peer this long, we assume it has died.
        self.keepalive_timedelta = int(hold_timedelta / 3.0)
        # Upper limit for duration between messages, to avoid being
        # declared to be dead.
        # The same as calling snapshot(), but also declares a field.
        self.snapshot_time = time.time()
        # Sometimes we need to store time. This is where to get
        # the value from afterwards. Time_keepalive may be too strict.
        self.peer_hold_time = self.snapshot_time + self.hold_timedelta
        # At this time point, peer will be declared dead.
        self.my_keepalive_time = None  # to be set later
        # At this point, we should be sending keepalive message.

    def snapshot(self):
        """Store current time in instance data to use later."""
        # Read as time before something interesting was called.
        self.snapshot_time = time.time()

    def reset_peer_hold_time(self):
        """Move hold time to future as peer has just proven it still lives."""
        self.peer_hold_time = time.time() + self.hold_timedelta

    # Some methods could rely on self.snapshot_time, but it is better
    # to require user to provide it explicitly.
    def reset_my_keepalive_time(self, keepalive_time):
        """Calculate and set the next my KEEP ALIVE timeout time

        Arguments:
            :keepalive_time: the initial value of the KEEP ALIVE timer
        """
        self.my_keepalive_time = keepalive_time + self.keepalive_timedelta

    def is_time_for_my_keepalive(self):
        """Check for my KEEP ALIVE timeout occurence"""
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
        # Hold time = 0 means keepalive checking off.
        if self.hold_timedelta != 0:
            # time.time() may be too strict
            if snapshot_time > self.peer_hold_time:
                logger.error("Peer has overstepped the hold timer.")
                raise RuntimeError("Peer has overstepped the hold timer.")
                # TODO: Include hold_timedelta?
                # TODO: Add notification sending (attempt). That means
                # move to write tracker.


class ReadTracker(object):
    """Class for tracking read of mesages chunk by chunk and
    for idle waiting.
    """

    def __init__(self, bgp_socket, timer, storage, evpn=False, wait_for_read=10):
        """The reader initialisation.

        Arguments:
            bgp_socket: socket to be used for sending
            timer: timer to be used for scheduling
            storage: thread safe dict
            evpn: flag that evpn functionality is tested
        """
        # References to outside objects.
        self.socket = bgp_socket
        self.timer = timer
        # BGP marker length plus length field length.
        self.header_length = 18
        # TODO: make it class (constant) attribute
        # Computation of where next chunk ends depends on whether
        # we are beyond length field.
        self.reading_header = True
        # Countdown towards next size computation.
        self.bytes_to_read = self.header_length
        # Incremental buffer for message under read.
        self.msg_in = ""
        # Initialising counters
        self.updates_received = 0
        self.prefixes_introduced = 0
        self.prefixes_withdrawn = 0
        self.rx_idle_time = 0
        self.rx_activity_detected = True
        self.storage = storage
        self.evpn = evpn
        self.wfr = wait_for_read

    def read_message_chunk(self):
        """Read up to one message

        Note:
            Currently it does not return anything.
        """
        # TODO: We could return the whole message, currently not needed.
        # We assume the socket is readable.
        chunk_message = self.socket.recv(self.bytes_to_read)
        self.msg_in += chunk_message
        self.bytes_to_read -= len(chunk_message)
        # TODO: bytes_to_read < 0 is not possible, right?
        if not self.bytes_to_read:
            # Finished reading a logical block.
            if self.reading_header:
                # The logical block was a BGP header.
                # Now we know the size of the message.
                self.reading_header = False
                self.bytes_to_read = (get_short_int_from_message(self.msg_in) -
                                      self.header_length)
            else:  # We have finished reading the body of the message.
                # Peer has just proven it is still alive.
                self.timer.reset_peer_hold_time()
                # TODO: Do we want to count received messages?
                # This version ignores the received message.
                # TODO: Should we do validation and exit on anything
                # besides update or keepalive?
                # Prepare state for reading another message.
                message_type_hex = self.msg_in[self.header_length]
                if message_type_hex == "\x01":
                    logger.info("OPEN message received: 0x%s",
                                binascii.b2a_hex(self.msg_in))
                elif message_type_hex == "\x02":
                    logger.debug("UPDATE message received: 0x%s",
                                 binascii.b2a_hex(self.msg_in))
                    self.decode_update_message(self.msg_in)
                elif message_type_hex == "\x03":
                    logger.info("NOTIFICATION message received: 0x%s",
                                binascii.b2a_hex(self.msg_in))
                elif message_type_hex == "\x04":
                    logger.info("KEEP ALIVE message received: 0x%s",
                                binascii.b2a_hex(self.msg_in))
                else:
                    logger.warning("Unexpected message received: 0x%s",
                                   binascii.b2a_hex(self.msg_in))
                self.msg_in = ""
                self.reading_header = True
                self.bytes_to_read = self.header_length
        # We should not act upon peer_hold_time if we are reading
        # something right now.
        return

    def decode_path_attributes(self, path_attributes_hex):
        """Decode the Path Attributes field (rfc4271#section-4.3)

        Arguments:
            :path_attributes: path_attributes field to be decoded in hex
        Returns:
            :return: None
        """
        hex_to_decode = path_attributes_hex

        while len(hex_to_decode):
            attr_flags_hex = hex_to_decode[0]
            attr_flags = int(binascii.b2a_hex(attr_flags_hex), 16)
#            attr_optional_bit = attr_flags & 128
#            attr_transitive_bit = attr_flags & 64
#            attr_partial_bit = attr_flags & 32
            attr_extended_length_bit = attr_flags & 16

            attr_type_code_hex = hex_to_decode[1]
            attr_type_code = int(binascii.b2a_hex(attr_type_code_hex), 16)

            if attr_extended_length_bit:
                attr_length_hex = hex_to_decode[2:4]
                attr_length = int(binascii.b2a_hex(attr_length_hex), 16)
                attr_value_hex = hex_to_decode[4:4 + attr_length]
                hex_to_decode = hex_to_decode[4 + attr_length:]
            else:
                attr_length_hex = hex_to_decode[2]
                attr_length = int(binascii.b2a_hex(attr_length_hex), 16)
                attr_value_hex = hex_to_decode[3:3 + attr_length]
                hex_to_decode = hex_to_decode[3 + attr_length:]

            if attr_type_code == 1:
                logger.debug("Attribute type=1 (ORIGIN, flags:0x%s)",
                             binascii.b2a_hex(attr_flags_hex))
                logger.debug("Attribute value=0x%s", binascii.b2a_hex(attr_value_hex))
            elif attr_type_code == 2:
                logger.debug("Attribute type=2 (AS_PATH, flags:0x%s)",
                             binascii.b2a_hex(attr_flags_hex))
                logger.debug("Attribute value=0x%s", binascii.b2a_hex(attr_value_hex))
            elif attr_type_code == 3:
                logger.debug("Attribute type=3 (NEXT_HOP, flags:0x%s)",
                             binascii.b2a_hex(attr_flags_hex))
                logger.debug("Attribute value=0x%s", binascii.b2a_hex(attr_value_hex))
            elif attr_type_code == 4:
                logger.debug("Attribute type=4 (MULTI_EXIT_DISC, flags:0x%s)",
                             binascii.b2a_hex(attr_flags_hex))
                logger.debug("Attribute value=0x%s", binascii.b2a_hex(attr_value_hex))
            elif attr_type_code == 5:
                logger.debug("Attribute type=5 (LOCAL_PREF, flags:0x%s)",
                             binascii.b2a_hex(attr_flags_hex))
                logger.debug("Attribute value=0x%s", binascii.b2a_hex(attr_value_hex))
            elif attr_type_code == 6:
                logger.debug("Attribute type=6 (ATOMIC_AGGREGATE, flags:0x%s)",
                             binascii.b2a_hex(attr_flags_hex))
                logger.debug("Attribute value=0x%s", binascii.b2a_hex(attr_value_hex))
            elif attr_type_code == 7:
                logger.debug("Attribute type=7 (AGGREGATOR, flags:0x%s)",
                             binascii.b2a_hex(attr_flags_hex))
                logger.debug("Attribute value=0x%s", binascii.b2a_hex(attr_value_hex))
            elif attr_type_code == 9:  # rfc4456#section-8
                logger.debug("Attribute type=9 (ORIGINATOR_ID, flags:0x%s)",
                             binascii.b2a_hex(attr_flags_hex))
                logger.debug("Attribute value=0x%s", binascii.b2a_hex(attr_value_hex))
            elif attr_type_code == 10:  # rfc4456#section-8
                logger.debug("Attribute type=10 (CLUSTER_LIST, flags:0x%s)",
                             binascii.b2a_hex(attr_flags_hex))
                logger.debug("Attribute value=0x%s", binascii.b2a_hex(attr_value_hex))
            elif attr_type_code == 14:  # rfc4760#section-3
                logger.debug("Attribute type=14 (MP_REACH_NLRI, flags:0x%s)",
                             binascii.b2a_hex(attr_flags_hex))
                logger.debug("Attribute value=0x%s", binascii.b2a_hex(attr_value_hex))
                address_family_identifier_hex = attr_value_hex[0:2]
                logger.debug("  Address Family Identifier=0x%s",
                             binascii.b2a_hex(address_family_identifier_hex))
                subsequent_address_family_identifier_hex = attr_value_hex[2]
                logger.debug("  Subsequent Address Family Identifier=0x%s",
                             binascii.b2a_hex(subsequent_address_family_identifier_hex))
                next_hop_netaddr_len_hex = attr_value_hex[3]
                next_hop_netaddr_len = int(binascii.b2a_hex(next_hop_netaddr_len_hex), 16)
                logger.debug("  Length of Next Hop Network Address=%s (0x%s)",
                             next_hop_netaddr_len,
                             binascii.b2a_hex(next_hop_netaddr_len_hex))
                next_hop_netaddr_hex = attr_value_hex[4:4 + next_hop_netaddr_len]
                next_hop_netaddr = ".".join(str(i) for i in struct.unpack("BBBB", next_hop_netaddr_hex))
                logger.debug("  Network Address of Next Hop=%s (0x%s)",
                             next_hop_netaddr, binascii.b2a_hex(next_hop_netaddr_hex))
                reserved_hex = attr_value_hex[4 + next_hop_netaddr_len]
                logger.debug("  Reserved=0x%s",
                             binascii.b2a_hex(reserved_hex))
                nlri_hex = attr_value_hex[4 + next_hop_netaddr_len + 1:]
                logger.debug("  Network Layer Reachability Information=0x%s",
                             binascii.b2a_hex(nlri_hex))
                nlri_prefix_list = get_prefix_list_from_hex(nlri_hex)
                logger.debug("  NLRI prefix list: %s", nlri_prefix_list)
                for prefix in nlri_prefix_list:
                    logger.debug("  nlri_prefix_received: %s", prefix)
                self.prefixes_introduced += len(nlri_prefix_list)  # update counter
            elif attr_type_code == 15:  # rfc4760#section-4
                logger.debug("Attribute type=15 (MP_UNREACH_NLRI, flags:0x%s)",
                             binascii.b2a_hex(attr_flags_hex))
                logger.debug("Attribute value=0x%s", binascii.b2a_hex(attr_value_hex))
                address_family_identifier_hex = attr_value_hex[0:2]
                logger.debug("  Address Family Identifier=0x%s",
                             binascii.b2a_hex(address_family_identifier_hex))
                subsequent_address_family_identifier_hex = attr_value_hex[2]
                logger.debug("  Subsequent Address Family Identifier=0x%s",
                             binascii.b2a_hex(subsequent_address_family_identifier_hex))
                wd_hex = attr_value_hex[3:]
                logger.debug("  Withdrawn Routes=0x%s",
                             binascii.b2a_hex(wd_hex))
                wdr_prefix_list = get_prefix_list_from_hex(wd_hex)
                logger.debug("  Withdrawn routes prefix list: %s",
                             wdr_prefix_list)
                for prefix in wdr_prefix_list:
                    logger.debug("  withdrawn_prefix_received: %s", prefix)
                self.prefixes_withdrawn += len(wdr_prefix_list)  # update counter
            else:
                logger.debug("Unknown attribute type=%s, flags:0x%s)", attr_type_code,
                             binascii.b2a_hex(attr_flags_hex))
                logger.debug("Unknown attribute value=0x%s", binascii.b2a_hex(attr_value_hex))
        return None

    def decode_update_message(self, msg):
        """Decode an UPDATE message (rfc4271#section-4.3)

        Arguments:
            :msg: message to be decoded in hex
        Returns:
            :return: None
        """
        logger.debug("Decoding update message:")
        # message header - marker
        marker_hex = msg[:16]
        logger.debug("Message header marker: 0x%s",
                     binascii.b2a_hex(marker_hex))
        # message header - message length
        msg_length_hex = msg[16:18]
        msg_length = int(binascii.b2a_hex(msg_length_hex), 16)
        logger.debug("Message lenght: 0x%s (%s)",
                     binascii.b2a_hex(msg_length_hex), msg_length)
        # message header - message type
        msg_type_hex = msg[18:19]
        msg_type = int(binascii.b2a_hex(msg_type_hex), 16)

        with self.storage as stor:
            # this will replace the previously stored message
            stor['update'] = binascii.hexlify(msg)

        logger.debug("Evpn {}".format(self.evpn))
        if self.evpn:
            logger.debug("Skipping update decoding due to evpn data expected")
            return

        if msg_type == 2:
            logger.debug("Message type: 0x%s (update)",
                         binascii.b2a_hex(msg_type_hex))
            # withdrawn routes length
            wdr_length_hex = msg[19:21]
            wdr_length = int(binascii.b2a_hex(wdr_length_hex), 16)
            logger.debug("Withdrawn routes lenght: 0x%s (%s)",
                         binascii.b2a_hex(wdr_length_hex), wdr_length)
            # withdrawn routes
            wdr_hex = msg[21:21 + wdr_length]
            logger.debug("Withdrawn routes: 0x%s",
                         binascii.b2a_hex(wdr_hex))
            wdr_prefix_list = get_prefix_list_from_hex(wdr_hex)
            logger.debug("Withdrawn routes prefix list: %s",
                         wdr_prefix_list)
            for prefix in wdr_prefix_list:
                logger.debug("withdrawn_prefix_received: %s", prefix)
            # total path attribute length
            total_pa_length_offset = 21 + wdr_length
            total_pa_length_hex = msg[total_pa_length_offset:total_pa_length_offset + 2]
            total_pa_length = int(binascii.b2a_hex(total_pa_length_hex), 16)
            logger.debug("Total path attribute lenght: 0x%s (%s)",
                         binascii.b2a_hex(total_pa_length_hex), total_pa_length)
            # path attributes
            pa_offset = total_pa_length_offset + 2
            pa_hex = msg[pa_offset:pa_offset + total_pa_length]
            logger.debug("Path attributes: 0x%s", binascii.b2a_hex(pa_hex))
            self.decode_path_attributes(pa_hex)
            # network layer reachability information length
            nlri_length = msg_length - 23 - total_pa_length - wdr_length
            logger.debug("Calculated NLRI length: %s", nlri_length)
            # network layer reachability information
            nlri_offset = pa_offset + total_pa_length
            nlri_hex = msg[nlri_offset:nlri_offset + nlri_length]
            logger.debug("NLRI: 0x%s", binascii.b2a_hex(nlri_hex))
            nlri_prefix_list = get_prefix_list_from_hex(nlri_hex)
            logger.debug("NLRI prefix list: %s", nlri_prefix_list)
            for prefix in nlri_prefix_list:
                logger.debug("nlri_prefix_received: %s", prefix)
            # Updating counters
            self.updates_received += 1
            self.prefixes_introduced += len(nlri_prefix_list)
            self.prefixes_withdrawn += len(wdr_prefix_list)
        else:
            logger.error("Unexpeced message type 0x%s in 0x%s",
                         binascii.b2a_hex(msg_type_hex), binascii.b2a_hex(msg))

    def wait_for_read(self):
        """Read message until timeout (next expected event).

        Note:
            Used when no more updates has to be sent to avoid busy-wait.
            Currently it does not return anything.
        """
        # Compute time to the first predictable state change
        event_time = self.timer.get_next_event_time()
        # snapshot_time would be imprecise
        wait_timedelta = min(event_time - time.time(), self.wfr)
        if wait_timedelta < 0:
            # The program got around to waiting to an event in "very near
            # future" so late that it became a "past" event, thus tell
            # "select" to not wait at all. Passing negative timedelta to
            # select() would lead to either waiting forever (for -1) or
            # select.error("Invalid parameter") (for everything else).
            wait_timedelta = 0
        # And wait for event or something to read.

        if not self.rx_activity_detected or not (self.updates_received % 100):
            # right time to write statistics to the log (not for every update and
            # not too frequently to avoid having large log files)
            logger.info("total_received_update_message_counter: %s",
                        self.updates_received)
            logger.info("total_received_nlri_prefix_counter: %s",
                        self.prefixes_introduced)
            logger.info("total_received_withdrawn_prefix_counter: %s",
                        self.prefixes_withdrawn)

        start_time = time.time()
        select.select([self.socket], [], [self.socket], wait_timedelta)
        timedelta = time.time() - start_time
        self.rx_idle_time += timedelta
        self.rx_activity_detected = timedelta < 1

        if not self.rx_activity_detected or not (self.updates_received % 100):
            # right time to write statistics to the log (not for every update and
            # not too frequently to avoid having large log files)
            logger.info("... idle for %.3fs", timedelta)
            logger.info("total_rx_idle_time_counter: %.3fs", self.rx_idle_time)
        return


class WriteTracker(object):
    """Class tracking enqueueing messages and sending chunks of them."""

    def __init__(self, bgp_socket, generator, timer):
        """The writter initialisation.

        Arguments:
            bgp_socket: socket to be used for sending
            generator: generator to be used for message generation
            timer: timer to be used for scheduling
        """
        # References to outside objects,
        self.socket = bgp_socket
        self.generator = generator
        self.timer = timer
        # Really new fields.
        # TODO: Would attribute docstrings add anything substantial?
        self.sending_message = False
        self.bytes_to_send = 0
        self.msg_out = ""

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
        # We assume there is a msg_out to send and socket is writable.
        # print "going to send", repr(self.msg_out)
        self.timer.snapshot()
        bytes_sent = self.socket.send(self.msg_out)
        # Forget the part of message that was sent.
        self.msg_out = self.msg_out[bytes_sent:]
        self.bytes_to_send -= bytes_sent
        if not self.bytes_to_send:
            # TODO: Is it possible to hit negative bytes_to_send?
            self.sending_message = False
            # We should have reset hold timer on peer side.
            self.timer.reset_my_keepalive_time(self.timer.snapshot_time)
            # The possible reason for not prioritizing reads is gone.
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
        # References to outside objects.
        self.socket = bgp_socket
        self.generator = generator
        self.timer = timer
        # Sub-trackers.
        self.reader = ReadTracker(bgp_socket, timer, storage, evpn=cliargs.evpn, wait_for_read=cliargs.wfr)
        self.writer = WriteTracker(bgp_socket, generator, timer)
        # Prioritization state.
        self.prioritize_writing = False
        # In general, we prioritize reading over writing. But in order
        # not to get blocked by neverending reads, we should
        # check whether we are not risking running out of holdtime.
        # So in some situations, this field is set to True to attempt
        # finishing sending a message, after which this field resets
        # back to False.
        # TODO: Alternative is to switch fairly between reading and
        # writing (called round robin from now on).
        # Message counting is done in generator.
        self.inqueue = inqueue

    def perform_one_loop_iteration(self):
        """ The main loop iteration

        Notes:
            Calculates priority, resolves all conditions, calls
            appropriate method and returns to caller to repeat.
        """
        self.timer.snapshot()
        if not self.prioritize_writing:
            if self.timer.is_time_for_my_keepalive():
                if not self.writer.sending_message:
                    # We need to schedule a keepalive ASAP.
                    self.writer.enqueue_message_for_sending(self.generator.keepalive_message())
                    logger.info("KEEP ALIVE is sent.")
                # We are sending a message now, so let's prioritize it.
                self.prioritize_writing = True

        try:
            msg = self.inqueue.get_nowait()
            logger.info("Received message: {}".format(msg))
            msgbin = binascii.unhexlify(msg)
            self.writer.enqueue_message_for_sending(msgbin)
        except Queue.Empty:
            pass
        # Now we know what our priorities are, we have to check
        # which actions are available.
        # socket.socket() returns three lists,
        # we store them to list of lists.
        list_list = select.select([self.socket], [self.socket], [self.socket],
                                  self.timer.report_timedelta)
        read_list, write_list, except_list = list_list
        # Lists are unpacked, each is either [] or [self.socket],
        # so we will test them as boolean.
        if except_list:
            logger.error("Exceptional state on the socket.")
            raise RuntimeError("Exceptional state on socket", self.socket)
        # We will do either read or write.
        if not (self.prioritize_writing and write_list):
            # Either we have no reason to rush writes,
            # or the socket is not writable.
            # We are focusing on reading here.
            if read_list:  # there is something to read indeed
                # In this case we want to read chunk of message
                # and repeat the select,
                self.reader.read_message_chunk()
                return
            # We were focusing on reading, but nothing to read was there.
            # Good time to check peer for hold timer.
            self.timer.check_peer_hold_time(self.timer.snapshot_time)
            # Quiet on the read front, we can have attempt to write.
        if write_list:
            # Either we really want to reset peer's view of our hold
            # timer, or there was nothing to read.
            # Were we in the middle of sending a message?
            if self.writer.sending_message:
                # Was it the end of a message?
                whole = self.writer.send_message_chunk_is_whole()
                # We were pressed to send something and we did it.
                if self.prioritize_writing and whole:
                    # We prioritize reading again.
                    self.prioritize_writing = False
                return
            # Finally to check if still update messages to be generated.
            if self.generator.remaining_prefixes:
                msg_out = self.generator.compose_update_message()
                if not self.generator.remaining_prefixes:
                    # We have just finished update generation,
                    # end-of-rib is due.
                    logger.info("All update messages generated.")
                    logger.info("Storing performance results.")
                    self.generator.store_results()
                    logger.info("Finally an END-OF-RIB is sent.")
                    msg_out += self.generator.update_message(wr_prefixes=[],
                                                             nlri_prefixes=[],
                                                             end_of_rib=True)
                self.writer.enqueue_message_for_sending(msg_out)
                # Attempt for real sending to be done in next iteration.
                return
            # Nothing to write anymore.
            # To avoid busy loop, we do idle waiting here.
            self.reader.wait_for_read()
            return
        # We can neither read nor write.
        logger.warning("Input and output both blocked for " +
                       str(self.timer.report_timedelta) + " seconds.")
        # FIXME: Are we sure select has been really waiting
        # the whole period?
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
    log_formatter = logging.Formatter("%(asctime)s %(levelname)s BGP-%(threadName)s: %(message)s")
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
    Notes:
        Establish BGP connection and run iterations.

    Arguments:
        :arguments: Command line arguments
        :inqueue: Data to be sent from play.py
        :storage: Shared dict for rpc server
    Returns:
        :return: None
    """
    bgp_socket = establish_connection(arguments)
    # Initial handshake phase. TODO: Can it be also moved to StateTracker?
    # Receive open message before sending anything.
    # FIXME: Add parameter to send default open message first,
    # to work with "you first" peers.
    msg_in = read_open_message(bgp_socket)
    timer = TimeTracker(msg_in)
    generator = MessageGenerator(arguments)
    msg_out = generator.open_message()
    logger.debug("Sending the OPEN message: " + binascii.hexlify(msg_out))
    # Send our open message to the peer.
    bgp_socket.send(msg_out)
    # Wait for confirming keepalive.
    # TODO: Surely in just one packet?
    # Using exact keepalive length to not to see possible updates.
    msg_in = bgp_socket.recv(19)
    if msg_in != generator.keepalive_message():
        error_msg = "Open not confirmed by keepalive, instead got"
        logger.error(error_msg + ": " + binascii.hexlify(msg_in))
        raise MessageError(error_msg, msg_in)
    timer.reset_peer_hold_time()
    # Send the keepalive to indicate the connection is accepted.
    timer.snapshot()  # Remember this time.
    msg_out = generator.keepalive_message()
    logger.debug("Sending a KEEP ALIVE message: " + binascii.hexlify(msg_out))
    bgp_socket.send(msg_out)
    # Use the remembered time.
    timer.reset_my_keepalive_time(timer.snapshot_time)
    # End of initial handshake phase.
    state = StateTracker(bgp_socket, generator, timer, inqueue, storage, arguments)
    while True:  # main reactor loop
        state.perform_one_loop_iteration()


class Rpcs:
    '''Handler for SimpleXMLRPCServer'''

    def __init__(self, sendqueue, storage):
        '''Init method

        Arguments:
            :sendqueue: queue for data to be sent towards odl
            :storage: thread safe dict
        '''
        self.queue = sendqueue
        self.storage = storage

    def send(self, text):
        '''Data to be sent

        Arguments:
            :text: hes string of the data to be sent
        '''
        self.queue.put(text)

    def get(self, text=''):
        '''Reads data form the storage

        - returns stored data or an empty string, at the moment only
          'update' is stored

        Arguments:
            :text: a key to the storage to get the data
        Returns:
            :data: stored data
        '''
        with self.storage as stor:
            return stor.get(text, '')

    def clean(self, text=''):
        '''Cleans data form the storage

        Arguments:
            :text: a key to the storage to clean the data
        '''
        with self.storage as stor:
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
    thread_args = []
    rpcqueue = Queue.Queue()
    storage = SafeDict()

    while 1:
        amount_per_util = (amount_left - 1) / utils_left + 1  # round up
        amount_left -= amount_per_util
        utils_left -= 1

        args = deepcopy(arguments)
        args.amount = amount_per_util
        args.firstprefix = prefix_current
        args.myip = myip_current
        thread_args.append(args)

        if not utils_left:
            break
        prefix_current += amount_per_util * 16
        myip_current += 1

    try:
        # Create threads
        for t in thread_args:
            thread.start_new_thread(job, (t, rpcqueue, storage))
    except Exception:
        print "Error: unable to start thread."
        raise SystemExit(2)

    rpcserver = SimpleXMLRPCServer((arguments.myip.compressed, 8000), allow_none=True)
    rpcserver.register_instance(Rpcs(rpcqueue, storage))
    rpcserver.serve_forever()


if __name__ == "__main__":
    arguments = parse_arguments()
    logger = create_logger(arguments.loglevel, arguments.logfile)
    threaded_job(arguments)
