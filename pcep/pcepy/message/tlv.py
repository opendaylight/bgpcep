# PCEP TLV definitions

# Copyright (c) 2012,2013 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

from . import data
from . import base
from . import code


# RFC 5440 PCEP
class NoPathVector(base.Tlv):
    type_id = 1
    unavailable = data.Flag(offset=31)
    destination = data.Flag(offset=30)
    source = data.Flag(offset=29)
    pks_failure = data.Flag(offset=27)        # RFC 5520 PK
    p2mp_reachability = data.Flag(offset=24)  # RFC 6006 P2MP


# RFC 5440 PCEP
class OverloadedDuration(base.Tlv):
    type_id = 2
    duration = data.Int(offset=0, size=32)


# RFC 5440 PCEP
class RequestMissing(base.Tlv):
    type_id = 3
    rp_id = data.Int(offset=0, size=32)


# RFC 5541 OF
class OfList(base.Tlv):
    type_id = 4
    _fixed = False
    __of_code_sup = 1 << 16

    def __init__(self, clone=None, **updates):
        self._of_codes = list()
        super(OfList, self).__init__(clone=clone)
        if clone is None:
            self._header.length = 0
        else:
            self._of_codes.extend(clone.of_codes)
        if updates:
            self.update(updates)

    @property
    def of_codes(self):
        return self._of_codes

    @of_codes.setter
    def of_codes(self, of_codes):
        if isinstance(of_codes, (list, tuple)):
            self._of_codes.extend(of_codes)
        else:
            self._of_codes.append(of_codes)

    def _get_size(self):
        size = 2 * len(self._of_codes)
        self._header.length = size
        return self._header.size + data.padlen(size, 4)

    def update(self, updates):
        updated = super(OfList, self).update(updates)
        if 'of_codes' in updates:
            self.of_codes = updates['of_codes']
            updated += 1
        return updated

    def read(self, buf, off, max_end):
        off = super(OfList, self).read(buf, off, max_end)
        end = off + data.padlen(self._header.length, 4)
        if self._header.length % 2:
            _errmsg = ('OfListTlv length (%s) is not even' % self._header.length)
            raise data.SizeError(_errmsg)
        if end > max_end:
            _errmsg = ('OfListTlv length (%s) exceeds limit [%s:%s]' %
                       (self._header.length, off, max_end))
            raise data.SizeError(_errmsg)
        of_codes = self._of_codes
        list_end = off + self._header.length
        while off < list_end:
            of_codes.append(buf[off] << 8 | buf[off+1])
            off += 2
        return end

    def write(self, buf, off):
        off = super(OfList, self).write(buf, off)
        end = off + data.padlen(2 * len(self._of_codes), 4)
        for of_code in self._of_codes:
            if of_code >= OfList.__of_code_sup:
                base._LOGGER.error('OF Code (%d) cannot fit 16 bits' % of_code)
            buf[off+1] = of_code & 0xFF
            of_code >>= 8
            buf[off] = of_code & 0xFF
            off += 2
        return end


# D STATEFUL
class StatefulPcepCapability(base.Tlv):
    type_id = 16
    updating = data.Flag(offset=31)
    include_db_version = data.Flag(offset=30)
    instantiation = data.Flag(offset=29)


# D STATEFUL
class LspSymbolicName(base.Tlv):
    type_id = 17
    _fixed = False

    def __init__(self, clone=None, **updates):
        self._lsp_name = b''
        self._lsp_name_length = 0
        super(LspSymbolicName, self).__init__(clone=clone)
        if clone is None:
            self._header.length = 0
        else:
            self.lsp_name = clone.lsp_name
        if updates:
            self.update(updates)

    @property
    def lsp_name(self):
        if self._lsp_name_length < len(self._lsp_name):
            return self._lsp_name[:self._lsp_name_length]
        return self._lsp_name

    @property
    def padded_lsp_name(self):
        return self._lsp_name

    @lsp_name.setter
    def lsp_name(self, lsp_name):
        lsp_name = bytes(lsp_name)
        self._lsp_name_length = len(lsp_name)
        self._header.length = self._lsp_name_length
        self._lsp_name = data.padded(lsp_name, 4)
        self._check_lsp_name()

    def _get_size(self):
        return self._header.size + len(self._lsp_name)

    def update(self, updates):
        updated = super(LspSymbolicName, self).update(updates)
        if 'lsp_name' in updates:
            self.lsp_name = updates['lsp_name']
            updated += 1
        return updated

    def read(self, buf, off, max_end):
        off = super(LspSymbolicName, self).read(buf, off, max_end)
        end = off + data.padlen(self._header.length, 4)
        if end > max_end:
            _errmsg = ('LspSymbolicNameTlv length (%s) exceeds limit [%s:%s]' %
                       (self._header.length, off, max_end))
            raise data.SizeError(_errmsg)
        self._lsp_name = buf[off:end]
        self._lsp_name_length = self._header.length
        base._LOGGER.debug(
            'Reading lsp_name from <%s>[%s:%s] = "%s" + "%s"' %
            (id(buf), off, end,
             self._lsp_name[:self._lsp_name_length],
             data.to_hex(self._lsp_name[self._lsp_name_length:])))
        self._check_lsp_name()
        return end

    def write(self, buf, off):
        self._check_lsp_name()
        off = super(LspSymbolicName, self).write(buf, off)
        end = off + len(self._lsp_name)
        buf[off:end] = self._lsp_name
        return end

    def _check_lsp_name(self):
        fails = list()
        nlen = self._lsp_name_length
        plen = len(self._lsp_name)
        if nlen <= 0 or nlen > plen:
            fails.append('length %d outside range(%d)' % (nlen, plen))
            nlen = min(max(nlen, 0), plen)
        if not bytes(self._lsp_name[0:nlen]).replace(b'_', b'').isalnum():
            fails.append('invalid character in name')
        if bytes(self._lsp_name[nlen:plen]).replace(b'\0', b''):
            fails.append('padding contains set bits')
        if fails:
            base._LOGGER.warning('LSP name "%s" check: %s' %
                                 (data.to_hex(self._lsp_name), '; '.join(fails)))
        return fails

    def __str__(self):
        return '%slsp_name="%s"' % (
            super(LspSymbolicName, self).__str__(),
            self.lsp_name
        )


# D STATEFUL
class Ipv4LspIdentifiers(base.Tlv):
    type_id = 18
    sender = data.Ipv4(offset=0)
    lsp_id = data.Int(offset=32, size=16)
    tunnel_id = data.Int(offset=32+16, size=16)
    extended_tunnel_id = data.Int(offset=32+32, size=32)


# D STATEFUL
class Ipv6LspIdentifiers(base.Tlv):
    type_id = 19
    sender = data.Ipv6(offset=0)
    lsp_id = data.Int(offset=128, size=16)
    tunnel_id = data.Int(offset=128+16, size=16)
    extended_tunnel_id = data.Int(offset=128+32, size=32)

LspIdentifiers = Ipv4LspIdentifiers, Ipv6LspIdentifiers


# D STATEFUL
class LspUpdateErrorCode(base.Tlv):
    type_id = 20
    lsp_update_error_code = data.Int(offset=0, size=32)

    @property
    def code(self):
        return code.LspUpdateError.from_code(self.lsp_update_error_code)

    @code.setter
    def code(self, code):
        self.lsp_update_error_code = code.lsp_update_error_code

    def __str__(self):
        return str(self.code)


# D STATEFUL
class Ipv4RsvpErrorSpec(base.Tlv):
    type_id = 21
    error_node = data.Ipv4(offset=0)
    in_place = data.Flag(offset=32+7)
    not_guilty = data.Flag(offset=32+6)
    error_code = data.Int(offset=32+8, size=8)
    error_value = data.Int(offset=32+16, size=16)


# D STATEFUL
class Ipv6RsvpErrorSpec(base.Tlv):
    type_id = 22
    error_node = data.Ipv6(offset=0)
    in_place = data.Flag(offset=128+7)
    not_guilty = data.Flag(offset=128+6)
    error_code = data.Int(offset=128+8, size=8)
    error_value = data.Int(offset=128+16, size=16)

RsvpErrorSpec = Ipv4RsvpErrorSpec, Ipv6RsvpErrorSpec


# D STATEFUL
class LspStateDbVersion(base.Tlv):
    type_id = 23
    db_version = data.Int(offset=0, size=64)


# D STATEFUL
class NodeIdentifier(base.Tlv):
    type_id = 24
    _fixed = False

    def __init__(self, clone=None, **updates):
        self._node_id = b''
        self._node_id_length = 0
        super(NodeIdentifier, self).__init__(clone=clone)
        if clone is None:
            self._header.length = 0
        else:
            self.node_id = clone.node_id
        if updates:
            self.update(updates)

    @property
    def node_id(self):
        if self._node_id_length < len(self._node_id):
            return self._node_id[:self._node_id_length]
        return self._node_id

    @property
    def padded_node_id(self):
        return self._node_id

    @node_id.setter
    def node_id(self, node_id):
        node_id = bytes(node_id)
        self._node_id_length = len(node_id)
        self._header.length = self._node_id_length
        self._node_id = data.padded(node_id, 4)

    def _get_size(self):
        return self._header.size + len(self._node_id)

    def update(self, updates):
        updated = super(NodeIdentifier, self).update(updates)
        if 'node_id' in updates:
            self.node_id = updates['node_id']
            updated += 1
        return updated

    def read(self, buf, off, max_end):
        off = super(NodeIdentifier, self).read(buf, off, max_end)
        end = off + data.padlen(self._header.length, 4)
        if end > max_end:
            base._LOGGER.error("Tlv value length (%s) exceeds limit [%s:%s]" %
                               (self._header.length, off, max_end))
            end = max_end
        self._node_id = buf[off:end]
        self._node_id_length = self._header.length
        base._LOGGER.debug('Reading node_id from <%s>[%s:%s] = "%s"' %
                           (id(buf), off, end, data.to_hex(self._node_id)))
        return end

    def write(self, buf, off):
        base._LOGGER.warn("Writing NodeIdentifier [%s]" % self._header)
        off = super(NodeIdentifier, self).write(buf, off)
        end = off + len(self._node_id)
        buf[off:end] = self._node_id
        return end

    def __str__(self):
        return '%snode_id="%s"' % (
            super(NodeIdentifier, self).__str__(),
            data.to_hex(self._node_id)
        )


class Unknown(base.Tlv):
    """TLV with unknown type and arbitrary value."""
    _fixed = False

    def __init__(self, clone=None, **updates):
        self._octets = b''
        self._octets_length = 0
        super(Unknown, self).__init__(clone=clone)
        if clone is None:
            self._header.length = 0
        else:
            self.octets = clone.octets
        if updates:
            self.update(updates)

    @property
    def octets(self):
        if self._octets_length < len(self._octets):
            return self._octets[:self._octets_length]
        return self._octets

    @property
    def padded_octets(self):
        return self._octets

    @octets.setter
    def octets(self, octets):
        octets = bytes(octets)
        self._octets_length = len(octets)
        self._header.length = self._octets_length
        self._octets = data.padded(octets, 4)

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
        end = off + data.padlen(self._header.length, 4)
        if end > max_end:
            base._LOGGER.error("Tlv value length (%s) exceeds limit [%s:%s]" %
                               (self._header.length, off, max_end))
            end = max_end
        self._octets = buf[off:end]
        self._octets_length = self._header.length
        base._LOGGER.debug('Reading octets from <%s>[%s:%s] = "%s"' %
                           (id(buf), off, end, data.to_hex(self._octets)))
        return end

    def write(self, buf, off):
        base._LOGGER.warn("Writing Unknown [%s]" % self._header)
        off = super(Unknown, self).write(buf, off)
        end = off + len(self._octets)
        buf[off:end] = self._octets
        return end

    def __str__(self):
        return '%soctets="%s"' % (
            super(Unknown, self).__str__(),
            data.to_hex(self._octets)
        )

base.Tlv.unknown_class = Unknown
