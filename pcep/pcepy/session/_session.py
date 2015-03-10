# Session implementation

# Copyright (c) 2012,2013 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import errno
import os
import socket as _socket
try:
    import queue
except ImportError:
    import Queue as queue
import time
import weakref

from .bus import resolve_timeout
from pcepy import message as _message

# Standard assigned port for PCEP
PCEP_PORT = 4189

import logging
_LOGGER = logging.getLogger('pcepy.session')


class Node(object):
    """A representation for an IP address with optional port;
    giving it a role and a name. End point of a connection.

    This class does not maky any assumption on the type of the address field,
    except that two addresses should compare equal even if originating
    from different representations (e.g. created from a configuration string,
    from a socket's address or message field).

    It is therefore client's responsibility to convert all addresses into
    compatible type(s) before use.
    """

    ROLE_PCE = 'role.pce'  # The node is a PCE (server or open connection)
    ROLE_PCC = 'role.pcc'  # The node is a PCC (client or open connection)

    PORT_SUP = 1 << 16

    @staticmethod
    def from_string(role, string, name=None):
        "Return node from the given string in form [name=]address[@port]"
        if '=' in string:
            name, string = string.split('=', 1)
        if '@' in string:
            address, port = string.split('@', 1)
        else:
            address = string
            port = None
        return Node(role, name, address, port)

    def __init__(self, role, name, address, port=None):

        # the role for this node
        self.role = role

        # distinctive name for the node
        self.name = name

        # IP address where it is reachable
        self.address = address

        # Specific port (E.g. for server or open connections)
        if port is not None and not (0 <= port <= Node.PORT_SUP):
            raise ValueError('Port %s outside range' % port)
        self.port = port

    def __str__(self):
        return '%s|%s@%s' % (self.name, self.address, self.port)

    def __eq__(self, other):
        return (self.role == other.role
                and self.name == other.name
                and self.address == other.address
                and self.port == other.port)

    def __ne__(self, other):
        return not self.__eq__(other)

    def with_port(self, port):
        """"Return a copy of node with port changed."""
        if port == self.port:
            return self
        return Node(self.role, self.name, self.address, port)


# Value for EMIT_RESULT cancelling the action implied by calling emit, if any
CANCEL_EVENT = object()


class Session(object):
    """Base class for sessions (socket wrappers) managed by the session bus.
    It also stores for its peer any configuration and data related to it.

    A Session emits these events to its peer:
        on_open(session):
            Called just after the bus started managing this session and the
            session set up its local side successfully.

        on_socket_exceptional(session):
            Session's socket got reported as exceptional by underlying select.

        on_socket_error(session, error, action):
            A socket error occurred while doing action.
            Actions are open, connect, accept, read, send.

        on_timeout(session, now):
            Peer-defined timeout has passed for this session.
            Note that on_timeout will be called for all handlers as any event.
    """

    CONFIG_NAME = 'session.name'
    STATE_CLOSING = '_session.closing'

    _socket = None  # Subclasses must set socket for each instance

    def __init__(self, peer, local, config):
        self._peer = weakref.ref(peer)
        self._local = local
        self._config = config or dict()
        self._update_name()
        self._bus = None
        self._error = None

    @property
    def peer(self):
        return self._peer()

    @property
    def local(self):
        """Local node"""
        return self._local

    @property
    def config(self):
        """Any configuration and data related to this session"""
        return self._config

    @property
    def name(self):
        return self._name

    @name.setter
    def name(self, name):
        self._name = name

    @property
    def bus(self):
        if self._bus in (None, False):
            return self._bus
        return self._bus()

    @property
    def closing(self):
        return self._config.get(Session.STATE_CLOSING)

    @closing.setter
    def closing(self, value):
        self._config[Session.STATE_CLOSING] = value

    @property
    def error(self):
        """Return last socket error"""
        return self._error

    def _update_name(self):
        """Update name to reflect change in node port(s)"""
        self._name = (self[Session.CONFIG_NAME] or
                      '%s[%s/%s]' % (self.__class__.__name__, self.peer, self._local))

    def is_server(self):
        """Return if this is a server socket session"""
        return False

    def open(self, bus):
        """Called from the Bus just after this session is added to it"""
        self._bus = weakref.ref(bus)
        self._socket.setblocking(False)
        self.peer.emit('on_open', session=self)

    def close(self):
        """Called from the Bus after on_close returns a False value."""
        if self.peer.emit('on_close', session=self) is not CANCEL_EVENT:
            if self._socket:
                self._socket.close()
            self.bus.remove(self)

    def fileno(self):
        """The file descriptor associated with this session"""
        return self._socket.fileno()

    def on_exception(self):
        """Called from the bus when the socket gets into 'exceptional state'."""
        self.peer.emit('on_socket_exceptional', session=self)

    def on_readable(self):
        """Called from the Bus whenever there is data available"""
        pass

    def on_writable(self):
        """Called from the Bus if the connection is writable and we want_write"""
        pass

    def on_timeout(self, now):
        """Called from the Bus if the connection timed out"""
        self.peer.emit('on_timeout', session=self, now=now)

    def timeout(self):
        """Return a timeout - latest time point to call on_timeout.
        Delegated to the peer."""
        return self.peer.timeout(self)

    def want_close(self):
        """Return whether this session is to be closed."""
        return self[Session.STATE_CLOSING]

    def want_write(self):
        """Return whether this end has data ready for writing"""
        return False

    def __str__(self):
        return self.name

    def get(self, key, default=None):
        """Convenience access to config dict."""
        return self._config.get(key, default)

    def __getitem__(self, key):
        """Convenience access to config dict.
        Returns None instead of raising KeyError.
        """
        return self._config.get(key)

    def __setitem__(self, key, value):
        """Convenience access to config dict."""
        self._config[key] = value

    def __delitem__(self, key):
        """Convenience access to config dict."""
        try:
            del self._config[key]
        except KeyError:
            pass

    def __contains__(self, key):
        """Convenience access to config dict."""
        return key in self._config


class PcepServer(Session):
    """Manages the PCE server socket.
    A PcepServer emits these events to its peer:
        on_connection(server, socket, address):
            A new connection was accepted from address with a client socket.
    """

    CONFIG_MAX_CLIENTS = 'pcep_server.max_clients'

    MAX_CLIENTS = 5

    def is_server(self):
        return True

    def open(self, bus):
        address = self._local.address
        port = self._local.port or PCEP_PORT
        try:
            string_address = self.peer.context.address_to_str(address)
            if self.peer.context.address_is_ipv6(address):
                family = _socket.AF_INET6
            else:
                family = _socket.AF_INET
            socket_address = _socket.getaddrinfo(
                string_address, port, family, _socket.SOCK_STREAM
            )[0][-1]

            self._socket = _socket.socket(family, _socket.SOCK_STREAM)
            self._socket.bind(socket_address)
            self._socket.listen(
                self._config.get(PcepServer.CONFIG_MAX_CLIENTS, self.MAX_CLIENTS)
            )
        except (_socket.error, ValueError, IndexError) as error:
            self._error = error
            if self.peer.emit('on_socket_error',
                              session=self,
                              error=error,
                              action='open') is not CANCEL_EVENT:
                self.closing = True
        super(PcepServer, self).open(bus)

    def on_readable(self):
        try:
            accept_socket, address = self._socket.accept()
        except _socket.error as error:
            self._error = error
            self.peer.emit('on_socket_error',
                           session=self,
                           error=error,
                           action='accept')
            return

        self.peer.emit('on_connection',
                       server=self,
                       socket=accept_socket,
                       address=address)


class _PcepSession(Session):
    """Manages either end of a PCEP session.
    A PcepSession emits these events to its peer:
        on_connect(session):
            Session to the remote node was established.

        on_transmit(session, message):
            A message is about to be serialized and transmitted.

        on_transmitted(session, message):
            A message was transmitted.

        on_message(session, message):
            A message has arrived.

        on_bad_header(session, header):
            A message header with invalid length or PCEP version was received.
            Handlers may adjust the values in the header before continuing.

        on_message_error(session, message, error, action):
            Message read or write (action=send) raised exception.
    """

    def __init__(self, peer, local, socket, remote, config):
        self._remote = remote
        super(_PcepSession, self).__init__(peer, local, config)
        self._socket = socket
        self._established = False

        # Buffer for incoming single message, contains at most _max_read bytes
        self.__incoming = bytearray()

        # Message in transmit
        self.__message = None

        # Dummy header for reading initial data
        self._header = _message.base.MessageHeader()

        # How many bytes to grow __incoming to before calling _read_message
        self.__want_read = self._header.size

        # Buffer for outgoing single message
        self.__outgoing = bytearray()

        # Message queues (_inbox may be superfluous)
        self._inbox = queue.Queue()
        self._outbox = queue.Queue()

        # Unix times
        now = time.time()
        self._last_received = now
        self._last_sent = now

    @property
    def established(self):
        return self._established

    @property
    def last_received(self):
        return self._last_received

    @property
    def last_sent(self):
        return self._last_sent

    @property
    def remote(self):
        """Remote node"""
        return self._remote

    def send(self, message):
        """Called by peer to send the message"""
        if self.closing:
            _LOGGER.warning('%s: Sending %s message <%s> to a closing session' % (
                            self, message.__class__.__name__, id(message)))
        else:
            _LOGGER.info('%s: Sending %s message <%s>' % (
                         self, message.__class__.__name__, id(message)))
        self._outbox.put(message)
        self.bus.hail()

    def on_readable(self):
        try:
            self.__incoming += self._socket.recv(
                self.__want_read - len(self.__incoming)
            )
        except _socket.error as error:
            self._error = error
            self.peer.emit('on_socket_error', session=self, error=error, action='read')
            self.closing = True
            return

        if len(self.__incoming) == self.__want_read:
            self._read_message()

    def on_writable(self):
        if not self.__outgoing:
            try:
                message = self._outbox.get_nowait()
            except queue.Empty:
                _LOGGER.error('Session %s wants write while nothing to write' % self)
                return

            if self.peer.emit('on_transmit', session=self, message=message) is CANCEL_EVENT:
                return

            transmission = _message.Transmission(session=self)
            self._last_sent = transmission.time
            message.transmission = transmission

            self.__message = message
            size = message.size
            self.__outgoing = bytearray(size)
            try:
                written = message.write(self.__outgoing, 0)
            except Exception as error:
                _LOGGER.exception('%s: Cannot serialize message <%s>: %s' % (
                                  self, id(message), error))
                self.peer.emit('on_message_error', session=self, message=message, error=error, action='send')
                del self.__outgoing[:]
                self.__message = None
                return

            if written != size:
                _LOGGER.error(
                    '%s: Message <%s> written size %s != reserved size %s'
                    % (self, id(message), written, size)
                )
            _LOGGER.debug('%s: Message <%s> written to <%s>[%s]: %s' % (
                          self, id(message), id(self.__outgoing), len(self.__outgoing),
                          _message.data.to_hex(self.__outgoing)))

        try:
            written = self._socket.send(self.__outgoing)
        except _socket.error as error:
            self._error = error
            self.peer.emit('on_socket_error', session=self, error=error, action='send')
            self.closing = True
            return
        del self.__outgoing[:written]

        if not self.__outgoing:
            self._outbox.task_done()
            self.peer.emit('on_transmitted', session=self, message=self.__message)
            self.__message = None

    def want_close(self):
        return (self._error or (not self.want_write() and super(_PcepSession, self).want_close()))

    def want_write(self):
        return bool(self.__outgoing) or not self._outbox.empty()

    def close(self):
        if self.want_write():
            _LOGGER.warning('%s: closing with %s bytes and %s messages to write' % (
                            self, len(self.__outgoing), self._outbox.qsize()))
        super(_PcepSession, self).close()

    def _read_header(self, header):
        """Read header and adjust want_read for the rest of message."""
        header.read(self.__incoming, 0, len(self.__incoming))
        length = header.length
        if (header.version != _message.code.PCEP_VERSION or not length or length % 4):
            if self.peer.emit('on_bad_header', session=self, header=header) is CANCEL_EVENT:
                del self.__incoming[:]
                return

            length = max(header.size, header.length)
        self.__want_read = length

    def _read_message(self):
        """Read message (header)"""
        _LOGGER.debug('%s: reading incoming data <%s>[%s]: %s' % (
                      self, id(self.__incoming), len(self.__incoming),
                      _message.data.to_hex(self.__incoming)))
        header = self._header
        if self.__want_read == header.size:
            self._read_header(header)

        if self.__want_read > len(self.__incoming):
            return  # need more data

        try:
            message = _message.Message.get_message(self.__incoming, header.type_id)
        except Exception as error:
            _LOGGER.exception('%s: Cannot parse message in <%s>: %s' % (self, id(self.__incoming), error))
            self.peer.emit('on_message_error', session=self, message=message, error=error, action='read')
            return
        finally:
            del self.__incoming[:]
        transmission = _message.Transmission(session=self, received=True)
        self._last_received = transmission.time
        message.transmission = transmission
        self._inbox.put(message)
        self.peer.emit('on_message', session=self, message=message)
        self.__want_read = header.size

    def _update_name(self):
        """Update name to reflect change in node port(s)"""
        super(_PcepSession, self)._update_name()
        self._name += '->[%s]' % self._remote


class PcepAccept(_PcepSession):
    """A PCEP session accepted by a PcepServer."""

    def open(self, bus):
        super(PcepAccept, self).open(bus)
        self._established = True
        self.peer.emit('on_connect', session=self)


class PcepClient(_PcepSession):
    """A PCEP session initiated by local peer."""

    CONFIG_CONNECT = 'pcep_client.connect'
    STATE_CONNECTAT = '_pcep_client.connect_at'

    CONNECT = 20

    def __init__(self, peer, local, remote, config):
        if peer.context.address_is_ipv6(local.address):
            family = _socket.AF_INET6
        else:
            family = _socket.AF_INET
        socket = _socket.socket(family, _socket.SOCK_STREAM)
        super(PcepClient, self).__init__(
            peer, local, socket, remote, config
        )

    def open(self, bus):
        address = self.local.address
        port = self.local.port or 0
        try:
            string_address = self.peer.context.address_to_str(address)
            if self.peer.context.address_is_ipv6(address):
                family = _socket.AF_INET6
            else:
                family = _socket.AF_INET
            socket_address = _socket.getaddrinfo(
                string_address, port, family, _socket.SOCK_STREAM
            )[0][-1]

            self._socket.bind(socket_address)
        except (_socket.error, ValueError, IndexError) as error:
            self._error = error
            self.peer.emit('on_socket_error', session=self, error=error, action='open')
            self.closing = True
            return

        super(PcepClient, self).open(bus)

        try:
            address = self.remote.address
            string_address = self.peer.context.address_to_str(address)
            port = self.remote.port or PCEP_PORT
            try:
                self._socket.connect((string_address, port))
            except _socket.error as error:
                if error.errno != errno.EINPROGRESS:
                    raise error
        except (_socket.error, ValueError, IndexError) as error:
            self._error = error
            self.peer.emit('on_socket_error', session=self, error=error, action='connect')
            self.closing = True
            return

        self._config[PcepClient.STATE_CONNECTAT] = resolve_timeout(
            self._config.get(PcepClient.CONFIG_CONNECT, self.CONNECT)
        )

    def on_writable(self):
        if not self._established:
            del self[PcepClient.STATE_CONNECTAT]
            error = self._socket.getsockopt(
                _socket.SOL_SOCKET, _socket.SO_ERROR
            )
            if error:
                error = _socket.error(error, os.strerror(error))
                self._error = error
                self.peer.emit('on_socket_error', session=self, error=error, action='connect')
                self.closing = True
                return
            self._established = True
            self._local = self._local.with_port(self._socket.getsockname()[-1])
            self._remote = self._remote.with_port(self._socket.getpeername()[-1])
            self._update_name()
            self.peer.emit('on_connect', session=self)
        super(PcepClient, self).on_writable()

    def on_timeout(self, now):
        if not self._established:
            error = _socket.timeout('Connection to "%s" timed out' % self._remote)
            self._error = error
            self.peer.emit('on_socket_error', session=self, error=error, action='connect')
            del self[PcepClient.STATE_CONNECTAT]
            self.closing = True
        else:
            super(PcepClient, self).on_timeout(now)

    def want_write(self):
        return not self._established or super(PcepClient, self).want_write()

    def timeout(self):
        if self._established:
            return super(PcepClient, self).timeout()
        return self._config.get(PcepClient.STATE_CONNECTAT)
