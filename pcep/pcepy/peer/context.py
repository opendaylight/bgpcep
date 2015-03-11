# Context - container and environment for peers, nodes and config

# Copyright (c) 2012,2013 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

# external dependency, licensed under the Apache 2.0 license
import ipaddr

from pcepy import session as _session


class Context(object):
    """The Context is a container and environment for all library objects.
    It should be the first object created.

    It holds information on Nodes (named network participants),
    PCEP Peers (on the side of this library), the Session Bus (multiplexing
    messages and events for Peers) and the configuration controlling them.

    It also serves as a single authority for IP address/network handling.
    """

    def __init__(self, config=None):
        super(Context, self).__init__()
        if config is None:
            config = dict()
        self._config = config
        self._nodes = list()  # Node
        self._peers = dict()  # name -> Peer
        self._bus = _session.Bus()

    @property
    def bus(self):
        """The session bus."""
        return self._bus

    @property
    def config(self):
        """Global configuration"""
        return self._config

    def get_peer(self, name):
        return self._peers[name]

    def set_peer(self, name, peer):
        self._peers[name] = peer

    def get_node(self, role, name=None, address=None, port=None):
        """Get or create a node for role matching name and/or address[:port].
        Raises ValueError if node cannot be found nor created.
        """
        nodes = [node for node in self._nodes if node.role == role]

        if name is None:
            if address is None:
                raise ValueError('Undefined node')
            name = '<unknown>'
        else:
            nodes = [node for node in nodes if node.name == name]

        if address is not None:
            address = self.address_from(address)
            nodes = [node for node in nodes if node.address == address]

        portless = None
        for node in nodes:
            if node.port == port:
                return node
            if node.port is None:
                portless = node

        if portless is not None:
            name = portless.name
            address = portless.address

        node = _session.Node(role, name, address, port)
        self._nodes.append(node)
        return node

    @staticmethod
    def address_from(address):
        """Return library-stored representation of IP address.
        The library does not use this representation outside this class,
        except checking for equality.
        """
        return ipaddr.IPAddress(address)

    @staticmethod
    def address_to_int(address):
        """Return library-stored address as an integer (network byte order)."""
        return int(address)

    @staticmethod
    def address_to_str(address):
        """Return library-stored address as a string."""
        return str(address)

    @staticmethod
    def address_is_ipv6(address):
        """Return if library-stored address is an IPv6 address."""
        return isinstance(address, ipaddr.IPv6Address)

    def get(self, key, default=None):
        """Convenience access to config dict."""
        return self._config.get(key, default)

    def __getitem__(self, key):
        """Convenience access to config dict.
        Returns None instead of raising KeyError.
        """
        return self.get(key, None)

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
        """Convenience access to config dict or context."""
        return key in self._config
