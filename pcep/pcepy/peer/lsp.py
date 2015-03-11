# Label-switched path, its configuration and state

# Copyright (c) 2012,2013 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html


from . import base

from pcepy import message as _message


class Lsp(object):
    """A MPLS-TE Label-switched path"""

    def __init__(self, lsp_id=None, name=None):
        self._lsp_id = lsp_id
        self._name = name
        self._report = None
        self._update = None
        self._history = list()

    @property
    def lsp_id(self):
        """Session-local id of LSP"""
        return self._lsp_id

    @property
    def name(self):
        """Symbolic name of LSP"""
        return self._name

    @property
    def report(self):
        """Current state in form of a message.Report object"""
        return self._report

    @report.setter
    def report(self, report):
        """Set newly-arrived current LSP state report"""
        self._report = report
        self._history.append(report)

    @property
    def update(self):
        """Latest update in form of a message.Update object"""
        return self._update

    @update.setter
    def update(self, update):
        self._update = update
        self._history.append(update)

    @property
    def history(self):
        """History of state reports and updates"""
        return self._history


class StateDb(object):
    """Store information on LSPs' state reports and updates"""

    def __init__(self, reporter, peer, session):
        self._reporter = reporter
        self._peer = peer
        self._session = session

        self._lsps = dict()  # lsp_id in session -> Lsp
        self._version = 1

    def add(self, lsp):
        """Add new lsp"""
        self._lsps[lsp.lsp_id] = lsp

    @property
    def version(self):
        """Current LSP state databese version"""
        return self._version

    @version.setter
    def version(self):
        self._version = version  # noqa

    def bump(self):
        """Bump state database version"""
        self._version = (self._version % 0xFFFFFFFE) + 1

    def put_version(self, lsp):
        """Add or replace LSP database version in message LSP object."""
        dbv = lsp.get(_message.tlv.LspStateDbVersion)
        if dbv is None:
            dbv = _message.tlv.LspStateDbVersion()
            lsp.add(dbv)
        dbv.db_version = self._version

    def get_version(self, lsp, must=False):
        """Update current LSP database version from message LSP object"""
        dbv = lsp.get(_message.tlv.LspStateDbVersion)
        if dbv is None:
            if must:
                raise ValueError("LSP Missing database version TLV")
            return
        if dbv.db_version < self._version:
            base._LOGGER.warning('Session "%s" database version lowered'
                                 ' from %s to %s' % (self._session, self._version, dbv.db_version))
        self._version = dbv.db_version

    def can_avoid(self, pcc_open, pce_open):
        """Return whether state synchronization can be avoided.
        Update version db from pcc_open if available.
        """
        tlv = pcc_open.get(_message.tlv.StatefulPcepCapability)
        if tlv is None or not tlv.include_db_version:
            return False
        tlv = pcc_open.get(_message.tlv.LspStateDbVersion)
        if tlv is None or not tlv.db_version:
            return False
        self._version = tlv.db_version
        tlv = pce_open.get(_message.tlv.StatefulPcepCapability)
        if tlv is None or not tlv.include_db_version:
            return False
        tlv = pce_open.get(_message.tlv.LspStateDbVersion)
        return tlv is not None and self._version == tlv.db_version

    def __getitem__(self, key):
        return self._lsps.get(key)

    def __contains__(self, key):
        return key in self._lsps

    def __iter__(self):
        return iter(self._lsps.values())

    def __len__(self):
        return len(self._lsps)


class Await(object):
    """Await a specific incoming message.
    The default implementation matches by lsp name or id or a callable.
    """

    def __init__(self, key, criterion, timeout=None):
        self.key = key
        self.criterion = criterion
        self.timeout = timeout

    def match(self, against):
        """Return True if the criterion matched"""
        criterion = self.criterion
        if callable(criterion):
            return criterion(against)
        lsp = against.poll('lsp')
        if lsp is None:
            return False
        if isinstance(criterion, int):
            return criterion == lsp.lsp_id
        elif isinstance(criterion, str):
            name = lsp.get(_message.tlv.LspSymbolicName)
            return name is not None and criterion == name.lsp_name
        return False


class Awaited(object):
    """Manage [timeouts on] arrival of messages satisfying criteria."""

    def __init__(self, owner, session):
        self._owner = owner
        self._session = session

        self._awaits = dict()  # user_key -> Await
        self._timeout = None

    def add(self, key, await):
        if key in self._awaits:
            raise KeyError("Duplicate: %s" % key)
        self._awaits[key] = await
        self._timeout = base.min_timeout(self._timeout, await.timeout)

    def match(self, against):
        """Return and remove a list of awaits matching."""
        matches = [key
                   for key, await in self._awaits.items()
                   if await.match(against)]
        if matches:
            for key in matches:
                del self._awaits[key]
            self._update_timeout()
        return matches

    def out(self, now):
        """Return and remove a list of awaits timed out now"""
        matches = [key
                   for key, await in self._awaits.items()
                   if await.timeout and await.timeout <= now]
        if matches:
            for key in matches:
                del self._awaits[key]
            self._update_timeout()
        return matches

    def all(self):
        """Return and clear all current awaits"""
        matches = self._awaits.keys()
        self._awaits = dict()
        self._timeout = None
        return matches

    def _update_timeout(self):
        timeout = None
        for await in self._awaits:
            timeout = base.min_timeout(timeout, await.timeout)
        self._timeout = timeout

    @property
    def timeout(self):
        return self._timeout
