/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.ObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.SubobjectRegistry;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PathKeySubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.subobject.subobject.type.PathKeyCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.subobject.subobject.type.path.key._case.PathKeyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.AsNumberSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.CSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.IpPrefixSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.LabelSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.SrlgSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.UnnumberedSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.SubobjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.AsNumberCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.LabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.SrlgCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.UnnumberedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.as.number._case.AsNumberBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.label._case.LabelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.srlg._case.Srlg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.unnumbered._case.Unnumbered;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractEROWithSubobjectsParser implements ObjectParser, ObjectSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractEROWithSubobjectsParser.class);

    private static final int HEADER_LENGTH = 2;

    private final SubobjectRegistry subobjReg;

    protected AbstractEROWithSubobjectsParser(final SubobjectRegistry subobjReg) {
        this.subobjReg = Preconditions.checkNotNull(subobjReg);
    }

    protected List<Subobject> parseSubobjects(final ByteBuf buffer) throws PCEPDeserializerException {
        // Explicit approval of empty ERO
        Preconditions.checkArgument(buffer != null, "Array of bytes is mandatory. Can't be null.");
        final List<Subobject> subs = new ArrayList<>();
        while (buffer.isReadable()) {
            final boolean loose = ((buffer.getUnsignedByte(buffer.readerIndex()) & (1 << Values.FIRST_BIT_OFFSET)) != 0) ? true : false;
            final int type = (buffer.readUnsignedByte() & Values.BYTE_MAX_VALUE_BYTES) & ~(1 << Values.FIRST_BIT_OFFSET);
            final int length = buffer.readUnsignedByte() - HEADER_LENGTH;
            if (length > buffer.readableBytes()) {
                throw new PCEPDeserializerException("Wrong length specified. Passed: " + length + "; Expected: <= "
                        + buffer.readableBytes());
            }
            LOG.debug("Attempt to parse subobject from bytes: {}", ByteBufUtil.hexDump(buffer));
            final CSubobject sub = this.subobjReg.parseSubobject(type, buffer.readSlice(length));
            if (sub == null) {
                LOG.warn("Unknown subobject type: {}. Ignoring subobject.", type);
            } else {
                LOG.debug("Subobject was parsed. {}", sub);
                final SubobjectBuilder builder = new SubobjectBuilder();
                builder.setLoose(loose);
                builder.setSubobjectType(specify(sub));
                subs.add(builder.build());
            }
        }
        return subs;
    }

    private SubobjectType specify(final CSubobject sub) {
        if (sub instanceof AsNumberSubobject) {
            return new AsNumberCaseBuilder().setAsNumber(new AsNumberBuilder((AsNumberSubobject)sub).build()).build();
        } else if (sub instanceof IpPrefixSubobject) {
            return new IpPrefixCaseBuilder().setIpPrefix(new IpPrefixBuilder((IpPrefixSubobject)sub).build()).build();
        } else if (sub instanceof LabelSubobject) {
            return new LabelCaseBuilder().setLabel(new LabelBuilder((LabelSubobject) sub).build()).build();
        } else if (sub instanceof PathKeySubobject) {
            return new PathKeyCaseBuilder().setPathKey(new PathKeyBuilder((PathKeySubobject) sub).build()).build();
        } else if (sub instanceof SrlgSubobject) {
            return new SrlgCaseBuilder().setSrlg((Srlg) sub).build();
        } else if (sub instanceof UnnumberedSubobject) {
            return new UnnumberedCaseBuilder().setUnnumbered((Unnumbered) sub).build();
        }
        return null;
    }

    protected final void serializeSubobject(final List<Subobject> subobjects, final ByteBuf buffer) {
        if(subobjects != null) {
            for (final Subobject subobject : subobjects) {
                this.subobjReg.serializeSubobject((CSubobject) subobject, buffer);
            }
        }
    }
}
