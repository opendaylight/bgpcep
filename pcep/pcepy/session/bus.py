# The event, socket, timeout multiplexer

# Copyright (c) 2012,2013 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import os
import select
import time
import threading

import logging
_LOGGER = logging.getLogger('pcepy.session.bus')


class Bus(threading.Thread):
    """Manage all PCEP connections (sockets) in a dedicated thread"""

    # Select timeout will be always at most this much into the future
    _max_timeout = 60

    # How many times may a session management change occur in one loop cycle
    # A management change is an open(add) or close(remove) call for session.
    _max_retries = 10

    def __init__(self):
        super(Bus, self).__init__(name='PcepBusThread')

        # Managed connections
        self._connections = list()
        self._managing = True

        # Control pipe; used to interrupt underlying select() calls
        # before timout expires.
        self._ctlpipe = CtlPipe(self)

    def start(self):
        _LOGGER.info('Bus management start requested')
        super(Bus, self).start()

    def stop(self):
        self._managing = False
        self.hail()
        _LOGGER.info('Bus management stop requested')

    def hail(self):
        "Notify bus of a change in session(s); return from select at once"
        self._ctlpipe.put()

    def add(self, connection):
        """Open and start managing a connection"""
        _LOGGER.info('Added connection to bus: %s' % connection)
        self._connections.append(connection)
        self.hail()

    def remove(self, connection):
        """Stop managing a connection and close it"""
        _LOGGER.info('Removing connection from bus: %s' % connection)
        self._connections.remove(connection)
        self.hail()

    def working(self):
        return self._managing or bool(self._connections)

    def run(self):
        _LOGGER.info('Bus management started')
        while self.working():
            timeout = resolve_timeout(self._max_timeout)
            readers = list()
            writers = list()
            readers.append(self._ctlpipe)

            retries = self._max_retries
            while retries and self.working():
                retries -= 1
                connections = list(self._connections)
                for connection in connections:
                    if connection.bus is None:
                        connection.open(self)
                        break
                    if connection.want_close():
                        connection.close()
                        break
                else:
                    break  # No change
            if not self.working():
                break
            if not retries:
                raise RuntimeError('Cannot stabilize managed connection list')

            for connection in connections:
                readers.append(connection)

                cto = connection.timeout()
                if cto:
                    timeout = min(timeout, cto)
                if connection.want_write():
                    writers.append(connection)

            timeout -= time.time()
            if timeout <= 0:
                _LOGGER.warning('Timeout in the past: %s' % timeout)
                timeout = 0

            readable, writable, exes = select.select(
                readers, writers, readers, timeout
            )
            if not self.working():
                break

            for connection in exes:
                connection.on_exception()
            for connection in writable:
                connection.on_writable()
            for connection in readable:
                connection.on_readable()

            now = time.time()
            for connection in connections:
                cto = connection.timeout()
                if cto and now >= cto:
                    connection.on_timeout(now)
        _LOGGER.info('Bus management stopped')

    def on_control(self, cmds):
        """Called whenever there is something on the control pipe"""
        pass


class CtlPipe(object):
    """Pipe-based control channel"""

    # TIP: could also be used for per-PCE control with named pipes

    def __init__(self, owner):
        self.owner = owner
        self._ctlread, self._ctlwrite = os.pipe()

    def put(self, value=b'\0'):
        """Put a string into the pipe. Must succeed in one write."""
        written = os.write(self._ctlwrite, value)
        if written != len(value):
            raise IOError("Write to control pipe failed")

    def get(self, max_len=255):
        """Get all available data from the pipe. Will block if there is none."""
        return os.read(self._ctlread, max_len)

    def fileno(self):
        """Return the read end file descriptor"""
        return self._ctlread

    def close(self):
        os.close(self._ctlwrite)
        os.close(self._ctlread)
        self._ctlwrite = None
        self._ctlread = None

    def on_readable(self):
        self.owner.on_control(self.get())

    def __del__(self):
        self.close()

    def __str__(self):
        return 'BusControl'


def resolve_timeout(timeout, now=None):
    """Return a point in time, either timeout seconds away from now,
    or timeout itself if it looks like a point in time itself.
    May be used as a sanity check and allows configuring timeouts both ways.
    """
    if timeout is None:
        return timeout
    if now is None:
        now = time.time()
    if timeout > 1000:
        if timeout - now > 100000:
            raise ValueError("Timeout too far away")
        return timeout
    return now + timeout


def min_timeout(time1, time2):
    """Return the earliest timeout, if any."""
    if time1 is None:
        return time2
    elif time2 is None:
        return time1
    elif time1 <= time2:
        return time1
    else:
        return time2
