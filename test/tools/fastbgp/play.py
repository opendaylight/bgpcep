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

__author__ = "Vratko Polak"
__copyright__ = "Copyright(c) 2015, Cisco Systems, Inc."
__license__ = "Eclipse Public License v1.0"
__email__ = "vrpolak@cisco.com"


import argparse
import binascii
import ipaddr
import select
import socket
import time


def parse_arguments():
    """Use argparse to get arguments, return args object."""
    parser = argparse.ArgumentParser()
    # TODO: Should we use --argument-names-with-spaces?
    str_help = "Autonomous System number use in the stream (current default as in ODL: 64496)."
    parser.add_argument("--asnumber", default=64496, type=int, help=str_help)
    # FIXME: We are acting as iBGP peer, we should mirror AS number from peer's open message.
    str_help = "Amount of IP prefixes to generate. Negative number is taken as overflown positive."
    parser.add_argument("--amount", default="1", type=int, help=str_help)
    str_help = "The first IPv4 prefix to announce, given as numeric IPv4 address."
    parser.add_argument("--firstprefix", default="8.0.1.0", type=ipaddr.IPv4Address, help=str_help)
    str_help = "If present, this tool will be listening for connection, instead of initiating it."
    parser.add_argument("--listen", action="store_true", help=str_help)
    str_help = "Numeric IP Address to bind to and derive BGP ID from. Default value only suitable for listening."
    parser.add_argument("--myip", default="0.0.0.0", type=ipaddr.IPv4Address, help=str_help)
    str_help = "TCP port to bind to when listening or initiating connection. Default only suitable for initiating."
    parser.add_argument("--myport", default="0", type=int, help=str_help)
    str_help = "The IP of the next hop to be placed into the update messages."
    parser.add_argument("--nexthop", default="192.0.2.1", type=ipaddr.IPv4Address, dest="nexthop", help=str_help)
    str_help = "Numeric IP Address to try to connect to. Currently no effect in listening mode."
    parser.add_argument("--peerip", default="127.0.0.2", type=ipaddr.IPv4Address, help=str_help)
    str_help = "TCP port to try to connect to. No effect in listening mode."
    parser.add_argument("--peerport", default="179", type=int, help=str_help)
    # TODO: The step between IP prefixes is currently hardcoded to 16. Should we make it configurable?
    # Yes, the argument list above is sorted alphabetically.
    arguments = parser.parse_args()
    # TODO: Are sanity checks (such as asnumber>=0) required?
    return arguments


def establish_connection(arguments):
    """Establish connection according to arguments, return socket."""
    if arguments.listen:
        # print "DEBUG: connecting in the listening case."
        listening_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        listening_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        listening_socket.bind((str(arguments.myip), arguments.myport))  # bind need single tuple as argument
        listening_socket.listen(1)
        bgp_socket, _ = listening_socket.accept()
        # TODO: Verify client IP is cotroller IP.
        listening_socket.close()
    else:
        # print "DEBUG: connecting in the talking case."
        talking_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        talking_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        talking_socket.bind((str(arguments.myip), arguments.myport))  # bind to force specified address and port
        talking_socket.connect((str(arguments.peerip), arguments.peerport))  # socket does not spead ipaddr, hence str()
        bgp_socket = talking_socket
    print "Connected to ODL."
    return bgp_socket


def get_short_int_from_message(message, offset=16):
    """Extract 2-bytes number from packed string, default offset is for BGP message size."""
    high_byte_int = ord(message[offset])
    low_byte_int = ord(message[offset + 1])
    short_int = high_byte_int * 256 + low_byte_int
    return short_int


class MessageError(ValueError):
    """Value error with logging optimized for hexlified messages."""

    def __init__(self, text, message, *args):
        """Store and call super init for textual comment, store raw message which caused it."""
        self.text = text
        self.msg = message
        super(MessageError, self).__init__(text, message, *args)

    def __str__(self):
        """
        Generate human readable error message

        Concatenate text comment, colon with space
        and hexlified message. Use a placeholder string
        if the message turns out to be empty.
        """
        message = binascii.hexlify(self.msg)
        if message == "":
            message = "(empty message)"
        return self.text + ": " + message


def read_open_message(bgp_socket):
    """Receive message, perform some validation, return the raw message."""
    msg_in = bgp_socket.recv(65535)  # TODO: Is smaller buffer size safe?
    # TODO: Is it possible for incoming open message to be split in more than one packet?
    # Some validation.
    if len(msg_in) < 37:  # 37 is minimal length of open message with 4-byte AS number.
        raise MessageError("Got something else than open with 4-byte AS number", msg_in)
    # TODO: We could check BGP marker, but it is defined only later; decide what to do.
    reported_length = get_short_int_from_message(msg_in)
    if len(msg_in) != reported_length:
        raise MessageError("Message length is not " + reported_length + " in message", msg_in)
    print "Open message received"
    return msg_in


class MessageGenerator(object):
    """Class with methods returning messages and state holding configuration data required to do it properly."""

    # TODO: Define bgp marker as class (constant) variable.
    def __init__(self, args):
        """Initialize data according to command-line args."""
        # Various auxiliary variables.
        # Hack: 4-byte AS number uses the same "int to packed" encoding as IPv4 addresses.
        asnumber_4bytes = ipaddr.v4_int_to_packed(args.asnumber)
        asnumber_2bytes = "\x5b\xa0"  # AS_TRANS value, 23456 decadic.
        if args.asnumber < 65536:  # AS number is mappable to 2 bytes
            asnumber_2bytes = asnumber_4bytes[2:4]
        # From now on, attribute docsrings are used.
        self.int_nextprefix = int(args.firstprefix)
        """Prefix IP address for next update message, as integer."""
        self.updates_to_send = args.amount
        """Number of update messages left to be sent."""
        # All information ready, so we can define messages. Mostly copied from play.py by Jozef Behran.
        # The following attributes are constant.
        self.bgp_marker = "\xFF" * 16
        """Every message starts with this, see rfc4271#section-4.1"""
        self.keepalive_message = self.bgp_marker + (
            "\x00\x13"  # Size
            "\x04"  # Type KEEPALIVE
        )
        """KeepAlive message, see rfc4271#section-4.4"""
        # TODO: Notification for hold timer expiration can be handy.
        self.eor_message = self.bgp_marker + (
            "\x00\x17"  # Size
            "\x02"  # Type (UPDATE)
            "\x00\x00"  # Withdrawn routes length (0)
            "\x00\x00"  # Total Path Attributes Length (0)
        )
        """End-of-RIB marker, see rfc4724#section-2"""
        self.update_message_without_prefix = self.bgp_marker + (
            "\x00\x30"  # Size
            "\x02"  # Type (UPDATE)
            "\x00\x00"  # Withdrawn routes length (0)
            "\x00\x14"  # Total Path Attributes Length (20)
            "\x40"  # Flags ("Well-Known")
            "\x01"  # Type (ORIGIN)
            "\x01"  # Length (1)
            "\x00"  # Origin: IGP
            "\x40"  # Flags ("Well-Known")
            "\x02"  # Type (AS_PATH)
            "\x06"  # Length (6)
            "\x02"  # AS segment type (AS_SEQUENCE)
            "\x01"  # AS segment length (1)
            + asnumber_4bytes +  # AS segment (4 bytes)
            "\x40"  # Flags ("Well-Known")
            "\x03"  # Type (NEXT_HOP)
            "\x04"  # Length (4)
            + args.nexthop.packed +  # IP address of the next hop (4 bytes)
            "\x1c"  # IPv4 prefix length, see RFC 4271, page 20. This tool uses Network Mask: 255.255.255.240
        )
        """The IP address prefix (4 bytes) has to be appended to complete Update message, see rfc4271#section-4.3."""
        self.open_message = self.bgp_marker + (
            "\x00\x2d"  # Size
            "\x01"  # Type (OPEN)
            "\x04"  # BGP Varsion (4)
            + asnumber_2bytes +  # My Autonomous System
            # FIXME: The following hold time is hardcoded separately. Compute from initial hold_time value.
            "\x00\xb4"  # Hold Time (180)
            + args.myip.packed +  # BGP Identifer
            "\x10"  # Optional parameters length
            "\x02"  # Param type ("Capability Ad")
            "\x06"  # Length (6 bytes)
            "\x01"  # Capability type (NLRI Unicast), see RFC 4760, secton 8
            "\x04"  # Capability value length
            "\x00\x01"  # AFI (Ipv4)
            "\x00"  # (reserved)
            "\x01"  # SAFI (Unicast)
            "\x02"  # Param type ("Capability Ad")
            "\x06"  # Length (6 bytes)
            "\x41"  # "32 bit AS Numbers Support" (see RFC 6793, section 3)
            "\x04"  # Capability value length
            + asnumber_4bytes  # My AS in 32 bit format
        )
        """Open message, see rfc4271#section-4.2"""
        # __init__ ends

    def compose_update_message(self):
        """Return update message, prepare next prefix, decrease amount without checking it."""
        prefix_packed = ipaddr.v4_int_to_packed(self.int_nextprefix)
        # print "DEBUG: prefix", self.int_nextprefix, "packed to", binascii.hexlify(prefix_packed)
        msg_out = self.update_message_without_prefix + prefix_packed
        self.int_nextprefix += 16  # Hardcoded, as open message specifies such netmask.
        self.updates_to_send -= 1
        return msg_out


class TimeTracker(object):
    """Class for tracking timers, both for my keepalives and peer's hold time."""

    def __init__(self, msg_in):
        """Initialize config, based on hardcoded defaults and open message from peer."""
        # Note: Relative time is always named timedelta, to stress that (non-delta) time is absolute.
        self.report_timedelta = 1.0  # In seconds. TODO: Configurable?
        """Upper bound for being stuck in the same state, we should at least report something before continuing."""
        # Negotiate the hold timer by taking the smaller of the 2 values (mine and the peer's).
        hold_timedelta = 180  # Not an attribute of self yet.
        # TODO: Make the default value configurable, default value could mirror what peer said.
        peer_hold_timedelta = get_short_int_from_message(msg_in, offset=22)
        if hold_timedelta > peer_hold_timedelta:
            hold_timedelta = peer_hold_timedelta
        if hold_timedelta != 0 and hold_timedelta < 3:
            raise ValueError("Invalid hold timedelta value: ", hold_timedelta)
        self.hold_timedelta = hold_timedelta  # only now the final value is visible from outside
        """If we do not hear from peer this long, we assume it has died."""
        self.keepalive_timedelta = int(hold_timedelta / 3.0)
        """Upper limit for duration between messages, to avoid being declared dead."""
        self.snapshot_time = time.time()  # The same as calling snapshot(), but also declares a field.
        """Sometimes we need to store time. This is where to get the value from afterwards."""
        self.peer_hold_time = self.snapshot_time + self.hold_timedelta  # time_keepalive may be too strict
        """At this time point, peer will be declared dead."""
        self.my_keepalive_time = None  # to be set later
        """At this point, we should be sending keepalive message."""

    def snapshot(self):
        """Store current time in instance data to use later."""
        self.snapshot_time = time.time()  # Read as time before something interesting was called.

    def reset_peer_hold_time(self):
        """Move hold time to future as peer has just proven it still lives."""
        self.peer_hold_time = time.time() + self.hold_timedelta

    # Some methods could rely on self.snapshot_time, but it is better to require user to provide it explicitly.
    def reset_my_keepalive_time(self, keepalive_time):
        """Move KA timer to future based on given time from before sending."""
        self.my_keepalive_time = keepalive_time + self.keepalive_timedelta

    def is_time_for_my_keepalive(self):
        if self.hold_timedelta == 0:
            return False
        return self.snapshot_time >= self.my_keepalive_time

    def get_next_event_time(self):
        if self.hold_timedelta == 0:
            return self.snapshot_time + 86400
        return min(self.my_keepalive_time, self.peer_hold_time)

    def check_peer_hold_time(self, snapshot_time):
        """Raise error if nothing was read from peer until specified time."""
        if self.hold_timedelta != 0:  # Hold time = 0 means keepalive checking off.
            if snapshot_time > self.peer_hold_time:  # time.time() may be too strict
                raise RuntimeError("Peer has overstepped the hold timer.")  # TODO: Include hold_timedelta?
                # TODO: Add notification sending (attempt). That means move to write tracker.


class ReadTracker(object):
    """Class for tracking read of mesages chunk by chunk and for idle waiting."""

    def __init__(self, bgp_socket, timer):
        """Set initial state."""
        # References to outside objects.
        self.socket = bgp_socket
        self.timer = timer
        # Really new fields.
        self.header_length = 18
        """BGP marker length plus length field length."""  # TODO: make it class (constant) attribute
        self.reading_header = True
        """Computation of where next chunk ends depends on whether we are beyond length field."""
        self.bytes_to_read = self.header_length
        """Countdown towards next size computation."""
        self.msg_in = ""
        """Incremental buffer for message under read."""

    def read_message_chunk(self):
        """Read up to one message, do not return anything."""
        # TODO: We also could return the whole message, but currently nobody cares.
        # We assume the socket is readable.
        chunk_message = self.socket.recv(self.bytes_to_read)
        self.msg_in += chunk_message
        self.bytes_to_read -= len(chunk_message)
        if not self.bytes_to_read:  # TODO: bytes_to_read < 0 is not possible, right?
            # Finished reading a logical block.
            if self.reading_header:
                # The logical block was a BGP header. Now we know size of message.
                self.reading_header = False
                self.bytes_to_read = get_short_int_from_message(self.msg_in)
            else:  # We have finished reading the body of the message.
                # Peer has just proven it is still alive.
                self.timer.reset_peer_hold_time()
                # TODO: Do we want to count received messages?
                # This version ignores the received message.
                # TODO: Should we do validation and exit on anything besides update or keepalive?
                # Prepare state for reading another message.
                self.msg_in = ""
                self.reading_header = True
                self.bytes_to_read = self.header_length
        # We should not act upon peer_hold_time if we are reading something right now.
        return

    def wait_for_read(self):
        """When we know there are no more updates to send, we use this to avoid busy-wait."""
        # First, compute time to first predictable state change (or report event)
        event_time = self.timer.get_next_event_time()
        wait_timedelta = event_time - time.time()  # snapshot_time would be imprecise
        if wait_timedelta < 0:
            # The program got around to waiting to an event in "very near
            # future" so late that it became a "past" event, thus tell
            # "select" to not wait at all. Passing negative timedelta to
            # select() would lead to either waiting forever (for -1) or
            # select.error("Invalid parameter") (for everything else).
            wait_timedelta = 0
        # And wait for event or something to read.
        select.select([self.socket], [], [self.socket], wait_timedelta)
        # Not checking anything, that will be done in next iteration.
        return


class WriteTracker(object):
    """Class tracking enqueueing messages and sending chunks of them."""

    def __init__(self, bgp_socket, generator, timer):
        """Set initial state."""
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
        """Change write state to include the message."""
        self.msg_out += message
        self.bytes_to_send += len(message)
        self.sending_message = True

    def send_message_chunk_is_whole(self):
        """Perform actions related to sending (chunk of) message, return whether message was completed."""
        # We assume there is a msg_out to send and socket is writable.
        # print "going to send", repr(self.msg_out)
        self.timer.snapshot()
        bytes_sent = self.socket.send(self.msg_out)
        self.msg_out = self.msg_out[bytes_sent:]  # Forget the part of message that was sent.
        self.bytes_to_send -= bytes_sent
        if not self.bytes_to_send:
            # TODO: Is it possible to hit negative bytes_to_send?
            self.sending_message = False
            # We should have reset hold timer on peer side.
            self.timer.reset_my_keepalive_time(self.timer.snapshot_time)
            # Which means the possible reason for not prioritizing reads is gone.
            return True
        return False


class StateTracker(object):
    """Main loop has state so complex it warrants this separate class."""

    def __init__(self, bgp_socket, generator, timer):
        """Set the initial state according to existing socket and generator."""
        # References to outside objects.
        self.socket = bgp_socket
        self.generator = generator
        self.timer = timer
        # Sub-trackers.
        self.reader = ReadTracker(bgp_socket, timer)
        self.writer = WriteTracker(bgp_socket, generator, timer)
        # Prioritization state.
        self.prioritize_writing = False
        """
        In general, we prioritize reading over writing. But in order to not get blocked by neverending reads,
        we should check whether we are not risking running out of holdtime.
        So in some situations, this field is set to True to attempt finishing sending a message,
        after which this field resets back to False.
        """
        # TODO: Alternative is to switch fairly between reading and writing (called round robin from now on).
        # Message counting is done in generator.

    def perform_one_loop_iteration(self):
        """Calculate priority, resolve all ifs, call appropriate method, return to caller to repeat."""
        self.timer.snapshot()
        if not self.prioritize_writing:
            if self.timer.is_time_for_my_keepalive():
                if not self.writer.sending_message:
                    # We need to schedule a keepalive ASAP.
                    self.writer.enqueue_message_for_sending(self.generator.keepalive_message)
                # We are sending a message now, so prioritize finishing it.
                self.prioritize_writing = True
        # Now we know what our priorities are, we have to check which actions are available.
        # socket.socket() returns three lists, we store them to list of lists.
        list_list = select.select([self.socket], [self.socket], [self.socket], self.timer.report_timedelta)
        read_list, write_list, except_list = list_list
        # Lists are unpacked, each is either [] or [self.socket], so we will test them as boolean.
        if except_list:
            raise RuntimeError("Exceptional state on socket", self.socket)
        # We will do either read or write.
        if not (self.prioritize_writing and write_list):
            # Either we have no reason to rush writes, or the socket is not writable.
            # We are focusing on reading here.
            if read_list:  # there is something to read indeed
                # In this case we want to read chunk of message and repeat the select,
                self.reader.read_message_chunk()
                return
            # We were focusing on reading, but nothing to read was there.
            # Good time to check peer for hold timer.
            self.timer.check_peer_hold_time(self.timer.snapshot_time)
            # Things are quiet on the read front, we can go on and attempt to write.
        if write_list:
            # Either we really want to reset peer's view of our hold timer, or there was nothing to read.
            if self.writer.sending_message:  # We were in the middle of sending a message.
                whole = self.writer.send_message_chunk_is_whole()  # Was it the end of a message?
                if self.prioritize_writing and whole:  # We were pressed to send something and we did it.
                    self.prioritize_writing = False  # We prioritize reading again.
                return
            # Finally, we can look if there is some update message for us to generate.
            if self.generator.updates_to_send:
                msg_out = self.generator.compose_update_message()
                if not self.generator.updates_to_send:  # We have just finished update generation, end-of-rib is due.
                    msg_out += self.generator.eor_message
                self.writer.enqueue_message_for_sending(msg_out)
                return  # Attempt for the actual sending will be done in next iteration.
            # Nothing to write anymore, except occasional keepalives.
            # To avoid busy loop, we do idle waiting here.
            self.reader.wait_for_read()
            return
        # We can neither read nor write.
        print "Input and output both blocked for", self.timer.report_timedelta, "seconds."
        # FIXME: Are we sure select has been really waiting the whole period?
        return


def main():
    """Establish BGP connection and enter main loop for sending updates."""
    arguments = parse_arguments()
    bgp_socket = establish_connection(arguments)
    # Initial handshake phase. TODO: Can it be also moved to StateTracker?
    # Receive open message before sending anything.
    # FIXME: Add parameter to send default open message first, to work with "you first" peers.
    msg_in = read_open_message(bgp_socket)
    timer = TimeTracker(msg_in)
    generator = MessageGenerator(arguments)
    msg_out = generator.open_message
    # print "DEBUG: going to send open:", binascii.hexlify(msg_out)
    # Send our open message to the peer.
    bgp_socket.send(msg_out)
    # Wait for confirming keepalive.
    # TODO: Surely in just one packet?
    msg_in = bgp_socket.recv(19)  # Using exact keepalive length to not see possible updates.
    if msg_in != generator.keepalive_message:
        raise MessageError("Open not confirmed by keepalive, instead got", msg_in)
    timer.reset_peer_hold_time()
    # Send the keepalive to indicate the connection is accepted.
    timer.snapshot()  # Remember this time.
    bgp_socket.send(generator.keepalive_message)
    timer.reset_my_keepalive_time(timer.snapshot_time)  # Use the remembered time.
    # End of initial handshake phase.
    state = StateTracker(bgp_socket, generator, timer)
    while True:  # main reactor loop
        state.perform_one_loop_iteration()

if __name__ == "__main__":
    main()
