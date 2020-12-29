/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.parser.object;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.stream.Collectors;
import org.opendaylight.protocol.pcep.spi.EROSubobjectRegistry;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.secondary.explicit.route.object.Sero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.secondary.explicit.route.object.SeroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.secondary.explicit.route.object.sero.SubobjectBuilder;

public final class PCEPSecondaryExplicitRouteObjecParser extends AbstractEROWithSubobjectsParser {
    private static final int CLASS = 29;
    private static final int TYPE = 1;
    private static String EMPTY_ARRAY_ERROR = "Array of bytes is mandatory. Can't be null or empty.";

    public PCEPSecondaryExplicitRouteObjecParser(final EROSubobjectRegistry subobjReg) {
        super(subobjReg, CLASS, TYPE);
    }

    @Override
    public Sero parseObject(final ObjectHeader header, final ByteBuf buffer) throws PCEPDeserializerException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), EMPTY_ARRAY_ERROR);

        final SeroBuilder builder = new SeroBuilder();
        builder.setIgnore(header.getIgnore());
        builder.setProcessingRule(header.getProcessingRule());
        final List<Subobject> subObjects = parseSubobjects(buffer);

        builder.setSubobject(subObjects.stream()
            .map(so -> new SubobjectBuilder().setLoose(so.getLoose()).setSubobjectType(so.getSubobjectType()).build())
            .collect(Collectors.toList()));

        return builder.build();
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof Sero,
            "Wrong instance of PCEPObject. Passed %s. Needed EroObject.", object.getClass());
        final Sero sero = (Sero) object;
        final ByteBuf body = Unpooled.buffer();
        final List<Subobject> subObjects = sero.nonnullSubobject().stream()
            .map(so -> new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109
                .explicit.route.object.ero.SubobjectBuilder()
                    .setLoose(so.getLoose())
                    .setSubobjectType(so.getSubobjectType())
                    .build())
            .collect(Collectors.toList());
        serializeSubobject(subObjects, body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.getProcessingRule(), object.getIgnore(), body, buffer);
    }
}
