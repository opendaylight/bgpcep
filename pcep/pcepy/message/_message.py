# PCEP message and implicit object definitions

# Copyright (c) 2012,2013 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

from . import base
from . import object  # Note: object here is a module


# RFC 5440 PCEP
class Open(base.Message):
    type_id = 1

    _sequence = [
        ('open', object.Open, 1),
    ]


# RFC 5440 PCEP
class Keepalive(base.Message):
    type_id = 2

    _sequence = []


# RFC 5886 MON
class GMon(base.Group):
    _sequence = [
        ('mon', object.Monitoring, 1),
        ('pcc', object.PccIdReq, 1),
        ('pce', object.PceId, '*'),
    ]


# RFC 5440 PCEP
class GRro(base.Group):
    _sequence = [
        ('rro', object.Rro, 1),
        ('rbw', object.Bandwidth, '?'),
    ]


# RFC 5440 PCEP
# Note: RFC 5520 PK says there's either a PatkKey,
# or all other objects in a request
# In RFC 5440 PCEP, Endpoints is a REQUIRED object
class Request(base.Group):
    _sequence = [
        ('rp', object.Rp, 1),
        ('pk', object.PathKey, '?'),  # RFC 5520 PK
        ('ep', object.Endpoints, '?'),  # Actually required
        ('lsp', object.Lsp, '?'),  # D STATEFUL
        ('lspa', object.Lspa, '?'),
        ('bw', object.Bandwidth, '?'),
        ('metric', object.Metric, '*'),
        ('of', object.Of, '?'),  # RFC 5541 OF
        ('grro', GRro, '?'),
        ('iro', object.Iro, '?'),
        ('lb', object.LoadBalancing, '?'),
        ('xro', object.Xro, '?'),  # RFC 5521 XRO
    ]


# RFC 5440 PCEP
class GSvec(base.Group):
    _sequence = [
        ('svec', object.Svec, 1),
        ('of', object.Of, '?'),  # RFC 5541 OF
        ('metric', object.Metric, '*'),  # RFC 5541 OF
    ]


# RFC 5440 PCEP
class PCReq(base.Message):
    type_id = 3

    _sequence = [
        ('gmon', GMon, '?'),  # RFC 5886 MON
        ('gsvec', GSvec, '*'),
        ('request', Request, '+'),
    ]


# RFC 5440 PCEP
class PcePathAttributes(base.Group):
    _sequence = [
        ('of', object.Of, '?'),  # RFC 5541 OF
        ('lspa', object.Lspa, '?'),
        ('bw', object.Bandwidth, '?'),
        ('metric', object.Metric, '*'),
        ('iro', object.Iro, '?'),  # Not in D STATEFUL-01
    ]


# RFC 5440 PCEP
class PcePath(base.Group):
    _sequence = [
        ('ero', object.Ero, 1),
        ('pcepa', PcePathAttributes, '?'),
    ]


# RFC 5886 MON
class MetricPce(base.Group):
    _sequence = [
        ('pce', object.PceId, 1),
        ('pt', object.ProcTime, '?'),
        ('ol', object.Overload, '?'),
    ]


# RFC 5440 PCEP
class Response(base.Group):
    _sequence = [
        ('rp', object.Rp, 1),
        ('gmon', GMon, '?'),  # Without pce list # RFC 5886 MON
        ('lsp', object.Lsp, '?'),  # D STATEFUL
        ('nopath', object.NoPath, '?'),
        ('pcepa', PcePathAttributes, '?'),
        ('path', PcePath, '*'),
        ('mp', MetricPce, '*'),  # RFC 5886 MON
    ]


# RFC 5440 PCEP
class PCRep(base.Message):
    type_id = 4

    _sequence = [
        ('response', Response, '+'),
    ]


# RFC 5440 PCEP
class GNotify(base.Group):
    _sequence = [
        ('rp', object.Rp, '*'),
        ('notify', object.Notification, '+'),
    ]


# RFC 5440 PCEP
class PCNtf(base.Message):
    type_id = 5

    _sequence = [
        ('gnotify', GNotify, '+'),
    ]


# RFC 5440 PCEP
class GError(base.Group):
    """Unified PCEP error item"""
    _sequence = [
        ('rp', object.Rp, '*'),
        ('error', object.Error, '+'),
        ('lsp', object.Lsp, '?'),  # D STATEFUL
    ]


# RFC 5440 PCEP
class PCErr(base.Message):
    type_id = 6

    _sequence = [
        ('gerror', GError, '+'),
        ('open', object.Open, '?'),
    ]


# RFC 5440 PCEP
class Close(base.Message):
    type_id = 7

    _sequence = [
        ('close', object.Close, 1)
    ]


# RFC 5886 MON
class PCMonReq(base.Message):
    type_id = 8

    _sequence = [
        ('gmon', GMon, 1),
        ('gsvec', GSvec, '*'),
        ('request', Request, '+'),
    ]


# RFC 5886 MON
class PCMonRep(base.Message):
    type_id = 9

    _sequence = [
        ('gmon', GMon, '?'),  # Without pce list
        ('rp', object.Rp, '*'),
        ('mp', MetricPce, '*'),
    ]


# D STATEFUL
class PccPath(base.Group):
    _sequence = [
        ('ero', object.Ero, 1),
        ('lspa', object.Lspa, '?'),
        ('bw', object.Bandwidth, '?'),
        ('rro', object.Rro, '?'),
        ('metric', object.Metric, '*'),
    ]


# D STATEFUL
class StateReport(base.Group):
    _sequence = [
        ('lsp', object.Lsp, 1),
        ('path', PccPath, '*'),
    ]


# D STATEFUL
class PCRpt(base.Message):
    type_id = 10

    _sequence = [
        ('report', StateReport, '+'),
    ]


# D STATEFUL
class UpdateRequest(base.Group):
    _sequence = [
        ('lsp', object.Lsp, 1),
        ('path', PcePath, '*'),
    ]


# D STATEFUL
class PCUpd(base.Message):
    type_id = 11

    _sequence = [
        ('update', UpdateRequest, '+'),
    ]


# D STATEFUL
class CreateRequest(base.Group):
    _sequence = [
        ('end-points', object.Endpoints, 1),
        ('lspa', object.Lspa, 1),
        ('ero', object.Ero, '?'),
        ('bw', object.Bandwidth, '?'),
        ('metric', object.Metric, '*'),
    ]


# D STATEFUL
class PCCreate(base.Message):
    type_id = 12

    _sequence = [
        ('create', CreateRequest, '+'),
    ]


# We do not need special handling for unknown messages
class Unknown(base.Message):
    pass
base.Message.unknown_class = Unknown
