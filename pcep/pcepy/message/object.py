# PCEP message objects

# Copyright (c) 2012, 2015 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import struct
import ipaddr

from . import data
from . import base
from . import code


# RFC 5440 PCEP
class Open(base.Object):
    class_id = 1
    type_id = 1

    version = data.Int(offset=0, size=3, value=code.PCEP_VERSION)
    keepalive = data.Int(offset=8, size=8)
    deadtimer = data.Int(offset=16, size=8)
    session_id = data.Int(offset=24, size=8)


# RFC 5440 PCEP
class Rp(base.Object):
    class_id = 2
    type_id = 1

    fragmentation = data.Flag(offset=18)    # RFC 6006 P2MP
    p2mp = data.Flag(offset=19)             # RFC 6006 P2MP
    ero_compression = data.Flag(offset=20)  # RFC 6006 P2MP
    make_bf_break = data.Flag(offset=21)    # RFC 5557 GCO
    report_order = data.Flag(offset=22)     # RFC 5557 GCO
    path_key = data.Flag(offset=23)         # RFC 5520 PK
    supply_of = data.Flag(offset=24)        # RFC 5541 OF
    vspt = data.Flag(offset=25)             # RFC 5441 BRPC
    strict = data.Flag(offset=26)           # RFC 5440 PCEP
    bidirectional = data.Flag(offset=27)
    reoptimize = data.Flag(offset=28)
    priority = data.Int(offset=29, size=3)
    rp_id = data.Int(offset=32, size=32)

    def __init__(self, *items, **updates):
        super(Rp, self).__init__(*items, **updates)
        self._header.process = True


# RFC 5440 PCEP
class NoPath(base.Object):
    class_id = 3
    type_id = 1

    nature = data.Int(offset=0, size=8)
    constraints = data.Flag(offset=8)

    @property
    def code(self):
        return code.NoPath.from_code(self.nature)

    @code.setter
    def code(self, code):
        self.nature = code.nature

    def update(self, updates):
        updated = super(NoPath, self).update(updates)
        if 'code' in updates:
            self.code = updates['code']
            updated += 1
        return updated


# RFC 5440 PCEP
class Ipv4Endpoints(base.Object):
    class_id = 4
    type_id = 1

    source = data.Ipv4(offset=0)
    destination = data.Ipv4(offset=32)

    def __init__(self, *items, **updates):
        if 'source' in updates:
            self.source = int(ipaddr.IPv4Address(updates['source']))
            del updates['source']
        if 'destination' in updates:
            self.destination = int(ipaddr.IPv4Address(updates['destination']))
            del updates['destination']

        super(Ipv4Endpoints, self).__init__(*items, **updates)
        self._header.process = True


# RFC 5440 PCEP
class Ipv6Endpoints(base.Object):
    class_id = 4
    type_id = 2

    source = data.Ipv6(offset=0)
    destination = data.Ipv6(offset=128)

    def __init__(self, *items, **updates):
        if 'source' in updates:
            self.source = int(ipaddr.IPv6Address(updates['source']))
            del updates['source']
        if 'destination' in updates:
            self.destination = int(ipaddr.IPv6Address(updates['destination']))
            del updates['destination']

        super(Ipv6Endpoints, self).__init__(*items, **updates)
        self._header.process = True


Endpoints = Ipv4Endpoints, Ipv6Endpoints


# RFC 5440 PCEP
class Bandwidth(base.Object):
    class_id = 5
    type_id = 1

    bandwidth = data.Float(offset=0)


# RFC 5440 PCEP
class Metric(base.Object):
    class_id = 6
    type_id = 1

    bound = data.Flag(offset=23)
    compute = data.Flag(offset=22)
    metric_type = data.Int(offset=24, size=8)
    metric = data.Float(offset=32)

    @property
    def code(self):
        return code.Metric.from_code(self.metric_type)

    @code.setter
    def code(self, code):
        self.metric_type = code.metric_type

    def update(self, updates):
        updated = super(Metric, self).update(updates)
        if 'code' in updates:
            self.code = updates['code']
            updated += 1
        return updated


# RFC 5440 PCEP
class Ero(base.RouteObject):
    class_id = 7
    type_id = 1


# RFC 5440 PCEP
class Rro(base.RouteObject):
    class_id = 8
    type_id = 1


# RFC 5440 PCEP
class Lspa(base.Object):
    class_id = 9
    type_id = 1

    exclude_any = data.Int(offset=0, size=32)
    include_any = data.Int(offset=32, size=32)
    include_all = data.Int(offset=64, size=32)

    setup_priority = data.Int(offset=96, size=8)
    holding_priority = data.Int(offset=104, size=8)
    lp_desired = data.Flag(offset=119)


# RFC 5440 PCEP
class Iro(base.RouteObject):
    class_id = 10
    type_id = 1


# RFC 5440 PCEP
class Svec(base.Object):
    class_id = 11
    type_id = 1
    _allow_items = False
    __rp_id_sup = 1 << 32

    link_diverse = data.Flag(offset=31)
    node_diverse = data.Flag(offset=30)
    srlg_diverse = data.Flag(offset=29)

    linkdir_diverse = data.Flag(offset=20)   # RFC 6006 P2MP
    partpath_diverse = data.Flag(offset=19)  # RFC 6006 P2MP

    def __init__(self, *items, **updates):
        if 'clone' in updates:
            clone = updates['clone']
            del updates['clone']
        else:
            clone = None
        if clone is None:
            self._rp_ids = list()
        else:
            self._rp_ids = list(clone.rp_ids)
        super(Svec, self).__init__(clone=clone)
        if updates:
            self.update(updates)
        self._items.extend(items)

    @property
    def rp_ids(self):
        return self._rp_ids

    @rp_ids.setter
    def rp_ids(self, rp_ids):
        if isinstance(rp_ids, (list, tuple)):
            self._rp_ids.extend(rp_ids)
        else:
            self._rp_ids.append(rp_ids)

    def _get_size(self):
        size = super(Svec, self)._get_size() + 4 * len(self._rp_ids)
        self._header.length = size
        return size

    def update(self, updates):
        updated = super(Svec, self).update(updates)
        if 'rp_ids' in updates:
            self.rp_ids = updates['rp_ids']
            updated += 1
        return updated

    def read(self, buf, off, max_end):
        ono = super(Svec, self).read(buf, off, max_end)
        end = off + self._header.length
        if end > max_end:
            _errmsg = ("SVEC length (%s) exceeds limit [%s:%s]" %
                       (self._header.length, off, max_end))
            raise data.SizeError(_errmsg)
        rp_ids = self._rp_ids
        trail = (end - ono) % 4
        if trail:
            blob = data.Blob()
            blob.read(buf, end - trail, end)
            self._items.append(blob)
            end -= trail
        rp_octets = bytes(buf[ono:end])
        rp_at = 0
        while ono < end:
            rp_ids.extend(struct.unpack_from(">I", rp_octets, rp_at))
            rp_at += 4
            ono += 4
        return ono + trail

    def write(self, buf, off):
        items = self._items
        self._items = None
        off = super(Svec, self).write(buf, off)
        self._items = items
        for rp_id in self._rp_ids:
            if rp_id >= Svec.__rp_id_sup:
                base._LOGGER.error('RP id (%d) cannot fit 32 bits' % rp_id)
                rp_id %= Svec.__rp_id_sup
            struct.pack_into(">I", buf, off, rp_id)
            off += 4
        for item in items:
            off = item.write(buf, off)
        return off

    def __str__(self):
        return '%s, rp_ids=[%s]' % (
            super(Svec, self).__str__(),
            ', '.join([str(rp_id) for rp_id in self._rp_ids])
        )


# RFC 5440 PCEP
class Notification(base.Object):
    class_id = 12
    type_id = 1

    notify_type = data.Int(offset=16, size=8)
    notify_value = data.Int(offset=24, size=8)

    @property
    def code(self):
        return code.Notification.from_code(
            (self.notify_type, self.notify_value)
        )

    @code.setter
    def code(self, code):
        self.notify_type = code.notify_type
        self.notify_value = code.notify_value

    def update(self, updates):
        updated = super(Notification, self).update(updates)
        if 'code' in updates:
            self.code = updates['code']
            updated += 1
        return updated

    def __str__(self):
        return str(self.code)


# RFC 5440 PCEP
class Error(base.Object):
    class_id = 13
    type_id = 1

    error_type = data.Int(offset=16, size=8)
    error_value = data.Int(offset=24, size=8)

    @property
    def code(self):
        return code.Error.from_code(
            (self.error_type, self.error_value)
        )

    @code.setter
    def code(self, code):
        self.error_type = code.error_type
        self.error_value = code.error_value

    def update(self, updates):
        updated = super(Error, self).update(updates)
        if 'code' in updates:
            self.code = updates['code']
            updated += 1
        return updated

    def __str__(self):
        return str(self.code) + self._str_items()


# RFC 5440 PCEP
class LoadBalancing(base.Object):
    class_id = 14
    type_id = 1

    max_lsp = data.Int(offset=24, size=8)
    min_bandwidth = data.Float(offset=32)


# RFC 5440 PCEP
class Close(base.Object):
    class_id = 15
    type_id = 1

    reason = data.Int(offset=24, size=8)

    @property
    def code(self):
        return code.Close.from_code(self.reason)

    @code.setter
    def code(self, code):
        self.reason = code.reason

    def update(self, updates):
        updated = super(Close, self).update(updates)
        if 'code' in updates:
            self.code = updates['code']
            updated += 1
        return updated

    def __str__(self):
        return str(self.code) + self._str_items()


# RFC 5520 PK
class PathKey(base.RouteObject):
    class_id = 16
    type_id = 1


# RFC 5521 XRO
class Xro(base.RouteObject):
    class_id = 17
    type_id = 1

    fail = data.Flag(offset=31)


# RFC 5886 MON
class Monitoring(base.Object):
    class_id = 19
    type_id = 1

    liveness = data.Flag(offset=31)
    general = data.Flag(offset=30)
    processing_time = data.Flag(offset=29)
    overload = data.Flag(offset=28)
    incomplete = data.Flag(offset=27)
    monitoring_id = data.Int(offset=32, size=32)


# RFC 5886 MON
class Ipv4PccIdReq(base.Object):
    class_id = 20
    type_id = 1

    address = data.Ipv4(offset=0)


# RFC 5886 MON
class Ipv6PccIdReq(base.Object):
    class_id = 20
    type_id = 2

    address = data.Ipv6(offset=0)

PccIdReq = (Ipv4PccIdReq, Ipv6PccIdReq)


# RFC 5541 OF
class Of(base.Object):
    class_id = 21
    type_id = 1

    of_code = data.Int(offset=0, size=16)


# RFC 5886 MON
class Ipv4PceId(base.Object):
    class_id = 25
    type_id = 1

    address = data.Ipv4(offset=0)


# RFC 5886 MON
class Ipv6PceId(base.Object):
    class_id = 25
    type_id = 2

    address = data.Ipv6(offset=0)

PceId = (Ipv4PceId, Ipv6PceId)


# RFC 5886 MON
class ProcTime(base.Object):
    class_id = 26
    type_id = 1

    estimated = data.Flag(offset=31)
    current = data.Int(offset=32, size=32)
    minimum = data.Int(offset=64, size=32)
    maximum = data.Int(offset=96, size=32)
    average = data.Int(offset=128, size=32)
    variance = data.Int(offset=160, size=32)


# RFC 5886 MON
class Overload(base.Object):
    class_id = 27
    type_id = 1

    duration = data.Int(offset=16, size=16)


# D STATEFUL
class Lsp(base.Object):
    class_id = 32
    type_id = 1

    lsp_id = data.Int(offset=0, size=20)
    delegate = data.Flag(offset=31)
    synchronize = data.Flag(offset=30)
    operational = data.Flag(offset=29)
    remove = data.Flag(offset=28)


class Unknown(base.Object):
    _allow_items = False

    def __init__(self, *items, **updates):
        if 'clone' in updates:
            clone = updates['clone']
            del updates['clone']
        else:
            clone = None
        self._octets = b'' if clone is None else clone.octets
        super(Unknown, self).__init__(clone=clone)
        if updates:
            self.update(updates)
        self._items.extend(items)

    @property
    def octets(self):
        return self._octets

    @octets.setter
    def octets(self, octets):
        octets = bytes(octets)
        self._octets = data.padded(octets, 4)

    def _get_size(self):
        size = self._header.size + len(self._octets)
        self._header.length = size
        return size

    def update(self, updates):
        updated = super(Unknown, self).update(updates)
        if 'octets' in updates:
            self.octets = updates['octets']
            updated += 1
        return updated

    def read(self, buf, off, max_end):
        """Read octets until max_end."""
        off = super(Unknown, self).read(buf, off, max_end)
        length = self._header.length - self._header.size
        end = off + length
        if end > max_end:
            base._LOGGER.error("Object data length (%s) exceeds limit [%s:%s]" %
                               (length, off, max_end))
        end = max_end  # always read the whole portion
        self._octets = buf[off:end]
        return end

    def write(self, buf, off):
        base._LOGGER.warning("Writing Unknown [%s]" % self._header)
        off = super(Unknown, self).write(buf, off)
        end = off + len(self._octets)
        buf[off:end] = self._octets
        return end

    def __str__(self):
        return '%soctets="%s"' % (
            super(Unknown, self).__str__(),
            data.to_hex(self._octets))

base.Object.unknown_class = Unknown
