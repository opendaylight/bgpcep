# PCE and its handlers

# Copyright (c) 2012,2013 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import socket as _socket

from . import base
from . import lsp as _lsp
from pcepy import session as _session
from pcepy import message as _message


class Pce(base.Peer):
    """A simulated Path Computation Element"""

    CONFIG_SERVER_CONFIG = 'pce.server_config'
    CONFIG_SESSION_CONFIG = 'pce.session_config'

    def __init__(self, name, context):
        super(Pce, self).__init__(name, context)
        self._servers = list()

    def _get_active(self):
        return bool(self._servers) or super(Pce, self)._get_active()

    def _create_handlers(self):
        super(Pce, self)._create_handlers()
        for handler_class in (Listener, Reporter,):
            self.add_handler(self._create_handler(handler_class))

    def create_server(self, node):
        """Bind to a server socket specified by node and put it on the bus."""
        key = Pce.CONFIG_SERVER_CONFIG
        server_config = self[key]
        if server_config:
            server_config = dict(server_config)
        else:
            server_config = dict()
        server_node_config = self[key, node.name]
        if server_node_config:
            server_config.update(server_node_config)

        if not node.port:
            node = node.with_port(_session.PCEP_PORT)
        server = _session.PcepServer(self, node, server_config)
        self._servers.append(server)
        self.context.bus.add(server)
        return server

    def create_session(self, local, socket, remote):
        """Create PCEP session from socket and put it on the bus."""
        key = Pce.CONFIG_SESSION_CONFIG
        session_config = self[key]
        if session_config:
            session_config = dict(session_config)
        else:
            session_config = dict()
        session_remote_config = self[key, remote.name]
        if session_remote_config:
            session_config.update(session_remote_config)

        pcep_session = _session.PcepAccept(
            self, local, socket, remote, session_config
        )
        self._sessions.append(pcep_session)
        self.context.bus.add(pcep_session)
        return pcep_session

    def shutdown(self):
        "Ask all server sessions to close. Then close all incoming sessions"
        for server in self._servers:
            server.closing = True
        super(Pce, self).shutdown()


class Listener(base.Handler):
    """Manage incoming connections on PceServer sockets"""

    CONFIG_TIMEOUT = 'pcep_server.timeout'
    STATE_TIMEOUT = '_listener.timeout'

    def on_open(self, peer, eventargs):
        server = eventargs['session']
        if not server.is_server():
            return
        if Listener.CONFIG_TIMEOUT not in server:
            timeout = peer[Listener.CONFIG_TIMEOUT]
            if timeout:
                server[Listener.STATE_TIMEOUT] = _session.resolve_timeout(timeout)

    def on_close(self, peer, eventargs):
        server = eventargs['session']
        if not server.is_server():
            return
        peer._servers.remove(server)

    def on_connection(self, peer, eventargs):
        # TODO: restrict addresses by config
        base._LOGGER.debug('Creating accepted session on peer %s' % peer)
        server = eventargs['server']
        socket = eventargs['socket']
        address, port = eventargs['address'][:2]
        address = peer.context.address_from(address)
        node = peer.context.get_node(
            _session.Node.ROLE_PCC,
            address=address,
            port=port)
        session = peer.create_session(server.local, socket, node)
        base._LOGGER.debug('Created accepted session %s' % session)
        del server[Listener.CONFIG_TIMEOUT]  # TODO: only if not expecting more

    def on_timeout(self, peer, eventargs):
        session = eventargs['session']
        now = eventargs['now']

        timeout = session[Listener.STATE_TIMEOUT]
        if not timeout or timeout > now:
            return

        del session[Listener.STATE_TIMEOUT]
        peer.emit(
            'on_socket_error',
            session=session,
            error=_socket.timeout('%s: timed out' % session))
        session.closing = True

    def timeout(self, session):
        if session.is_server():
            return session[Listener.STATE_TIMEOUT]


class Requester(base.Handler):
    """Manage reception of PCReq messages."""
    # May be implemented later
    pass


class Reporter(base.Handler):
    """Manage reception of PCRpt messages.

    Manage state database (lsp.StateReports). Await specific state reports.

    The Reporter emits these events to its peer:
        on_state_report(session, lsp, report, new):
            A [new] state report has arrived; called before adding to statedb.

        on_synchronized(session, statedb):
            State synchronization has completed and recorded in lsp.Reports.

        on_await_report(session, key, arrived):
            State report for key has arrived (with report) or timed out (None).
    """

    STATE_STATEDB = '_reporter.statedb'
    STATE_AWAITED = '_reporter.awaited'
    STATE_STATE = '_reporter.state'

    # Values for STATE_STATE
    RS_NONE, RS_SYNCING, RS_AVOID, RS_SYNCED = range(0, 4)

    def on_session_open(self, peer, eventargs):
        session = eventargs['session']
        if session[base.Opener.STATE_PCEPTYPE] == base.Opener.PCEPTYPE_STATELESS:
            session[Reporter.STATE_STATE] = Reporter.RS_NONE
            return
        statedb = self._get_statedb(peer, session)
        session[Reporter.STATE_STATEDB] = statedb
        session[Reporter.STATE_AWAITED] = self._get_awaited(peer, session)

        local_open = session[base.Opener.STATE_LOCAL_OPEN]
        remote_open = session[base.Opener.STATE_REMOTE_OPEN]
        avoid = statedb.can_avoid(pce_open=local_open, pcc_open=remote_open)
        if avoid:
            base._LOGGER.info('Session "%s" has valid database version "%s"' % (session, statedb.version))
            state = Reporter.RS_AVOID
        else:
            state = Reporter.RS_SYNCING
        session[Reporter.STATE_STATE] = state

    def on_message(self, peer, eventargs):
        session = eventargs['session']
        statedb = session[Reporter.STATE_STATEDB]
        if statedb is None:
            return
        message = eventargs['message']
        if not isinstance(message, _message.PCRpt):
            return
        awaited = session[Reporter.STATE_AWAITED]
        use_dbv = session[base.Opener.STATE_USE_DBV]
        state = session[Reporter.STATE_STATE]

        for report in message.poll('report'):
            report_lsp = report.poll('lsp')
            if report_lsp is None:
                peer.make_pcep_error(
                    origin=self,
                    session=session,
                    cause=report,
                    send=_message.code.Error.MandatoryObjectMissing_LSP,
                    closing=False)
                continue
            lsp = statedb[report_lsp.lsp_id]
            if lsp is None:
                new = True
                name = report_lsp.get(_message.tlv.LspSymbolicName)
                if name is None:
                    base._LOGGER.error('New LSP "%s" missing name in "%s"' % (report_lsp, session))
                    # FIXME: should be a PCEP error
                else:
                    name = name.lsp_name
                lsp = _lsp.Lsp(name=name, lsp_id=report_lsp.lsp_id)
            else:
                new = False

            peer.emit(
                'on_state_report',
                session=session,
                lsp=lsp,
                report=report,
                new=new)

            try:
                statedb.get_version(report_lsp, use_dbv)
            except ValueError as value_error:
                peer.make_pcep_error(
                    origin=self,
                    session=session,
                    cause=(report, value_error),
                    send=_message.code.Error.MandatoryObjectMissing_DBV,
                    closing=True)
                return

            if new:
                statedb.add(lsp)
            lsp.report = report

            for key in awaited.match(report):
                peer.emit(
                    'on_await_report',
                    session=session,
                    key=key,
                    arrived=report)

            if not report_lsp.synchronize:
                if state != Reporter.RS_SYNCED:
                    state = Reporter.RS_SYNCED
                    session[Reporter.STATE_STATE] = state
                    peer.emit('on_synchronized', session=session, statedb=statedb)
            elif state == Reporter.RS_AVOID:
                base._LOGGER.warning('Session "%s": synchronization not avoided' % session)
                state = Reporter.RS_SYNCING
                session[Reporter.STATE_STATE] = state
            else:
                base._LOGGER.error('Session "%s": already synchronized' % session)

    def on_timeout(self, peer, eventargs):
        session = eventargs['session']
        now = eventargs['now']
        awaited = session[Reporter.STATE_AWAITED]
        if awaited is None:
            return
        outs = awaited.out(now)
        for out in outs:
            peer.emit(
                'on_await_report',
                session=session,
                key=out,
                arrived=None)

    def timeout(self, session):
        awaited = session[Reporter.STATE_AWAITED]
        return None if awaited is None else awaited.timeout

    def _get_statedb(self, peer, session):
        """Create state database for session"""
        return _lsp.StateDb(self, peer, session)

    def _get_awaited(self, peer, session):
        """Create awaited report database for session"""
        return _lsp.Awaited(self, session)

    def await(self, session, criteria, timeout=None):
        """Watch for a [set of] state reports satisfying criteria."""
        awaited = session[Reporter.STATE_AWAITED]
        for key, criterion in criteria.items():
            awaited.add(self._get_await(session, key, criterion, timeout))

    def _get_await(self, session, key, criterion, timeout=None):
        """Transform a criterium into an Await object."""
        return _lsp.Await(key, criterion, criterion.get('timeout', timeout))
