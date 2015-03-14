# Atomic data types and Block base

# Copyright (c) 2012, 2015 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import binascii
import struct

import logging
_LOGGER = logging.getLogger('pcepy.message')


class Bits(object):
    """A fixed single field - an atomic value occupying a continuous sequence
    of bits inside a Block"""

    # Default default value
    _value = None

    # Size of field, possibly fixed by subclass
    _size = None

    def __init__(self, **kwargs):
        super(Bits, self).__init__()
        self.name = kwargs.get('name')
        self._offset = kwargs.get('offset', 0)

        if self._size is None:
            self._size = kwargs['size']

        # Default value
        if 'value' in kwargs:
            self._value = kwargs['value']

        # The block class containing this field
        self._block = None

    def __get__(self, instance, block):
        """Current value for instance"""
        if instance is None:
            return self
        else:
            return instance.__dict__.get(self._ikey, self._value)

    def __set__(self, instance, value):
        """Set value for instance"""
        instance.__dict__[self._ikey] = self._set_value(value)

    def _set_value(self, value):
        """Convert value-to-be-set to stored value"""
        return value

    def __delete__(self, instance):
        """Delete value in instance (effectively resets to default)"""
        if self._ikey in instance.__dict__:
            del instance.__dict__[self._ikey]

    def _str_value(self, value):
        """Convert value to human-readable representation"""
        return str(value)

    def str(self, instance):
        """Get human-readable value in instance"""
        return '%s: %s' % (
            self._name,
            self._str_value(self.__get__(instance, type(instance)))
        )

    def read(self, buf, off, instance):
        """Read value of this field for Block instance from buffer buf.
        where the block starts at byte off and has network byte order.
        """
        raise NotImplementedError()

    def write(self, buf, off, instance):
        """Write value of this field for Block instance into buffer buf.
        where the block starts at byte off and has network byte order.
        The buffer must be pre-filled with NULs to the correct size.
        """
        raise NotImplementedError()

    @property
    def offset(self):
        """The big-endian bit offset within owners block"""
        return self._offset

    @property
    def size(self):
        """The number of bits occupied"""
        return self._size

    @property
    def name(self):
        """The unique name for its block, and possibly higher.
        By default, it is set to its identifier in the block."""
        return self._name

    @name.setter
    def name(self, name):
        self._name = name
        self._ikey = None if not name else '_bits_%s' % name

    def __lt__(self, other):
        return self._name < other.name


class Flag(Bits):

    _value = False
    _size = 1

    def __init__(self, **kwargs):
        if 'value' not in kwargs:
            kwargs['value'] = False
        super(Flag, self).__init__(**kwargs)

    def _set_value(self, value):
        return bool(value)

    def str(self, instance):
        if instance.__dict__.get(self._ikey, self._value):
            return self._name
        return ''

    def read(self, buf, off, instance):
        off += self._offset // 8
        bit = buf[off] & (1 << (7 - self._offset % 8))
        instance.__dict__[self._ikey] = self._set_value(bit)

    def write(self, buf, off, instance):
        off += self._offset // 8
        bit = 1 << (7 - self._offset % 8)
        if instance.__dict__.get(self._ikey, self._value):
            buf[off] |= bit
        else:
            # do not write a zero value
            # buf[off] &= ~bit
            pass


class Int(Bits):

    _value = 0

    def __init__(self, **kwargs):
        super(Int, self).__init__(**kwargs)
        self._sup = 1 << self._size

    def _set_value(self, value):
        value = int(value)
        if value < 0:
            raise ValueError("Value %d is negative for field %s" % (value, self._name))
        if value >= self._sup:
            raise ValueError("Value %d too large for field %s of bitlength %d" % (value, self._name, self._size))
        return value

    def read(self, buf, off, instance):
        off += self._offset // 8
        startbit = self._offset % 8
        # how many bits do we occupy
        span = startbit + self._size
        value = 0
        while span > 0:
            value <<= 8
            value |= buf[off]
            span -= 8
            off += 1
        if span:  # kill bits in last byte after us
            value >>= -span
        if startbit:  # kill bits before startbit
            value %= 1 << self._size
        instance.__dict__[self._ikey] = value

    def write(self, buf, off, instance):
        off += self._offset // 8
        startbit = self._offset % 8
        # how many bits do we occupy
        span = startbit + self._size

        value = instance.__dict__.get(self._ikey, self._value)
        if value == 0:
            return
        if value < 0:
            _LOGGER.error("Value %d is negative for field %s" % (value, self._name))
            value = 0
        if value >= self._sup:
            _LOGGER.error("Value %d too large for field %s of bitlength %d" % (value, self._name, self._size))
            value %= self._sup

        # copy bits from first byte
        if startbit:
            byte = buf[off] >> (8 - startbit)
            byte <<= self._size
            value |= byte

        # move to last byte
        off += span // 8
        rest = span % 8
        if not rest:
            off -= 1

        # copy bits from last byte
        if rest:
            rest = 8 - rest
            value <<= rest
            byte = buf[off]
            # kill our bits
            byte %= 1 << rest
            value |= byte

        # rewrite buffer
        while span > 0:
            buf[off] = value & 0xFF
            value >>= 8
            span -= 8
            off -= 1


class Float(Bits):

    _value = 0.0
    _size = 32

    def __init__(self, **kwargs):
        if kwargs.get('offset') % 8:
            raise ValueError('Floats must be byte-aligned')
        super(Float, self).__init__(**kwargs)

    def _set_value(self, value):
        return float(value)

    def _str_value(self, value):
        return "%0.2f" % value

    def read(self, buf, off, instance):
        off += self._offset // 8
        octets = bytes(buf[off:off+4])
        value = struct.unpack(">f", octets)
        instance.__dict__[self._ikey] = value

    def write(self, buf, off, instance):
        off += self._offset // 8
        value = instance.__dict__.get(self._ikey, self._value)
        if value == 0.0:
            return
        wbytes = struct.pack(">f", value)
        buf[off:off+4] = wbytes


def _int_to_bytes(value, length):
    """Convert an int to the byte array it really represents."""
    buf = bytearray(length)
    for off in range(length):
        buf[off] = value & 0xFF
        value >>= 8
    buf.reverse()
    return buf


class Ipv4(Int):
    """An Int block representing an IPv4 address."""
    _size = 32

    def _str_value(self, value):
        return to_hex(_int_to_bytes(value, self._size // 8))


class Ipv6(Int):
    """An Int block representing an IPv6 address."""
    _size = 128

    def _str_value(self, value):
        return to_hex(_int_to_bytes(value, self._size // 8))


class Unset(Flag):
    """Bits added to each block to mark reserved/unassigned bits"""

    def read(self, buf, off, instance):
        super(Unset, self).read(buf, off, instance)
        if instance.__dict__.get(self._ikey):
            _LOGGER.warning("Reading at <%s>[%s]: Bit %s is set" % (id(buf), off, self._offset))

    def write(self, buf, off, instance):
        super(Unset, self).write(buf, off, instance)
        if instance.__dict__.get(self._ikey):
            _LOGGER.warning("Writing at <%s>[%s]: Bit %s is set" % (id(buf), off, self._offset))


def padlen(length, base):
    """Return the lowest multiple of base not smaller than length"""
    rest = length % base
    if rest:
        length += base - rest
    return length


def padded(buf, base=4, fill=b'\0', length=None):
    """Return buf padded with fill to length or to a multiple of base"""
    original = len(buf)
    if length is None:
        length = padlen(original, base)
    length -= original
    assert length >= 0, (length, original, buf)
    if length:
        buf = buf + fill * length
    return bytes(buf)


def to_hex(octets):
    """Return a human-readable hexadecimal representation of octets."""
    as_hex = binascii.hexlify(bytes(octets))
    items = list()
    index = 0
    lenhex = len(as_hex)
    if lenhex == 0:
        return ''
    while index < lenhex:
        if index % 8:
            items.append(b' ')
        else:
            items.append(b'|')
        end = index + 2
        items.append(as_hex[index:end])
        index = end
    if lenhex % 8 == 0:
        items.append(b'|')
    return b''.join(items)


class SizeError(Exception):
    """Size Error Exception

    Exception thrown when a Block refuses to read a buffer due to unsatisfied
    size constraints. The calling function may then choose to replace this Block
    with an Unknown Block of a shared superclass."""
    pass


class _BlockMeta(type):
    """Metaclass for Block classes

    Adds the _size, _bits attributes, and all Unset bits (named _unset_<offset>).
    _size is the fixed size of the block, padded to _block_pad attribute (default 4).
    _bits is a list of all Bits attributes defined for this block (including Unset)
    """

    def __new__(mcs, name, bases, attrs):
        """Create new Block-based class"""

        # Collect [assigned] Bits
        assigned = set()
        bits = list()
        for attrname, attr in attrs.items():
            if isinstance(attr, Bits):
                assigned.update(
                    range(attr.offset, attr.offset + attr.size)
                )
                bits.append(attr)
                if not attr.name:
                    attr.name = attrname

        # Compute size
        end = max(assigned) + 1 if assigned else 0
        size = end // 8
        if end % 8:
            size += 1
        pad = attrs.get('_block_pad', None)
        if pad is None:
            for base in bases:
                pad = getattr(base, '_block_pad', None)
                if pad is not None:
                    break
            else:
                pad = 4
        size = padlen(size, pad)

        # Add unset bits
        for bit in range(size * 8):
            if bit not in assigned:
                unset = Unset(offset=bit, name='_unset_%d' % bit)
                bits.append(unset)
                attrs[unset.name] = unset

        bits.sort(key=lambda item: item.name)
        attrs['_size'] = size
        attrs['_bits'] = bits
        return super(_BlockMeta, mcs).__new__(mcs, name, bases, attrs)

# Kludge for python 2 and 3 syntax compatibility
_BlockBase = _BlockMeta('_BlockBase', (object, ), dict(
    # updated by metaclass
    _bits=None,
    _size=None))


class Block(_BlockBase):
    """A group of Bits occupying a continuous sequence of bytes.

    The current design does not allow a subclass of Block to have its own Bits
    if the parent class does.

    Blocks may be included inside parent blocks. Although not enforced by this
    library, Blocks should not be shared among their parents, to prevent any
    bugs or mix-ups.

    Blocks may be unions of same-sized related structures; to this end, a field
    with value equal to zero will not write this value to a buffer.
    Conversely, all alternate values are read and set separately - do not write
    a read block with a modified field without setting all others to zero.

    Each block supports the clone method (and init parameter) that clone all
    subblocks and copy all bits and related data.
    """

    def clone(self):
        """Create a new instance of this Block, initialized by this instance."""
        return type(self)(clone=self)

    def __init__(self, clone=None):
        super(Block, self).__init__()
        if clone is not None:
            cls = type(clone)
            for bits in self._bits:
                bits.__set__(self, bits.__get__(clone, cls))

    def _get_size(self):
        """Redefinable getter for the size property.
        Children may choose not to call any super implementations.
        """
        return self._size

    @property
    def size(self):
        """The current size of the block. It must be padded to this Block's
        padding and may be more than the padded size containing all its Bits.
        It must be the actual size that will be written when serializing.
        It is computed by _get_size(); this method should not refer to any Bits
        containing only the reported size of items the Block comprises,
        but must update them if necessary.
        """
        return self._get_size()

    def update(self, updates):
        """Update the fields contained by this block with a dict of values.
        The dict may contain ignored keys that do not belong to this block.
        Return number of fields that were updated.
        """
        updated = 0
        for bits in self._bits:
            if bits.name in updates:
                bits.__set__(self, updates[bits.name])
                updated += 1
        return updated

    def read(self, buf, off, max_end):
        """Read the block from buf, starting at off, until max_end.
        Return offset after this block - should equal and cannot exceed max_end.
        Raise SizeError if number of bytes offered is wrong.
        """
        end = off + self._size
        if end > max_end:
            _errmsg = ("Block[%s] cannot fit [%d:%d], it needs [%d:%d]" %
                       (self.__class__.__name__, off, max_end, off, end))
            raise SizeError(_errmsg)
        for bits in self._bits:
            bits.read(buf, off, self)
        return end

    def write(self, buf, off):
        """Write the block into buf, starting at off.
        Enough space pre-filled with NULs must be reserved for its size.
        Return offset after this block."""
        for bits in self._bits:
            bits.write(buf, off, self)
        return off + self._size

    def __str__(self):
        return self.show({})

    def show(self, format, prefix=''):
        unsets = list()
        values = list()
        self_type = type(self)
        for bits in self._bits:
            if isinstance(bits, Unset):
                if bits.__get__(self, self_type):
                    unsets.append(bits.offset)
            else:
                values.append(bits.str(self))
        if unsets:
            unsets.sort()
            values.append('UNSET: [%s]' % ' '.join([str(off) for off in unsets]))
        separator = format.get('data', ', ')
        prefix = prefix or ''
        output = ''
        for value in values:
            if value:
                output += prefix + value + separator
        return output
        # return prefix + separator.join(value for value in values if value)


class HeadBlock(Block):
    """Base class for Blocks that start with a fixed header

    The header is a Block of its own and its size counts to the respective
    HeadBlock's total size (as computed by the _get_size method).

    The header may contain the total or partial size of the object; subclasses
    should update this value in _get_size to ensure its correctness upon
    sending. To report a false value when writing, set the report_length
    property to a non-None value.

    Though a HeadBlock is read and written from its header's offset,
    all offsets of its own body start at 0.

    Each subclass is responsible for reading its header.

    A HeadBlock may also contain more than a header and its body; it is up
    to a subclass to read and write additional data and track its length.
    Header lengths need not be cloned. Reported lengths are never cloned.
    """

    # the Block class for the header object
    _header_class = None

    @classmethod
    def get_header(cls, clone=None):
        return cls._header_class(clone=clone)

    def __init__(self, clone=None):
        super(HeadBlock, self).__init__(clone=clone)
        self._header = self.get_header(
            clone=None if clone is None else clone.header)
        self._report_length = None

    def _get_size(self):
        return self._header.size + self._size

    @property
    def header(self):
        return self._header

    @property
    def report_length(self):
        """The reported length when writing the Block."""
        return self._report_length

    @report_length.setter
    def report_length(self, length):
        if not hasattr(self._header, 'length'):
            raise AttributeError('Header does not have length attribute')
        self._report_length = length

    def update(self, updates):
        updated = (super(HeadBlock, self).update(updates) + self._header.update(updates))
        if 'report_length' in updates:
            self.report_length = updates['report_length']
            updated += 1
        return updated

    def write(self, buf, off):
        length = None
        if self._report_length is not None:
            length = self._header.length
            _LOGGER.info('Block %s reporting length %s instead of %s' %
                         (self.__class__.__name__, self._report_length, length))
            self._header.length = self._report_length
        off = self._header.write(buf, off)
        if length is not None:
            self._header.length = length
        return super(HeadBlock, self).write(buf, off)

    def __str__(self):
        return self.show({})

    def show(self, format, prefix=''):
        if self.__class__.__bases__[0].__name__ == 'Object':
            separator = format.get('obj_sep', ' ')
        elif self.__class__.__bases__[0].__name__ == 'Tlv' or self.__class__.__bases__[0].__name__ == 'Rsvp':
            separator = format.get('item_sep', ' ')
        else:
            separator = ' '

        n_prefix = None if prefix is None else prefix + '  '
        prefix = prefix or ''
        output = prefix + '=Data=' + separator
        output += super(HeadBlock, self).show(format, n_prefix)
        return output


class Blob(object):
    """Arbitrary data in place of any message Block."""

    def __init__(self, octets=b'', size=None):
        if size is not None:
            octets = b'\0' * size
        self.octets = octets

    @property
    def octets(self):
        """Data contained in the blob."""
        return self._octets

    @octets.setter
    def octets(self, octets):
        self._octets = octets

    @property
    def valid(self):
        """A blob is always invalid."""
        return False

    @property
    def size(self):
        """Size of blob in bytes"""
        return len(self.octets)

    def update(self, updates):
        if 'octets' in updates:
            self.octets = updates['octets']
            return 1
        return 0

    def read(self, buf, off, end):
        """Read blob from buffer and return end."""
        _LOGGER.error("Reading blob in <%s>[%s:%s]" % (id(buf), off, end))
        self.octets = buf[off:end]
        return end

    def write(self, buf, off):
        end = off + len(self.octets)
        _LOGGER.error("Writing blob to <%s>[%s:%s]" % (id(buf), off, end))
        buf[off:end] = self.octets
        return end

    def __str__(self):
        return 'Blob octets="%s"' % to_hex(self.octets)
