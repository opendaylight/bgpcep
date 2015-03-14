# RSVP-TE subobjects allowed inside PCEP ERO/IRO/RRO/XRO/PK objects

# Copyright (c) 2012,2013 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

# The respective classes are unions of all their definitions;
# their fields may and do overlap - one such field must be used at a time;
# additional constraints should be used for validation.

from . import data
from . import base


# RFC 5440 PCEP -> RFC 3209 RSVP-TE LSP
class Ipv4Prefix(base.Rsvp):
    type_id = 1
    address = data.Ipv4(offset=0)
    prefix_length = data.Int(offset=32, size=8)
    lp_available = data.Flag(offset=32+15)       # RRO
    lp_in_use = data.Flag(offset=32+14)          # RRO
    attribute = data.Int(offset=32+8, size=8)    # RFC 4874 RSVP-TE XRO


# RFC 5440 PCEP -> RFC 3209 RSVP-TE LSP
class Ipv6Prefix(base.Rsvp):
    type_id = 2
    address = data.Ipv6(offset=0)
    prefix_length = data.Int(offset=128, size=8)
    lp_available = data.Flag(offset=128+15)      # RRO
    lp_in_use = data.Flag(offset=128+14)         # RRO
    attribute = data.Int(offset=128+8, size=8)   # RFC 4874 RSVP-TE XRO

Prefix = Ipv4Prefix, Ipv6Prefix


# RFC 5440 PCEP -> RFC 3209 RSVP-TE LSP
# Defined for RRO;
# RFC 3471 and 3473 define a somewhat different label for both ERO and RRO
class Label(base.Rsvp):
    type_id = 3
    upstream = data.Flag(offset=0)
    global_label = data.Flag(offset=7)
    ctype = data.Int(offset=8, size=8, value=1)
    label = data.Int(offset=16, size=32)


# RFC 5440 PCEP -> RFC 3477 RSVP-TE UNNUMBERED
class UnnumberedInterface(base.Rsvp):
    type_id = 4
    lp_available = data.Flag(offset=7)           # RRO
    lp_in_use = data.Flag(offset=6)              # RRO
    router_id = data.Int(offset=16, size=32)
    interface_id = data.Int(offset=48, size=32)


# RFC 5521 XRO -> RFC 4874 RSVP-TE XRO
class Srlg(base.Rsvp):
    type_id = 34
    srlg_id = data.Int(offset=0, size=32)
    attribute = data.Int(offset=40, size=8, value=2)


# RFC 5440 PCEP -> RFC 3209 RSVP-TE LSP
class ASNumber(base.Rsvp):
    type_id = 32
    as_number = data.Int(offset=0, size=16)


# RFC 5520 PK
class Ipv4PathKey(base.Rsvp):
    type_id = 64
    path_key = data.Int(offset=0, size=16)
    pce_id = data.Ipv4(offset=16)


# RFC 5520 PK
class Ipv6PathKey(base.Rsvp):
    type_id = 65
    path_key = data.Int(offset=0, size=16)
    pce_id = data.Ipv6(offset=16)

PathKey = (Ipv4PathKey, Ipv6PathKey)


# RFC 5521 XRO
class Exr(base.Rsvp):
    """The Explicit Route Exclusion is another RouteObject inside an Iro."""
    type_id = 33
    _fixed = False

    _unset_15 = data.Unset(offset=15)

    class ExrRoHeader(base.ObjectHeader):
        """Dummy header for ExrRo. Does not read nor write itself."""

        def read(self, buf, off, end):
            return off

        def write(self, buf, off):
            return off

        def _get_size(self):
            return 0

    class ExrRo(base.RouteObject):
        """The Route Object contained inside an Exr subobject"""
        class_id = 0xFF
        type_id = 0xF

    ExrRo._header_class = ExrRoHeader

    def __init__(self, *items, **updates):
        if 'clone' in updates:
            clone = updates['clone']
            del updates['clone']
        else:
            clone = None
        if clone is None:
            self._ro = Exr.ExrRo()
        else:
            self._ro = Exr.ExrRo(clone=clone.ro)
        super(Exr, self).__init__(clone=clone)
        if updates:
            self.update(updates)
        self._ro.items.extend(items)

    @property
    def ro(self):
        """The delegated Route Object"""
        return self._ro

    @ro.setter
    def ro(self, ro):
        self._ro = ro

    def _get_size(self):
        size = super(Exr, self)._get_size() + self._ro.size
        self._header.length = size
        return size

    def update(self, updates):
        updated = super(Exr, self).update(updates)
        if 'ro' in updates:
            updated += self._ro.update(updates['ro'])
        return updated

    def read(self, buf, off, end):
        ono = super(Exr, self).read(buf, off, end)
        if self._header.length != end - off:
            base._LOGGER.error('Length %s in Exr header not matching [%s:%s]' %
                               (self._header.length, off, end))
        self._ro.header.length = end - ono
        return self._ro.read(buf, ono, end)

    def write(self, buf, off):
        off = super(Exr, self).write(buf, off)
        return self._ro.write(buf, off)

    def __str__(self):
        sup = super(Exr, self).__str__()
        if self._ro.items:
            sup += '[ %s ]' % (
                ' + '.join([str(i) for i in self._ro.items])
            )
        return sup


class Unknown(base.Rsvp):
    """RSVP-TE subobject with unknown type and arbitrary data."""
    _fixed = False

    def __init__(self, clone=None, **updates):
        self._octets = b'\0\0' if clone is None else clone.octets
        super(Unknown, self).__init__(clone=clone)
        if updates:
            self.update(updates)

    @property
    def octets(self):
        return self._octets

    @octets.setter
    def octets(self, octets):
        octets = bytes(octets)
        head = self._header.size
        padlen = data.padlen(len(octets) + head, 4)
        self._octets = data.padded(octets, length=padlen-head)
        self._header.length = padlen

    def _get_size(self):
        return self._header.size + len(self._octets)

    def update(self, updates):
        updated = super(Unknown, self).update(updates)
        if 'octets' in updates:
            self.octets = updates['octets']
            updated += 1
        return updated

    def read(self, buf, off, max_end):
        off = super(Unknown, self).read(buf, off, max_end)
        length = self._header.length - self._header.size
        end = off + length
        if end > max_end:
            base._LOGGER.error("Rsvp data length (%s) exceeds limit [%s:%s]" %
                               (length, off, max_end))
        end = max_end  # always read the whole portion
        self._octets = buf[off:end]
        return end

    def write(self, buf, off):
        base._LOGGER.warn("Writing Unknown [%s]" % self._header)
        off = super(Unknown, self).write(buf, off)
        end = off + len(octets)  # noqa
        buf[off:end] = octets  # noqa
        return end

    def __str__(self):
        return '%soctets="%s"' % (
            super(Unknown, self).__str__(),
            data.to_hex(self._octets)
        )

base.Rsvp.unknown_class = Unknown
