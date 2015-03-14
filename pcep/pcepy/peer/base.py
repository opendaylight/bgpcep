# Base PCEP Peer and common Handlers

# Copyright (c) 2012,2013 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import weakref
import traceback
import logging
_LOGGER = logging.getLogger('pcepy.peer')

from pcepy import session as _session
from pcepy import message as _message
resolve_timeout = _session.resolve_timeout
min_timeout = _session.min_timeout

EMIT_RESULT = 'EMIT_RESULT'  # key to put into eventargs
CANCEL_EVENT = _session.CANCEL_EVENT


class Peer(object):
    """Base class for PCEP peers (PCE or PCC).
    It creates connections (e.g. PCEP sessions), which are managed by a shared
    session Bus.

    It acts on these connections by creating and writing messages, and
    by defining a timeout, a latest point in time by which an event must occur.

    It reacts to events (e.g. connection setup and teardown, incoming messages,
    timeouts) generated on the session Bus' thread by its connections.
    The reaction is indirect, as the peer delegates all events to an ordered
    list of Handlers, each defining callbacks for a set of specific events.

    All events are triggered by calling a peer's emit method, which relays
    the event to all enabled and interested handlers in a predetermined order.

    The events are usually triggered on a shared thread handling all other
    events, socket traffic and timeouts: this thread must not be hogged.

    A handler is interested in an event if it has a method of the same name
    as the event. Event names start with 'on_'.
    A handler method shall accept two arguments: peer and a dict of arguments.
    The dict of event arguments is shared among handlers; it may be used
    to pass values among themselves, especially the key EMIT_RESULT, which
    is used as the result of the emit method (e.g. the CANCEL_EVENT singleton).
    See Session and Handler subclasses' documentation for a list of events.
    """

    CONFIG_PEER = 'peer.config'

    def __init__(self, name, context):
        self._name = name
        self._context = weakref.ref(context)

        key = Peer.CONFIG_PEER
        peer_config = context[key]
        if peer_config:
            peer_config = dict(peer_config)
        else:
            peer_config = dict()
        peer_name_config = context[key, name]
        if peer_name_config:
            peer_config.update(peer_name_config)
        self._config = peer_config
        context.set_peer(name, self)

        # All PCEP Sessions
        self._sessions = list()

        # Handlers
        self._handlers = list()
        self._create_handlers()

    @property
    def context(self):
        """The context in which this peer operates."""
        return self._context()

    @property
    def config(self):
        """Peer-local configuration."""
        return self._config

    @property
    def name(self):
        return self._name

    @property
    def active(self):
        "Read-only flag indicating that there are active sessions on the Peer"
        return self._get_active()

    def _get_active(self):
        "Overridable getter for the active property"
        return bool(self._sessions)

    @property
    def sessions(self):
        "List of PCEP sessions on the Peer"
        return self._sessions

    @property
    def handlers(self):
        "List of event Handlers for this Peer"
        return self._handlers

    def _create_handlers(self):
        """Create all required handlers"""
        for handler_class in (Logger, Opener, Closer, Keeper):
            self.add_handler(self._create_handler(handler_class))

    def _create_handler(self, handler_class):
        """Create a handler of the specified class, its descendant,
        replacement of any class or None to ignore.
        """
        return handler_class()

    def add_handler(self, handler):
        """Add handler, if not None."""
        if handler is not None:
            self._handlers.append(handler)

    def remove_handler(self, handler_type):
        """ Remove handler of handler_type. """
        new_handlers = [
            h for h in self._handlers if not isinstance(h, handler_type)
        ]

        if len(self._handlers) != len(new_handlers) + 1:
            raise AttributeError('Handler %s not in handlers' % handler_type)

        self._handlers = new_handlers

    def emit(self, event, **eventargs):
        """Emit an event (e.g. on_message) to all enabled handlers.
        Return the EMIT_RESULT key from [modified] eventargs, if present.
        """
        for handler in self._handlers:
            if not handler.enabled:
                continue
            handle = getattr(handler, event, False)
            if handle is not False:
                try:
                    handle(self, eventargs)
                except Exception as error:
                    _LOGGER.exception(
                        "Handler %s failed to handle %s for %s: %s"
                        % (handler, event, eventargs, error)
                    )
        return eventargs.get(EMIT_RESULT)

    def make_pcep_error(self, origin, session, cause, send, closing, **kwargs):
        """Emit an on_pcep_error event for session on behalf of origin handler.
        Cause is a free-form object carrying information on the error.
        Send is a Error- or Close- Code, Object, Group or complete message.
        Closing indicates if the session shall be asked to close.
        The message will be pack()-ed if created or updated by this method.
        Kwargs are applied only to instances created by this method or to send.
        They may contain:
            object_set (updates for main object of the message group),
            object_tlv (additional tlv(s) to the object),
            group_set (updates for the Error Group object),
            message_set (updates for complete Message).
        """

        is_applied = True
        if isinstance(send, _message.code.Error):
            send = _message.object.Error(code=send)
        elif isinstance(send, _message.code.Close):
            send = _message.object.Close(code=send)
        elif not isinstance(send, (_message.object.Error, _message.object.Close)):
            is_applied = False

        if is_applied:
            updates = kwargs.get('object_set')
            if updates:
                send.update(updates)
            tlvs = kwargs.get('object_tlv')
            if tlvs is not None:
                if not isinstance(tlvs, list):
                    tlvs = [tlvs]
                for tlv in tlvs:
                    send.add(tlv)

            if isinstance(send, _message.object.Error):
                send = _message.GError(error=send)

        is_applied = True
        if isinstance(send, _message.GError):
            updates = kwargs.get('group_set')
            if updates:
                send.update(updates)

            send = _message.PCErr(gerror=send)
        elif isinstance(send, _message.object.Close):
            send = _message.Close(close=send)

        elif isinstance(send, _message.Message):
            is_applied = False
        else:
            raise ValueError("Cannot send instance of %s on PCEP error" % send.__class__.__name__)

        updates = kwargs.get('message_set')
        if updates:
            send.update(updates)
            is_applied = True

        if is_applied:
            send.pack()
        return self._emit_pcep_error(origin, session, cause, send, closing)

    def _emit_pcep_error(self, origin, session, cause, message, closing):
        """Process the error created by make_pcep_error"""
        result = self.emit(
            'on_pcep_error',
            session=session,
            origin=origin,
            cause=cause,
            message=message,
            closing=closing)
        if message:
            session.send(message)
        if closing:
            session.closing = closing
        return result

    def timeout(self, session):
        """Return earliest time at which to emit on_timeout for session.
        None means no timeout.
        Note that on_timeout will be called for all handlers (as all events).
        """
        timeout = None
        for handler in self._handlers:
            if not handler.enabled:
                continue
            getter = getattr(handler, 'timeout', False)
            if getter is False:
                continue
            got = getter(session)
            if got is None:
                continue
            if timeout is None or timeout > got:
                timeout = got
        return timeout

    def shutdown(self):
        """Ask all sessions to close"""
        for session in self._sessions:
            if not session.closing:
                session.send(_message.Close(_message.object.Close(code=_message.code.Close.NoExplanation)))
                session.closing = True

    def __str__(self):
        return self.name

    def get(self, key, default=None):
        """Convenience access to config dict or context."""
        try:
            return self._config[key]
        except KeyError:
            return self.context.get(key, default)

    def __getitem__(self, key):
        """Convenience access to config dict or context.
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
        return key in self._config or key in self.context


class Handler(object):
    """Base class for all library-defined event handlers.
    A handler is any object that defines one or more methods named after
    an event (e.g. on_message).
    The method takes the Peer instance as its first argument, and
    a dict of keyword arguments as passed to Peer.emit().

    It also needs an attribute named 'enabled' to control if events
    shall be reported to it.

    Each handler may emit these common events to its peer:
        on_pcep_error(session, cause, error_message, close_message, closing):
            A violation of PCEP occurred. Messages may be sent, session closed.
    """

    def __init__(self):
        self._enabled = True

    def is_enabled(self):
        """Enabled controls if events shall be reported to this handler."""
        return self._enabled

    def set_enabled(self, enabled):
        """Change the enabled attribute of this handler."""
        self._enabled = enabled

    enabled = property(is_enabled, set_enabled)

    def __str__(self):
        return "%s Handler" % self.__class__.__name__


class Opener(Handler):
    """Handle opening a new session and parameter negotiation.
    It emits these events to its peer:
        on_session_open(session):
            Session initialization was succesful.
    """

    CONFIG_PCEPTYPE = 'session.pceptype'

    PCEPTYPE_STATELESS = 'pcep.stateless'
    PCEPTYPE_STATEFUL = 'pcep.stateful'
    PCEPTYPE_STATEFULA = 'pcep.stateful_active'
    PCEPTYPE_STATEFULI = 'pcep.stateful_instantiation'

    # May be changed in children
    PCEPTYPE_DEFAULT = PCEPTYPE_STATEFULA

    # -1 means use versions, but don't send LspStateDbVersionTlv in Open
    CONFIG_DB_VERSION = 'session.db_version'

    CONFIG_NODE_ID = 'peer.node_id'

    STATE_STATE = '_opener.state'
    STATE_PCEPTYPE = '_opener.pceptype'
    STATE_OPENWAIT = '_opener.openwait'
    STATE_KEEPWAIT = '_opener.keepwait'
    STATE_SESSIONID = '_opener.sessionid'
    STATE_USE_DBV = '_opener.use_dbv'

    # Session initialization state variables
    STATE_REMOTE_OPEN = '_opener.remote_open'
    STATE_REMOTE_OK = '_opener.remote_ok'
    STATE_LOCAL_OPEN = '_opener.local_open'
    STATE_LOCAL_OK = '_opener.local_ok'

    OPENWAIT = 60
    KEEPWAIT = 60

    # Values for STATE_STATE
    OS_OPENWAIT, OS_KEEPWAIT, OS_FAILED, OS_UP = range(1, 5)

    def on_connect(self, peer, eventargs):
        session = eventargs['session']
        if session.is_server():
            return

        # TODO: duplicate sessions -> send err 9 close
        session[Opener.STATE_STATE] = Opener.OS_OPENWAIT
        session[Opener.STATE_PCEPTYPE] = self._get_pceptype(peer, session)
        session[Opener.STATE_SESSIONID] = self._get_sessionid(peer, session)
        session[Keeper.STATE_KEEPALIVE] = self._get_keepalive(peer, session)
        session[Keeper.STATE_DEADTIMER] = self._get_deadtimer(peer, session)
        session[Opener.STATE_OPENWAIT] = resolve_timeout(Opener.OPENWAIT)
        open = self._make_open(peer, session)
        self._send_open(peer, session, open)

    def on_message(self, peer, eventargs):
        session = eventargs['session']
        message = eventargs['message']
        state = session[Opener.STATE_STATE]

        if session.closing or not state or state == Opener.OS_FAILED:
            _LOGGER.warning(
                "Session %s received %s Message while closing and/or failed" % (session, message.__class__.__name__))
            return

        is_open = isinstance(message, _message.Open)

        if state == Opener.OS_UP:
            if is_open:
                _LOGGER.error("Session %s received open message while already open" % session)
            return

        open = message.get(_message.object.Open)
        error_code = None  # we are sending

        # SEE APPENDIX A of RFC 5440 for following logic:
        if state == Opener.OS_OPENWAIT:

            if not is_open or not open:
                peer.make_pcep_error(
                    origin=self,
                    session=session,
                    cause=message,
                    send=_message.code.Error.EstablishmentFailure_ReceivedNotOpen,
                    closing=True)
                session[Opener.STATE_STATE] = Opener.OS_FAILED
                return

            open_retry = session[Opener.STATE_REMOTE_OPEN] is not None
            session[Opener.STATE_REMOTE_OPEN] = open
            accept_open = self._accept_open(peer, session, open)

            if accept_open is True:
                del session[Opener.STATE_OPENWAIT]
                session[Opener.STATE_REMOTE_OK] = True
                session.send(_message.Keepalive())

                if session[Opener.STATE_LOCAL_OK]:
                    self._session_open(peer, session)
                else:
                    session[Opener.STATE_STATE] = Opener.OS_KEEPWAIT
                    session[Opener.STATE_KEEPWAIT] = resolve_timeout(
                        Opener.KEEPWAIT
                    )
                return

            if open_retry:
                closing = True
                accept_open = None
                error_code = _message.code.Error\
                    .EstablishmentFailure_StillUnacceptable

            elif isinstance(accept_open, _message.object.Open):
                closing = False
                error_code = _message.code.Error\
                    .EstablishmentFailure_Negotiable
                if session[Opener.STATE_LOCAL_OK]:
                    session[Opener.STATE_OPENWAIT] = resolve_timeout(
                        Opener.OPENWAIT
                    )
                else:
                    del session[Opener.STATE_OPENWAIT]
                    session[Opener.STATE_STATE] = Opener.OS_KEEPWAIT
                    session[Opener.STATE_KEEPWAIT] = resolve_timeout(
                        Opener.KEEPWAIT
                    )

            else:
                closing = True
                accept_open = None
                error_code = _message.code.Error\
                    .EstablishmentFailure_Nonnegotiable

            peer.make_pcep_error(
                origin=self,
                session=session,
                cause=message,
                send=error_code,
                closing=closing,
                message_set=dict(open=accept_open))
            if closing:
                session[Opener.STATE_STATE] = Opener.OS_FAILED
            return

        assert state == Opener.OS_KEEPWAIT

        if isinstance(message, _message.Keepalive):
            session[Opener.STATE_LOCAL_OK] = True
            del session[Opener.STATE_KEEPWAIT]
            if session[Opener.STATE_REMOTE_OK]:
                self._session_open(peer, session)
            else:
                session[Opener.STATE_STATE] = Opener.OS_OPENWAIT
                session[Opener.STATE_OPENWAIT] = resolve_timeout(
                    Opener.OPENWAIT
                )
            return

        if isinstance(message, _message.PCErr):
            error = message.get(_message.object.Error)
            if (open and error.code == _message.code.Error.EstablishmentFailure_Negotiable):
                accept_open = self._accept_error(peer, session, open)
                if accept_open:
                    self._send_open(peer, session, accept_open)
                    if session[Opener.STATE_REMOTE_OK]:
                        session[Opener.STATE_KEEPWAIT] = resolve_timeout(
                            Opener.KEEPWAIT
                        )
                    else:
                        del session[Opener.STATE_KEEPWAIT]
                        session[Opener.STATE_STATE] = Opener.OS_OPENWAIT
                        session[Opener.STATE_OPENWAIT] = (
                            resolve_timeout(Opener.OPENWAIT)
                        )
                    return

                error_code = _message.code.Error\
                    .EstablishmentFailure_ErrorUnacceptable

        if error_code is None:
            error_code = _message.code.Error\
                .EstablishmentFailure_ReceivedNotOpen

        peer.make_pcep_error(
            origin=self,
            session=session,
            cause=message,
            send=error_code,
            closing=True)

    def on_timeout(self, peer, eventargs):
        session = eventargs['session']
        now = eventargs['now']
        state = session[Opener.STATE_STATE]

        timeout = session[Opener.STATE_OPENWAIT]
        error_code = None
        if state == Opener.OS_OPENWAIT and timeout and timeout <= now:
            error_code = _message.code.Error\
                .EstablishmentFailure_OpenWaitExpired
            del session[Opener.STATE_OPENWAIT]
        else:
            timeout = session[Opener.STATE_KEEPWAIT]
            if state == Opener.OS_KEEPWAIT and timeout and timeout <= now:
                error_code = _message.code.Error\
                    .EstablishmentFailure_KeepWaitExpired
                del session[Opener.STATE_KEEPWAIT]

        if error_code is None:
            return

        session[Opener.STATE_STATE] = Opener.OS_FAILED
        peer.make_pcep_error(
            origin=self,
            session=session,
            cause=(now, timeout),
            send=error_code,
            closing=True)

    def timeout(self, session):
        return min_timeout(
            session[Opener.STATE_OPENWAIT], session[Opener.STATE_KEEPWAIT]
        )

    def _get_pceptype(self, peer, session):
        """Return PCEP session type for new session"""
        return (session[Opener.CONFIG_PCEPTYPE] or
                peer.get(Opener.CONFIG_PCEPTYPE, self.PCEPTYPE_DEFAULT))

    def _get_sessionid(self, peer, session):
        """Return PCEP session id for new session"""
        # does not check collisions
        sessionid = peer.get(Opener.STATE_SESSIONID, 0) % 255 + 1
        peer[Opener.STATE_SESSIONID] = sessionid
        return sessionid

    def _get_keepalive(self, peer, session):
        """Return PCEP session local keepalive for new session"""
        return session.get(Keeper.CONFIG_KEEPALIVE, Keeper.KEEPALIVE)

    def _get_deadtimer(self, peer, session):
        """Return PCEP session local deadtimer for new session"""
        keepalive = session.get(Keeper.STATE_KEEPALIVE, Keeper.KEEPALIVE)
        deadtimer = session[Keeper.CONFIG_DEADTIMER]
        if deadtimer is None or deadtimer < keepalive:
            deadtimer = min(4 * keepalive, 255)
        return deadtimer

    def _make_open(self, peer, session):
        open = _message.object.Open()
        open.session_id = session[Opener.STATE_SESSIONID]
        open.keepalive = session[Keeper.STATE_KEEPALIVE]
        open.deadtimer = session[Keeper.STATE_DEADTIMER]

        pceptype = session[Opener.STATE_PCEPTYPE]
        if pceptype != Opener.PCEPTYPE_STATELESS:
            db_version = session[Opener.CONFIG_DB_VERSION]
            node_id = session[Opener.CONFIG_NODE_ID]

            spc = _message.tlv.StatefulPcepCapability()
            spc.updating = (pceptype == Opener.PCEPTYPE_STATEFULA or pceptype == Opener.PCEPTYPE_STATEFULI)
            spc.instantiation = pceptype == Opener.PCEPTYPE_STATEFULI
            spc.include_db_version = db_version is not None
            open.add(spc)

            if db_version is not None and db_version != -1:
                open.add(_message.tlv.LspStateDbVersion(db_version=db_version))

            if node_id:
                open.add(_message.tlv.NodeIdentifier(node_id=node_id))
        return open

    def _send_open(self, peer, session, open):
        session[Opener.STATE_LOCAL_OPEN] = open
        message = _message.Open()
        message.add(open)
        session.send(message)
        # Neglect time spent serializing and transmitting
        session[Opener.STATE_KEEPWAIT] = resolve_timeout(Opener.KEEPWAIT)

    def _accept_open(self, peer, session, open):
        """Accept an open object from remote peer's open message.
        Returns True if accepted, False if not negotiable,
        or Open object with proposed acceptable parameters.
        """
        pceptype = session[Opener.STATE_PCEPTYPE]
        spc = open.get(_message.tlv.StatefulPcepCapability)
        if not spc:
            return pceptype == Opener.PCEPTYPE_STATELESS
        if spc.updating != (pceptype == Opener.PCEPTYPE_STATEFULA):
            return False
        if spc.instantiation != (pceptype == Opener.PCEPTYPE_STATEFULI):
            return False
        return True

    def _accept_error(self, peer, session, open):
        """Accept an open object from remote peer's error message.
        Returns new Open object to be sent, or None if unacceptable.
        """
        pass  # may be implemented later

    def _session_open(self, peer, session):
        """Bring session up and setup negotiated session state"""
        session[Opener.STATE_STATE] = Opener.OS_UP

        def __use_dbv(open):
            if not open:
                return False
            spc = open.get(_message.tlv.StatefulPcepCapability)
            return spc is not None and spc.include_db_version

        session[Opener.STATE_USE_DBV] = (
            session[Opener.STATE_PCEPTYPE] != Opener.PCEPTYPE_STATELESS
            and __use_dbv(session[Opener.STATE_LOCAL_OPEN])
            and __use_dbv(session[Opener.STATE_REMOTE_OPEN])
        )
        peer.emit('on_session_open', session=session)


class Keeper(Handler):
    """Handle keepalives and deadtimers (except configuration)"""

    CONFIG_KEEPALIVE = 'session.keepalive'
    CONFIG_DEADTIMER = 'session.deadtimer'

    STATE_KEEPALIVE = '_keeper.keepalive'
    STATE_DEADTIMER = '_keeper.deadtimer'
    STATE_REMOTE_KEEPALIVE = '_keeper.remote_keepalive'
    STATE_REMOTE_DEADTIMER = '_keeper.remote_deadtimer'

    STATE_KEEPALIVEAT = '_keeper.keepalive_at'
    STATE_DEADTIMERAT = '_keeper.deadtimer_at'

    KEEPALIVE = 30

    def on_session_open(self, peer, eventargs):
        session = eventargs['session']
        remote_open = session[Opener.STATE_REMOTE_OPEN]
        session[Keeper.STATE_REMOTE_KEEPALIVE] = remote_open.keepalive
        session[Keeper.STATE_REMOTE_DEADTIMER] = remote_open.deadtimer

        self._set_keepalive(peer, session)
        self._set_deadtimer(peer, session)

    def on_message(self, peer, eventargs):
        self._set_deadtimer(peer, eventargs['session'])

    def on_transmit(self, peer, eventargs):
        self._set_keepalive(peer, eventargs['session'])

    def on_timeout(self, peer, eventargs):
        session = eventargs['session']
        if session.closing:
            return
        now = eventargs['now']
        timeout = session[Keeper.STATE_DEADTIMERAT]
        if timeout and timeout <= now:
            peer.make_pcep_error(
                origin=self,
                session=session,
                cause=(now, timeout),
                send=_message.code.Close.DeadtimerExpired,
                closing=True)
            return

        timeout = session[Keeper.STATE_KEEPALIVEAT]
        if timeout and timeout <= now:  # should always happen
            del session[Keeper.STATE_KEEPALIVEAT]
            session.send(_message.Keepalive())

    def on_pcep_error(self, peer, eventargs):
        if eventargs.get('closing'):
            session = eventargs['session']
            del session[Keeper.STATE_KEEPALIVEAT]
            del session[Keeper.STATE_DEADTIMERAT]

    def timeout(self, session):
        return min_timeout(
            session[Keeper.STATE_DEADTIMERAT], session[Keeper.STATE_KEEPALIVEAT]
        )

    def _set_keepalive(self, peer, session):
        """Set next keepalive timeout"""
        keepalive = session[Keeper.STATE_KEEPALIVE]
        if keepalive and not session.closing:
            session[Keeper.STATE_KEEPALIVEAT] = resolve_timeout(keepalive)
            _LOGGER.debug('Setting keepalive timeout to %0.1f on %s' %
                          (session[Keeper.STATE_KEEPALIVEAT], session))
        else:
            del session[Keeper.STATE_DEADTIMERAT]

    def _set_deadtimer(self, peer, session):
        """Set next deadtimer timeout"""
        if session.closing or not session[Keeper.STATE_REMOTE_KEEPALIVE]:
            del session[Keeper.STATE_DEADTIMERAT]
            return
        session[Keeper.STATE_DEADTIMERAT] = resolve_timeout(
            session[Keeper.STATE_REMOTE_DEADTIMER]
        )
        _LOGGER.debug('Setting deadtimer timeout to %0.1f on %s' %
                      (session[Keeper.STATE_DEADTIMERAT], session))


class Closer(Handler):
    """Handle session closure"""

    def on_message(self, peer, eventargs):
        session = eventargs['session']
        message = eventargs['message']
        if isinstance(message, _message.Close):
            session.closing = True

    def on_bad_header(self, peer, eventargs):
        session = eventargs['session']
        header = eventargs['header']
        peer.make_pcep_error(
            origin=self,
            session=session,
            cause=header,
            send=_message.code.Close.MalformedMessage,
            closing=True)

    def on_close(self, peer, eventargs):
        session = eventargs['session']
        if not session.is_server():
            peer._sessions.remove(session)


class Logger(Handler):
    """Log every event; possibly tracing its call stack"""

    def __init__(self):
        super(Logger, self).__init__()
        self.traces = list()

    def report_event(self, name, peer, eventargs):
        args = ' # '.join(['%s = %s' % item for item in eventargs.items()])
        msg = 'Event %s on peer %s: %s' % (name, peer, args)
        if name in self.traces:
            msg += "\n%s" % ''.join(traceback.format_stack())
        _LOGGER.debug(msg)

    def __getattr__(self, name):
        if name.startswith('on_'):
            meth = lambda peer, eventargs: self.report_event(name, peer, eventargs)
            self.__dict__[name] = meth
            return meth
        raise AttributeError(name)
