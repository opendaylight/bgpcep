# Constants; Error, notification and other codes with their descriptions

# Copyright (c) 2012,2013 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import logging
_LOGGER = logging.getLogger('pcepy.message')

PCEP_VERSION = 1


class _CodeMeta(type):
    """Metaclass for Code classes: Converts the _values definitions
    into true objects and adds access to individual components by _names"""

    # Default value for the second level attribute
    _value = 0

    class _GetCode(object):
        def __init__(self, index):
            self._index = index

        def __get__(self, instance, cls):
            return instance.get_code(self._index)

    def __new__(mcs, name, bases, attrs):
        sub_default = attrs.get('_value')
        if sub_default is None:
            sub_default = mcs._value
            attrs['_value'] = sub_default

        cls = super(_CodeMeta, mcs).__new__(mcs, name, bases, attrs)

        if '_names' in attrs:
            for index, name in enumerate(attrs['_names']):
                setattr(cls, name, mcs._GetCode(index))

        if '_values' in attrs:
            codes = dict()
            cls._codes = codes
            for name, defs in attrs['_values'].items():
                if not isinstance(defs, tuple):
                    defs = (defs, name)
                main_code = defs[0]
                main_expl = defs[1]

                code = (main_code, sub_default)
                code_instance = cls(
                    name=name,
                    explanation=main_expl,
                    code=code,
                    known=True,
                )
                setattr(cls, name, code_instance)
                codes[code] = code_instance

                if len(defs) == 2:
                    continue

                for sub_name, sub_defs in defs[2].items():
                    if not isinstance(sub_defs, tuple):
                        sub_defs = (sub_defs, sub_name)
                    sub_name = name + '_' + sub_name
                    code = (main_code, sub_defs[0])
                    code_instance = cls(
                        name=sub_name,
                        explanation=main_expl + ': ' + sub_defs[1],
                        code=code,
                        known=True,
                    )
                    setattr(cls, sub_name, code_instance)
                    codes[code] = code_instance

        return cls

# Kludge for python 2 and 3 syntax compatibility
_CodeBase = _CodeMeta('_CodeBase', (object, ), dict(
    # updated by metaclass
    _value=0,
    _codes=None,
))


class _Code(_CodeBase):
    """Base class for code-holding types

    Subclassess shall use a list _names for code component names (at most two)
    and dict _values for code value definitions (see example uses for syntax).
    """
    # Explanation of an unknown code
    unknown_name = 'Unknown'
    unknown_explanation = 'Unknown'

    @classmethod
    def from_code(cls, code):
        """Return a code object corresponding to code value or tuple"""
        if not isinstance(code, tuple):
            code = (code, cls._value)
        result = cls._codes.get(code)
        if result is None:  # Create one
            parent = cls._codes.get((code[0], cls._value))
            if parent is not None:
                name = parent.name + '_' + cls.unknown_name
                expl = parent.explanation + ': ' + cls.unknown_explanation
            else:
                name = cls.unknown_name
                expl = cls.unknown_explanation

            result = cls(
                name=name,
                explanation=expl,
                code=code,
                known=False,
            )
            cls._codes[code] = result
            _LOGGER.debug('Created unknown code "%s"' % result)
        return result

    def __init__(self, name, code, explanation, known=False):
        self._name = name
        self._code = code
        self._explanation = explanation
        self._known = known

    def get_code(self, index):
        return self._code[index]

    @property
    def name(self):
        return self._name

    @property
    def explanation(self):
        return self._explanation

    @property
    def is_known(self):
        """"Return whether the code is known by the library (and PCEP)"""
        return self._known

    def __str__(self):
        return '%s.%s[%s]: %s' % (
            self.__class__.__name__,
            self._name,
            ', '.join(['%s: %s' % codeval
                      for codeval in zip(self._names, self._code)]),
            self._explanation
        )


# RFC 5440 PCEP
class Error(_Code):
    """Content of an ERROR object"""

    _names = ['error_type', 'error_value']

    _values = dict(
        EstablishmentFailure=(1, 'Session establishment failure', dict(
            ReceivedNotOpen=(1, 'Reception of an invalid Open message or non-Open message'),

            OpenWaitExpired=(2, 'No Open message received after the expiration of the OpenWait timer'),

            Nonnegotiable=(3, 'Unacceptable and non-negotiable session characteristics'),

            Negotiable=(4, 'Unacceptable but negotiable session characteristics'),

            StillUnacceptable=(5, 'Reception of an Open message with still unacceptable session characteristics'),

            ErrorUnacceptable=(6, 'Reception of an Error message proposing unacceptable session characteristics'),

            KeepWaitExpired=(7, 'No KeepAlive or PCErr received before expiration of the KeepWait timer'),

            PcepVersion=(8, 'PCEP version not supported'),
        )),


        CapabilityNotSupported=(2, 'Capability not supported'),

        UnknownObject=(3, 'Unknown object', dict(
            Class=1,
            Type=2,
        )),

        UnsupportedObject=(4, 'Not supported object', dict(
            Class=1,
            Type=2,
        )),

        PolicyViolation=(5, 'Policy violation', dict(
            MetricCBit=(1, 'C bit of METRIC object set (request rejected)'),

            RpOBit=(2, 'O bit of RP object set (request rejected)'),

            # RFC 5541 OF
            OFunction=(3, 'Objective function not allowed (request rejected)'),

            RpOFBit=(4, 'OF bit of RP object set (request rejected)'),

            # RFC 5886 MON
            Monitoring=(6, 'Monitoring message supported but rejected'),
        )),

        MandatoryObjectMissing=(6, 'Mandatory object missing', dict(
            RP=(1, 'RP object missing'),
            RRO=(2, 'RRO object missing for a reoptimization request'),
            EP=(3, 'END-POINTS object missing'),

            # RFC 5886 MON
            MON=(4, 'MONITORING object missing'),

            # D STATEFUL
            LSP=(8, 'LSP object missing'),
            ERO=(9,
                 'ERO object missing for a path in an LSP Update Request' +
                 ' where TE-LSP setup is requested'),
            BW=(10,
                'BANDWIDTH object missing for a path in an LSP Update Request' +
                ' where TE-LSP setup is requested'),
            LSPA=(11,
                  'BANDWIDTH object missing for a path in an LSP Update Request' +
                  ' where TE-LSP setup is requested'),
            DBV=(12, 'LSP-DB-VERSION TLV is missing'),
        )),

        SyncRpMissing=(7, 'Synchronized path computation request missing'),

        UnknownRequest=(8, 'Unknown request reference'),

        SessionEstablished=(9, 'Attempt to establish a second PCEP session'),

        InvalidObject=(10, 'Reception of an invalid object', dict(
            PFlagNotSet=(1, 'P flag not set although required by RFC5440'),
        )),

        # RFC 5521 XRO
        UnrecognizedExrs=(11, 'Unrecognized EXRS subobject'),

        # D STATEFUL
        InvalidOperation=(19, 'Invalid operation', dict(
            LspNotDelegated=(1, 'Attempted LSP update request for a non-delegated LSP'),
            DelegationNotActive=(
                2,
                'Attempted LSP update request if active stateful' +
                ' PCE capability was not negotiated'),
        )),

        # D STATEFUL
        StateSync=(20, 'LSP state synchronization error', dict(
            CannotProcess=(1, 'State report cannot be processed'),
            BadDbVersion=(2, 'LSP database version mismatch'),
            NoDbVersion=(3, 'LSP-DB-VERSION TLV missing when state synchronization avoidance enabled'),
        )),
    )  # Error._value


# RFC 5440 PCEP
class Close(_Code):
    """Reason for closure of an PCEP session"""

    _names = ['reason']

    _values = dict(
        NoExplanation=(1, 'No explanation'),
        DeadtimerExpired=(2, 'DeadTimer expired'),
        MalformedMessage=(3, 'Reception of a malformed PCEP message'),
        TooManyUnknown=(4, 'Reception of an unacceptable number of unknown requests or responses'),
        TooManyUnrecognized=(5, 'Reception of an unacceptable number of unrecognized PCEP messages'),
    )


# RFC 5440 PCEP
class Notification(_Code):
    """Contents of an Notification object"""

    _names = ['notify_type', 'notify_value']

    _values = dict(
        RequestCancelled=(1, 'Pending request cancelled', dict(
            ByPcc=(1, 'Cancelled by PCC'),
            ByPce=(2, 'Cancelled by PCE'),
        )),

        OverloadedPce=(2, 'Overloaded PCE', dict(
            Now=(1, 'Overloaded'),
            End=(2, 'Overload ended'),
        )),
    )


# RFC 5440 PCEP
class Metric(_Code):
    """Known type of METRIC objects"""

    _names = ['metric_type']

    _values = dict(
        IGP=1,
        TE=2,
        HOPS=3,

        # RFC 5541 OF
        ABC=(4, 'Aggregate bandwidth consumption'),
        LMLL=(5, 'Load of the most loaded link'),
        CIC=(6, 'Cumulative IGP cost'),
        CTC=(7, 'Cumulative TE cost'),

        # RFC 6006 P2MP
        P2MI=(8, 'P2MP IGM'),
        P2MT=(9, 'P2MP TE'),
        P2MH=(10, 'P2MP HOPS')
    )


# D STATEFUL
class LspUpdateError(_Code):
    """Error code inside an LSP Update Error Code TLV"""

    _names = ['lsp_update_error_code']

    _values = dict(
        SetupFail=(1, 'Setup failed outside of node'),
        LspDown=(2, 'LSP not operational'))


# RFC 5440 PCEP
class NoPath(_Code):

    _names = ['nature']

    _values = dict(
        NoPathFound=(0, 'No path satisfying the set of constraints could be found'),
        PceChainBroken=(1, 'PCE chain broken'))
