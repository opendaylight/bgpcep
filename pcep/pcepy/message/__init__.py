# PCEP message elements

# Copyright (c) 2012,2013 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html


# In general, only the base RFC 5440 PCEP and draft-ietf-pce-stateful-00 aka
# D STATEFUL are supported by this library.

# Also supported are the RouteObject Rsvp subobject types 1, 2, 4 and 32,
# as defined by RFC 3209 RSVP-TE LSP and RFC 3477 RSVP-TE UNNUMBERED.

# Some (most or almost none) message element definitions have been implemented
# from RFC 5521 XRO, RFC 4874 RSVP-TE XRO, RFC 5441 BRPC, RFC 5520 PK,
# RFC 5541 OF, RFC 5557 CGO, RFC 5886 MON and RFC 6006 P2MP.
# These elements are not used by any other components and should be ignored.

from . import data
from . import base
from . import tlv  # noqa
from . import rsvp  # noqa
from . import object  # noqa
from ._message import *  # noqa

SizeError = data.SizeError
Blob = data.Blob
Message = base.Message
Transmission = base.Transmission
Object = base.Object
Tlv = base.Tlv
Rsvp = base.Rsvp
