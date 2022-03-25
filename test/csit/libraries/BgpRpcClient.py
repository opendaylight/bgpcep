"""
The purpose of this library is communicate with tools which run xmlrpc server.
At the moment it is going to be the with the exarpc.py (used with exabgp) and
with play.py for bgp functional testing.
exa_ methods apply to test/tools/exabgp_files/exarpc.py
play_ methods apply to test/tool/fastbgp/play.py  (only with --evpn used)
"""

import re
import xmlrpc.client


class BgpRpcClient(object):
    """The client for SimpleXMLRPCServer."""

    def __init__(self, peer_addr, port=8000):
        """Setup destination point of the rpc server"""
        self.proxy = xmlrpc.client.ServerProxy("http://{}:{}".format(peer_addr, port))

    def exa_announce(self, full_exabgp_cmd):
        """The full command to be passed to exabgp."""
        return self.proxy.execute(full_exabgp_cmd)

    def _exa_get_counter(self, msg_type):
        """Gets counter form the server of given message type."""
        return self.proxy.get_counter(msg_type)

    def exa_get_received_open_count(self):
        """Gets open messages counter."""
        return self._exa_get_counter("open")

    def exa_get_received_keepalive_count(self):
        """Gets keepalive messages counter."""
        return self._exa_get_counter("keepalive")

    def exa_get_received_update_count(self):
        """Gets update messges counter."""
        return self._exa_get_counter("update")

    def exa_get_received_route_refresh_count(self):
        """Gets route refresh message counter."""
        return self._exa_get_counter("route_refresh")

    def _exa_clean_counter(self, msg_type):
        """Cleans counter on the server of given message type."""
        return self.proxy.clean_counter(msg_type)

    def exa_clean_received_open_count(self):
        """Cleans open message counter."""
        return self._exa_clean_counter("open")

    def exa_clean_received_keepalive_count(self):
        """Cleans keepalive message counter."""
        return self._exa_clean_counter("keepalive")

    def exa_clean_received_update_count(self):
        """Cleans update message counter."""
        return self._exa_clean_counter("update")

    def exa_clean_received_route_refresh_count(self):
        """Cleans route refresh message counter."""
        return self._exa_clean_counter("route_refresh")

    def _exa_clean_message(self, msg_type):
        """Cleans stored message on the server of given message type."""
        return self.proxy.clean_message(msg_type)

    def exa_clean_update_message(self):
        """Cleans update message."""
        return self._exa_clean_message("update")

    def _exa_get_message(self, msg_type):
        """Gets stored message on the server of given message type."""
        return self.proxy.get_message(msg_type)

    def exa_get_update_message(self, msg_only=True):
        """Cleans update message.

        Exabgp provides more details than just message content (e.g. peer ip,
        timestamp, ...). msg_only is a flag that we want just message content
        and no details.
        """
        msg = self._exa_get_message("update")
        if not msg_only:
            return msg
        return msg if "neighbor" not in msg else msg["neighbor"]["message"]

    def play_send(self, hexstring):
        """Sends given hex data, already encoded bgp update message is expected."""
        return self.proxy.send(hexstring.rstrip())

    def play_get(self, what="update"):
        """Gets the last received (update) mesage as hex string."""
        return self.proxy.get(what)

    def play_clean(self, what="update"):
        """Cleans the message (update) on the server."""
        return self.proxy.clean(what)

    def sum_hex_message(self, hex_string):
        """Verifies two hex messages are equal even in case, their arguments are misplaced.
        Converts hex message arguments to integers and sums them up and returns the sum."""
        return sum(
            [int(x, 16) for x in re.compile(r"[a-f\d]{2}").findall(hex_string[32:])]
        )
