# PCC and its handlers

# Copyright (c) 2012,2013 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

from . import base
from . import lsp as _lsp
from pcepy import session as _session
from pcepy import message as _message


class Pcc(base.Peer):
    """A simulated Path Computation Client"""

    CONFIG_SESSION_CONFIG = 'pcc.session_config'

    def _create_handlers(self):
        super(Pcc, self)._create_handlers()
        for handler_class in (Updater,):
            self.add_handler(self._create_handler(handler_class))

    def create_session(self, local, remote):
        """Create PCEP client session to remote node and put it on the bus."""
        key = Pcc.CONFIG_SESSION_CONFIG
        session_config = self[key]
        if session_config:
            session_config = dict(session_config)
        else:
            session_config = dict()
        session_remote_config = self[key, remote.name]
        if session_remote_config:
            session_config.update(session_remote_config)

        pcep_session = _session.PcepClient(self, local, remote, session_config)
        self._sessions.append(pcep_session)
        self.context.bus.add(pcep_session)
        return pcep_session


class Replyer(base.Handler):
    """Manage reception of PCRep messages."""
    # May be implemented later
    pass


class Updater(base.Handler):
    """Manage reception of PCUpd messages.

    Manage state database (lsp.UpdateRequest). Await specific update requests.

    The Updater emits these events to its peer:
        on_update_request(session, lsp, request):
            An update request has arrived; called before adding to updates.
            If lsp is None, it is now known.

        on_await_update(session, key, arrived):
            Update request for key has arrived (with update) or timed out (None).
    """

    CONFIG_SYNC_SEPARATELY = 'updater.sync_separately'

    STATE_STATEDB = '_updater.statedb'
    STATE_AWAITED = '_updater.awaited'

    def on_session_open(self, peer, eventargs):
        session = eventargs['session']
        if session[base.Opener.STATE_PCEPTYPE] == base.Opener.PCEPTYPE_STATELESS:
            return
        statedb = self._get_statedb(peer, session)
        session[Updater.STATE_STATEDB] = statedb
        self._make_lsps(peer, session, statedb)
        session[Updater.STATE_AWAITED] = self._get_awaited(peer, session)

        local_open = session[base.Opener.STATE_LOCAL_OPEN]
        remote_open = session[base.Opener.STATE_REMOTE_OPEN]
        avoid = statedb.can_avoid(pcc_open=local_open, pce_open=remote_open)
        if avoid:
            base._LOGGER.info('Session "%s" has valid database version "%s"' % (session, statedb.version))
        else:
            self._send_state_sync(peer, session)

    def on_message(self, peer, eventargs):
        session = eventargs['session']
        statedb = session[Updater.STATE_STATEDB]
        if statedb is None:
            return
        message = eventargs['message']
        if not isinstance(message, _message.PCUpd):
            return
        awaited = session[Updater.STATE_AWAITED]

        if session[base.Opener.STATE_PCEPTYPE] != base.Opener.PCEPTYPE_STATEFULA:
            peer.make_pcep_error(
                origin=self,
                session=session,
                cause=message,
                send=_message.code.Error.InvalidOperation_DelegationNotActive,
                closing=False)
            return

        for update in message.poll('update'):
            if not update.have('lsp'):
                peer.make_pcep_error(
                    origin=self,
                    session=session,
                    cause=update,
                    send=_message.code.Error.MandatoryObjectMissing_LSP,
                    closing=False)
                continue
            lsp_id = update.poll('lsp').lsp_id
            lsp = statedb[lsp_id]
            peer.emit(
                'on_update_request',
                session=session,
                lsp_id=lsp_id,
                lsp=lsp,
                update=update)
            if lsp is not None:
                lsp.update = update

            for key in awaited.match(update):
                peer.emit(
                    'on_await_update',
                    session=session,
                    key=key,
                    arrived=update)

    def on_timeout(self, peer, eventargs):
        session = eventargs['session']
        now = eventargs['now']
        awaited = session[Updater.STATE_AWAITED]
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
        awaited = session[Updater.STATE_AWAITED]
        return None if awaited is None else awaited.timeout

    def _get_statedb(self, peer, session):
        """Create state database for session"""
        return _lsp.StateDb(self, peer, session)

    def _get_awaited(self, peer, session):
        """Create awaited update database for session"""
        return _lsp.Awaited(self, session)

    def _make_lsps(self, peer, session, statedb):
        """Populate the state database on session open.
        Should be implemented by a subclass.
        """
        pass

    def _make_report(self, peer, session, lsp):
        """Create a lsp.Report object for lsp or None."""
        pass

    def _send_state_sync(self, peer, session):
        """Send state reports from current database."""
        statedb = session[Updater.STATE_STATEDB]

        reports = [self._make_report(peer, session, lsp) for lsp in statedb]
        reports = [report for report in reports if report is not None]
        count = len(reports)
        if not count:
            return

        use_dbv = session[base.Opener.STATE_USE_DBV]

        for report in reports:
            lsp = report.poll('lsp')
            if lsp is not None:  # possibly sending bad report
                lsp.synchronize = True
                if use_dbv:
                    statedb.put_version(lsp)

        if self[Updater.CONFIG_SYNC_SEPARATELY]:
            for report in reports:
                message = _message.PCRpt()
                message.push('report', report)
                message.pack()
                session.send(message)
        else:
            message = _message.PCRpt()
            message.push('report', reports)
            message.pack()
            session.send(message)

    def await(self, session, criteria, timeout=None):
        """Watch for a [set of] update requests satisfying criteria."""
        awaited = session[Updater.STATE_AWAITED]
        for key, criterion in criteria.items():
            awaited.add(self._get_await(session, key, criterion, timeout))

    def _get_await(self, session, key, criterion, timeout=None):
        """Transform a criterium into an Await object."""
        return _lsp.Await(key, criterion, criterion.get('timeout', timeout))
