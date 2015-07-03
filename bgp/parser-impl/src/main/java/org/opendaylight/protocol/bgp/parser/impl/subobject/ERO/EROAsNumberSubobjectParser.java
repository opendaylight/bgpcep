/*
 *
 *  * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.protocol.bgp.parser.impl.subobject.ERO;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.impl.subobject.AsNumberCaseParser;
import org.opendaylight.protocol.bgp.parser.spi.EROSubobjectParser;
import org.opendaylight.protocol.bgp.parser.spi.EROSubobjectSerializer;
import org.opendaylight.protocol.bgp.parser.spi.EROSubobjectUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.AsNumberCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.explicit.route.subobjects.list.SubobjectContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.explicit.route.subobjects.list.SubobjectContainerBuilder;

/**
 * Parser for {@link AsNumberCase}
 */
public class EROAsNumberSubobjectParser implements EROSubobjectParser, EROSubobjectSerializer {

    public static final int TYPE = 32;

    @Override
    public SubobjectContainer parseSubobject(final ByteBuf buffer, final boolean loose) throws BGPParsingException {
        return new SubobjectContainerBuilder().setLoose(loose).setSubobjectType(AsNumberCaseParser.parseSubobject
            (buffer)).build();
    }

    @Override
    public void serializeSubobject(final SubobjectContainer subobject, final ByteBuf buffer) {
        Preconditions.checkArgument(subobject.getSubobjectType() instanceof AsNumberCase, "Unknown subobject instance. Passed %s. Needed AsNumberCase.", subobject.getSubobjectType().getClass());
        final ByteBuf body = AsNumberCaseParser.serializeSubobject((AsNumberCase) subobject.getSubobjectType());
        EROSubobjectUtil.formatSubobject(TYPE, subobject.isLoose(), body, buffer);
    }
}
