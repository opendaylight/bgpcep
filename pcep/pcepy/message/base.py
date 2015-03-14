# Base classes for Messages, Objects, Rsvp subobjects and Tlvs

# Copyright (c) 2012,2013 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import time
import weakref

from . import code
from . import data

import logging
_LOGGER = logging.getLogger('pcepy.message')


class TlvHeader(data.Block):
    """Common TLV header (the TL part)"""
    type_id = data.Int(offset=0, size=16)
    length = data.Int(offset=16, size=16)

    def show(self, format, prefix=''):
        separator = format.get('item_sep', ' ')
        n_prefix = None if prefix is None else prefix + '  '
        prefix = prefix or ''
        output = prefix + '=Header=' + separator
        output += super(TlvHeader, self).show(format, n_prefix)
        return output


class Tlv(data.HeadBlock):
    """Base class for PCEP type-length-value triplets appendable to Objects"""
    _block_pad = 1  # For BlockMeta;
    type_id = 0     # Set in each child
    _header_class = TlvHeader
    _fixed = True   # size

    unknown_class = None  # class of unknown tlvs

    @classmethod
    def get_subclass(cls, type_id):
        for subcls in cls.__subclasses__():
            if subcls.type_id == type_id:
                return subcls
        _LOGGER.warning('No Tlv for type %d' % type_id)
        return cls.unknown_class

    @classmethod
    def size_from_length(cls, length):
        """Return total Tlv size from header length"""
        return TlvHeader._size + data.padlen(length, 4)

    def _get_size(self):
        return Tlv.size_from_length(self._size)

    def __init__(self, clone=None, **updates):
        super(Tlv, self).__init__(clone=clone)
        if clone is None:
            self._header.type_id = type(self).type_id
            self._header.length = self._size
        if updates:
            self.update(updates)

    def read(self, buf, off, end):
        _LOGGER.debug('Reading %s Tlv from <%s>[%s:%s]' % (self.__class__.__name__, id(buf), off, end))
        ono = self._header.read(buf, off, end)
        if self._fixed and self._header.length != self._size:
            raise data.SizeError('Length %s in Tlv header not matching size' % (self._header.length, self._size))
        super(Tlv, self).read(buf, ono, end)
        return off + Tlv.size_from_length(self._size)

    def write(self, buf, off):
        _LOGGER.debug('Writing %s Tlv to <%s>[%s:]' % (self.__class__.__name__, id(buf), off))
        super(Tlv, self).write(buf, off)
        return off + Tlv.size_from_length(self._size)

    def show(self, format, prefix=''):
        separator = format.get('item_sep', ' ')
        n_prefix = None if prefix is None else prefix + '  '
        prefix = prefix or ''
        output = prefix + '[' + self.__class__.__name__ + ' item' + ']' + \
            separator
        output += self.header.show(format, n_prefix) + separator
        output += super(Tlv, self).show(format, n_prefix) + separator
        return output

    def __str__(self):
        return self.show({})


class RsvpHeader(data.Block):
    """Unified Rsvp Header"""
    _block_pad = 2  # For BlockMeta

    # This flag is defined for ERO and XRO; it has different meaning in either
    # of them (it means 'mandatory' in XRO and is False when it is mandatory).
    loose = data.Flag(offset=0)
    type_id = data.Int(offset=1, size=7)
    length = data.Int(offset=8, size=8)

    def show(self, format, prefix=''):
        separator = format.get('item_sep', ' ')
        n_prefix = None if prefix is None else prefix + '  '
        prefix = prefix or ''
        output = prefix + '=Header=' + separator
        output += super(RsvpHeader, self).show(format, n_prefix)
        return output


class Rsvp(data.HeadBlock):
    """Base class for RSVP-TE subobjects of E/I/R/X route objects and PK."""
    _block_pad = 2  # For BlockMeta; 2 because of header - use Unset if necessary
    type_id = 0     # Set in each child
    _header_class = RsvpHeader
    _fixed = True   # size

    unknown_class = None  # class of unknown rsvp subobjects

    @classmethod
    def get_subclass(cls, type_id):
        for subcls in cls.__subclasses__():
            if subcls.type_id == type_id:
                return subcls
        _LOGGER.warning('No Rsvp for type %d' % type_id)
        return cls.unknown_class

    @classmethod
    def size_from_length(cls, length):
        """Return total Rsvp size from header length"""
        return length

    def __init__(self, clone=None, **updates):
        super(Rsvp, self).__init__(clone=clone)
        if clone is None:
            self._header.type_id = type(self).type_id
            self._header.length = self._get_size()
        if updates:
            self.update(updates)

    def read(self, buf, off, end):
        _LOGGER.debug('Reading %s Rsvp from <%s>[%s:%s]' % (self.__class__.__name__, id(buf), off, end))
        off = self._header.read(buf, off, end)
        if self._fixed and self._header.length != self._header.size + self._size:
            raise data.SizeError('Length %s in Rsvp header not matching size' % (self._header.length, self._size))
        return super(Rsvp, self).read(buf, off, end)

    def write(self, buf, off):
        _LOGGER.debug('Writing %s Rsvp to <%s>[%d:]' % (self.__class__.__name__, id(buf), off))
        return super(Rsvp, self).write(buf, off)

    def __str__(self):
        return self.show({})

    def show(self, format, prefix=''):
        separator = format.get('item_sep', ' ')
        n_prefix = None if prefix is None else prefix + '  '
        prefix = prefix or ''
        output = prefix + '[' + self.__class__.__name__ + ' item' + ']' + \
            separator
        output += self.header.show(format, n_prefix) + separator
        output += super(Rsvp, self).show(format, n_prefix) + separator
        return output


class ObjectHeader(data.Block):
    """Common object header"""
    class_id = data.Int(offset=0, size=8)
    type_id = data.Int(offset=8, size=4)
    process = data.Flag(offset=14)
    ignore = data.Flag(offset=15)
    length = data.Int(offset=16, size=16)

    def show(self, format, prefix=''):
        separator = format.get('obj_sep', ' ')
        n_prefix = None if prefix is None else prefix + '  '
        prefix = prefix or ''
        output = prefix + '=Header=' + separator
        output += super(ObjectHeader, self).show(format, n_prefix)
        return output


class Object(data.HeadBlock):
    """Base class for PCEP message objects.
    Its body may be trailed by a list of items (Tlvs or Rsvp subobjects)."""
    class_id = 0  # Set in each child
    type_id = 0   # Set in each child
    _header_class = ObjectHeader
    _item_class = Tlv
    _allow_items = True  # Disabled in Unknown, SVEC

    unknown_class = None  # class of unknown objects

    @classmethod
    def get_subclass(cls, class_id, type_id=1):
        for subcls in Object.__subclasses__() + RouteObject.__subclasses__():
            if subcls.class_id == class_id and subcls.type_id == type_id:
                return subcls
        _LOGGER.warning('No Object for class %d and type %d' % (class_id, type_id))
        return cls.unknown_class

    def __init__(self, *items, **updates):
        """Initialize the object, optionally with a list of items and a dict
        of fields to update itself and its header.
        There is an additional keyword argument, clone - initializing this
        Object with both its fields and cloned items.
        """
        if 'clone' in updates:
            clone = updates['clone']
            del updates['clone']
        else:
            clone = None
        super(Object, self).__init__(clone=clone)
        if clone is None:
            self._items = list()
            self._header.class_id = type(self).class_id
            self._header.type_id = type(self).type_id
            self._get_size()
        else:
            self._items = [item.clone() for item in clone.items]
        if updates:
            self.update(updates)
        self._items.extend(items)

    @property
    def items(self):
        return self._items

    def add(self, item):
        """Add item to object."""
        self._items.append(item)

    def get(self, cls, off=0, with_position=False):
        """Retrieve first item matching class [tuple] from offset or None.
        Also return position or another None if with_position."""
        olen = len(self._items)
        while off < olen:
            item = self._items[off]
            if isinstance(item, cls):
                return (item, off) if with_position else item
            off += 1
        return (None, None) if with_position else None

    def _get_size(self):
        """Provide value for size property. Also updates length in header."""
        size = (super(Object, self)._get_size() +
                sum(item.size for item in self._items))
        self._header.length = size
        return size

    def read(self, buf, off, end):
        """Read object from buffer at off until end.
        If items are allowed, may include Blobs as some of them;
        else may return end offset less than end.
        """
        _LOGGER.debug("Reading %s Object from <%s>[%s:%s]" % (self.__class__.__name__, id(buf), off, end))
        off = self._header.read(buf, off, end)
        if self._header.length - self._header.size != end - off:
            _LOGGER.error('Length %s in object header not matching [%s:%s]' % (self._header.length, off, end))
        ono = super(Object, self).read(buf, off, end)

        if not self._allow_items:
            return ono

        items = self._items
        icls = self._item_class
        header = icls.get_header()
        while ono < end:
            try:
                header.read(buf, ono, end)
            except data.SizeError as error:
                _LOGGER.error("Cannot read %s header: %s" % (icls.__name__, error))
                blob = data.Blob()
                ono = blob.read(buf, ono, end)
                items.append(blob)
                break

            # Definitely read until here in this pass
            max_end = ono + icls.size_from_length(header.length)
            if max_end > end:
                item = icls.unknown_class()
                max_end = end
            else:
                item = icls.get_subclass(type_id=header.type_id)()

            try:
                ono = item.read(buf, ono, max_end)
            except data.SizeError as error:
                _LOGGER.error("Cannot read %s: %s" % (icls.__name__, error))
                item = icls.unknown_class()
                ono = item.read(buf, ono, max_end)
            items.append(item)

            if ono > max_end:
                _LOGGER.error('Item read past its end: %s > %s' % (ono, max_end))
                ono = max_end
            if ono < max_end:
                blob = data.Blob()
                ono = blob.read(buf, ono, max_end)
                items.append(blob)

        return ono

    def write(self, buf, off):
        _LOGGER.debug('Writing %s Object to <%s>[%d:]' % (self.__class__.__name__, id(buf), off))
        off = super(Object, self).write(buf, off)
        if self._items:  # May be None (Svec)
            for item in self._items:
                off = item.write(buf, off)
        return off

    def show(self, format, prefix=''):
        # Get separator
        separator = format.get('obj_sep', ' ')
        # Prefix for nested show()
        n_prefix = None if prefix is None else prefix + '  '
        prefix = prefix or ''
        output = prefix + '[' + self.__class__.__name__ + ' object' + ']' + \
            separator
        output += self.header.show(format, n_prefix) + separator
        output += super(Object, self).show(format, n_prefix) + separator

        for item in self.items:
            output += item.show(format, n_prefix)

        # Items
        # output += prefix + 'Items:' + separator
        # if self._items:
        #     output += separator.join(item.show(format, prefix * 2) for item \
        #         in self._items)
        # else:
        #     output += prefix * 2 + 'No items' + separator

        return output

    def __str__(self):
        return self.show({})


class RouteObject(Object):
    """Base class for Routing Objects (Rsvp containers).
    Each route object may contain only a subset of all available Rsvp objects.
    Furthermore, some fields in the unified Rsvp objects are only meaningful
    in some RouteObjects and should not be used in any other.
    Some such fields may even overlap - only one alternative may be nonzero
    when writing such objects; when reading, all values will be filled.
    """
    _item_class = Rsvp


class Group(object):
    """Base class for implicit logical objects grouping a sequence of related
    items (objects and other groups) inside a Message.

    The permitted sequence is defined as a list of tuples (key, clsdef, sigil),
    where key is the unique name for the item, clsdef is a subgroup class or
    object class or tuple of object classes. Sigil is 1 for single,
    '?' for optional, '*' for list and finally '+' for mandatory list.

    The sequence does not support alternation: therefore, the sequence must
    be defined as a superset of the actual sequence, making all objects within
    alternatives optional; further constraints should be defined another way.

    Note that object manipulation via Group's methods does not affect
    the containing message until its pack() method is called.
    Conversely, manipulating the Message's objects list is not reflected until
    the next call to the message's grab().
    The matching generated by the grab() method is not cloned by any message.
    """

    # A sequence of triplets
    _sequence = list()

    def __init__(self, **pushes):
        """Initialize a Group, optionally with items to push"""
        self._matches = dict()  # name -> Object/List/Group
        self._valid = True
        self._present = False
        if pushes:
            self.push_all(pushes)

    @property
    def valid(self):
        """The sequence of objects was valid at last grab or pack."""
        return self._valid

    @property
    def present(self):
        """The group is actually present (contains objects or subgroups)."""
        return self._present

    def grab(self, objects, off):
        """Grab objects into group from offset.
        Return end position."""
        valid = True
        selfid = '%s <%s>' % (self.__class__.__name__, id(self))  # noqa
        present = False
        olen = len(objects)
        _badobject = (data.Blob, Object.unknown_class)
        self._matches.clear()

        for key, clsdef, sigil in self._sequence:
            # _LOGGER.debug('Grabbing %s (%s) into %s' % (key, sigil, selfid))
            val = None
            group = self._is_group(clsdef)
            is_list = self._is_list(sigil)
            if is_list:
                val = list()

            while off < olen:
                item = objects[off]
                if isinstance(item, _badobject):
                    # _LOGGER.warning('Bad element %s while reading %s in %s'
                    #     % (item.__class__.__name__, key, selfid)
                    # )
                    valid = False
                    off += 1
                    continue
                newval = None
                if group:
                    newval = clsdef()
                    off = newval.grab(objects, off)
                    if not newval.present:
                        newval = None
                    elif not newval.valid:
                        # _LOGGER.warning('Invalid %s while reading %s in %s'
                        #     % (clsdef, key, selfid)
                        # )
                        valid = False
                elif isinstance(item, clsdef):
                    # _LOGGER.debug('Grabbed %s as %s in %s'
                    #    % (item.__class__.__name__, key, selfid)
                    # )
                    newval = item
                    off += 1
                if newval is None:
                    break
                if is_list:
                    val.append(newval)
                else:
                    val = newval
                    break
            if (val if is_list else (val is not None)):
                self._matches[key] = val
                present = True
            elif not self._is_opt(sigil):
                # _LOGGER.warning('Missing %s in %s' % (key, selfid))
                valid = False

        self._valid = valid
        self._present = present
        return off

    def pack(self, objects):
        """Pack items into objects list. Return if sequence is valid."""
        valid = True
        for key, clsdef, sigil in self._sequence:
            val = self._matches.get(key)
            if val is None:
                if not self._is_opt(sigil):
                    valid = False
                continue
            if not self._is_list(sigil):
                val = [val]
            elif not val and not self._is_opt(sigil):
                valid = False
                continue
            if self._is_group(clsdef):
                for item in val:
                    if not item.pack(objects):
                        valid = False
            else:
                objects.extend(val)
        self._valid = valid
        return valid

    def have(self, key):
        """Return if key is currently in the group."""
        return key in self._matches

    def poll(self, key):
        """Get a grabbed item.
        If not present and list, create and return new empty list.
        If not present and group, create and return new non-present group.
        Raises KeyError for invalid key.
        """
        key, clsdef, sigil = self._get_defs(key)
        item = self._matches.get(key)
        if item is None:
            if self._is_list(sigil):
                item = list()
                self._matches[key] = item
            elif self._is_group(clsdef):
                item = clsdef()
                self._matches[key] = item
        return item

    def push(self, key, item):
        """Put an item for the key into the group.
        If key denotes a list, append. If item is a list also, extend.
        If item is None, remove [all if list].
        Raises KeyError for invalid key."""
        key, clsdef, sigil = self._get_defs(key)
        if item is None:
            if key in self._matches:
                del self._matches[key]
        elif self._is_list(sigil):
            items = self._matches.get(key)
            if items is None:
                items = list()
                self._matches[key] = items
            if self._isa_list(item):
                items.extend(item)
            else:
                items.append(item)
        else:
            self._matches[key] = item
        self._present = bool(self._matches)

    def push_all(self, pushes):
        """Push all items in the dict."""
        for key, item in pushes.items():
            self.push(key, item)

    def __str__(self):
        """Give a shallow view of objects in group."""
        if not self._matches:
            return '%s; Not Grabbed' % self.__class__.__name__
        items = list()
        for key, clsdef, sigil in self._sequence:
            is_list = self._is_list(sigil)
            is_group = self._is_group(clsdef)
            val = self._matches.get(key)
            if (not val if is_list else (val is None)):
                if self._is_opt(sigil):
                    val = 'none'
                else:
                    val = 'missing'
            else:
                if not is_list:
                    val = [val]
                val = ' + '.join([
                    str(v) if is_group else v.__class__.__name__
                    for v in val
                ])
            items.append('%s: %s' % (key, val))
        valid = '' if self._valid else ' (invalid)'
        return '%s%s{ %s }' % (self.__class__.__name__, valid, ', '.join(items))

    def _isa_group(self, item):
        return isinstance(item, Group)

    def _is_group(self, clsdef):
        return not self._is_alt(clsdef) and issubclass(clsdef, Group)

    def _is_alt(self, clsdef):
        return isinstance(clsdef, tuple)

    def _isa_list(self, item):
        return isinstance(item, (list, tuple))

    def _is_list(self, sigil):
        return sigil in ('+', '*')

    def _is_opt(self, sigil):
        return sigil in ('?', '*')

    def _get_defs(self, key):
        for defs in self._sequence:
            if defs[0] == key:
                return defs
        raise KeyError(key)


class Transmission(object):
    """Information on a transmission of a Message. It holds following values:
    session - session.PcepSession through which it passed
    time - unix time of transmission
    received - True if the message was received by our Peer, False if sent
    """

    def __init__(self, **kwargs):
        self._session = weakref.ref(kwargs['session'])
        self.time = kwargs.get('time') or time.time()
        self.received = kwargs.get('received', False)

    @property
    def session(self):
        return self._session()


class MessageHeader(data.Block):
    """Common message header"""
    version = data.Int(offset=0, size=3, value=code.PCEP_VERSION)
    type_id = data.Int(offset=8, size=8)
    length = data.Int(offset=16, size=16)

    def show(self, format, prefix=''):
        separator = format.get('msg_sep', ' ')
        n_prefix = None if prefix is None else prefix + '  '
        prefix = prefix or ''
        output = prefix + '=Header=' + separator
        output += super(MessageHeader, self).show(format, n_prefix)
        return output


class Message(Group):
    """Base class for PCEP messages.
    Contains most of a HeadBlock's functionality, but may not contain own Bits.

    Messages should not be transmitted more than once or share objects.
    """
    type_id = 0  # Set in each child

    def clone(self):
        """Create a new instance of this Message, initialized by this one."""
        return type(self)(clone=self)

    unknown_class = None  # class of unknown messages

    @classmethod
    def get_subclass(cls, type_id):
        for subcls in cls.__subclasses__():
            if subcls.type_id == type_id:
                return subcls
        _LOGGER.warning('No Message for type %d' % type_id)
        return cls.unknown_class

    @classmethod
    def get_message(cls, buf, type_id):
        """Read buffer from start to end, returning a Message for type_id.
        The message returned need not be valid; its elements may even be Blobs.
        """
        message = cls.get_subclass(type_id)()
        message.read(buf, 0, len(buf))
        return message

    def __init__(self, *objects, **pushes):
        """Initialize the message, optionally with a list of objects to contain
        or the group pushes (see Group).
        There are two additional keyword arguments: clone (a message to clone),
        header (a dict of values updating the header, including report_length).
        """
        if 'clone' in pushes:
            clone = pushes['clone']
            del pushes['clone']
        else:
            clone = None
        if 'header' in pushes:
            header = pushes['header']
            del pushes['header']
        else:
            header = None
        super(Message, self).__init__()
        self._transmission = None
        self._report_length = None
        if clone is None:
            self._header = MessageHeader()
            self._header.type_id = type(self).type_id
            self._objects = list()
        else:
            self._header = MessageHeader(clone=clone.header)
            self._objects = [obj.clone() for obj in clone.objects]

        if objects:
            if pushes:
                raise ValueError(
                    'Cannot mix objects and pushes - use header instead'
                )
            self._objects.extend(objects)
            self.grab()
        elif pushes:
            self.push_all(pushes)
            self.pack()
        if header:
            self.update(header)

    @property
    def header(self):
        """Message header"""
        return self._header

    @property
    def size(self):
        """Current message size in bytes"""
        return self._get_size()

    @property
    def objects(self):
        """Direct access to message's objects"""
        return self._objects

    @property
    def transmission(self):
        """Information on message's (last) transmission"""
        return self._transmission

    @transmission.setter
    def transmission(self, value):
        self._transmission = value

    @property
    def report_length(self):
        """The reported length when writing the Message."""
        return self._report_length

    @report_length.setter
    def report_length(self, length):
        self._report_length = length

    def update(self, updates):
        """Update header fields and report_length"""
        updated = self._header.update(updates)
        if 'report_length' in updates:
            self.report_length = updates['report_length']
            updated += 1
        return updated

    def add(self, obj):
        """Add object to message."""
        self._objects.append(obj)

    def get(self, cls, off=0, with_position=False):
        """Retrieve first object matching class [tuple] from offset or None.
        Also return position or another None if with_position."""
        olen = len(self._objects)
        while off < olen:
            obj = self._objects[off]
            if isinstance(obj, cls):
                return (obj, off) if with_position else obj
            off += 1
        return (None, None) if with_position else None

    def grab(self):
        """Grab objects from objects list into the named sequence.
        Return if valid, unlike parent class.
        """
        if super(Message, self).grab(self._objects, 0) != len(self._objects):
            self._valid = False
        return self._valid

    def pack(self):
        """Replace message objects with message's sequence objects(see Group).
        This is not called automatically, thus must be called after push()[es].
        """
        objects = list()
        super(Message, self).pack(objects)
        self._objects = objects
        return self._valid

    def _get_size(self):
        """Provide value for size property. Also updates length in header."""
        size = (self._header.size + sum(obj.size for obj in self._objects))
        self._header.length = size
        return size

    def read(self, buf, off, end):
        """Read message from buffer at offset, exactly until end.
        Then grab() groups. Set and return if valid overall.
        """
        valid = self.__class__ is not Message.unknown_class
        _LOGGER.debug("Reading %s Message from <%s>[%s:%s]" % (self.__class__.__name__, id(buf), off, end))
        ono = self._header.read(buf, off, end)
        if self._header.length != end - off:
            _LOGGER.error('Length %s in message header not matching [%s:%s]' % (self._header.length, off, end))
            valid = False

        objects = self._objects
        header = ObjectHeader()
        _baditem = (data.Blob, Tlv.unknown_class, Rsvp.unknown_class)
        while ono < end:
            try:
                header.read(buf, ono, end)
            except data.SizeError as error:
                _LOGGER.error("Cannot read object header: %s" % error)
                blob = data.Blob()
                ono = blob.read(buf, ono, end)
                objects.append(blob)
                valid = False
                break

            objcls = Object.get_subclass(
                class_id=header.class_id, type_id=header.type_id
            )
            if objcls is Object.unknown_class:
                valid = False

            # Definitely read until here in this pass
            max_end = ono + header.length
            if max_end > end:
                valid = False
                objcls = Object.unknown_class
                max_end = end

            obj = objcls()

            try:
                ono = obj.read(buf, ono, max_end)
                for item in obj.items:
                    if isinstance(item, _baditem):
                        valid = False
            except data.SizeError as error:
                _LOGGER.error("Cannot read object: %s" % error)
                valid = False
                obj = Object.unknown_class()
                ono = obj.read(buf, ono, max_end)
            objects.append(obj)

            if ono < max_end:
                blob = data.Blob()
                ono = blob.read(buf, ono, max_end)
                objects.append(blob)
                valid = False

        self._valid = valid and self.grab()
        return self._valid

    def write(self, buf, off):
        msgid = '%s%s Message <%s>' % (
            '' if self.grab() else 'invalid ',
            self.__class__.__name__,
            id(self)
        )
        _LOGGER.info('Writing %s to <%s>[%d:]' % (msgid, id(buf), off))

        length = None
        if self._report_length is not None:
            length = self._header.length
            _LOGGER.info('In %s: reporting length %s instead of %s' % (msgid, self._report_length, length))
            self._header.length = self._report_length
        off = self._header.write(buf, off)
        if length is not None:
            self._header.length = length

        for obj in self._objects:
            off = obj.write(buf, off)
        return off

    def __str__(self):
        return self.show({})

    def show(self, format, prefix=None):
        # Get separator (use default if not provided)
        separator = format.get('msg_sep', ' ')
        # n_prefix will be used for show of nested items
        n_prefix = None if prefix is None else prefix + '  '
        prefix = prefix or ''
        output = prefix + '[' + self.__class__.__name__ + ' message]' + separator
        output += self.header.show(format, n_prefix) + separator

        for obj in self.objects:
            output += obj.show(format, n_prefix)

        # output += separator + prefix + super(Message, self).__str__()

        return output
