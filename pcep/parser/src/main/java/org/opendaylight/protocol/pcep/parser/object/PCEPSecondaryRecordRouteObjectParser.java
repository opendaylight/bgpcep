/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.object;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.stream.Collectors;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.RROSubobjectRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.reported.route.object.rro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.secondary.reported.route.object.Srro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.secondary.reported.route.object.SrroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.secondary.reported.route.object.srro.SubobjectBuilder;

public final class PCEPSecondaryRecordRouteObjectParser extends AbstractRROWithSubobjectsParser {
    private static final int CLASS = 30;
    private static final int TYPE = 1;
    private static String EMPTY_ARRAY_ERROR = "Array of bytes is mandatory. Can't be null or empty.";

    public PCEPSecondaryRecordRouteObjectParser(final RROSubobjectRegistry subobjReg) {
        super(subobjReg, CLASS, TYPE);
    }

    @Override
    public Srro parseObject(final ObjectHeader header, final ByteBuf buffer) throws PCEPDeserializerException {
        checkArgument(buffer != null && buffer.isReadable(), EMPTY_ARRAY_ERROR);

        final SrroBuilder builder = new SrroBuilder();
        builder.setIgnore(header.getIgnore());
        builder.setProcessingRule(header.getProcessingRule());
        final List<Subobject> subObjects = parseSubobjects(buffer);

        builder.setSubobject(subObjects.stream()
            .map(so -> new SubobjectBuilder()
                    .setSubobjectType(so.getSubobjectType())
                    .setProtectionAvailable(so.getProtectionAvailable())
                    .setProtectionInUse(so.getProtectionInUse())
                    .build())
            .collect(Collectors.toList()));

        return builder.build();
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        checkArgument(object instanceof Srro,
            "Wrong instance of PCEPObject. Passed %s. Needed EroObject.", object.getClass());
        final Srro sero = (Srro) object;
        final ByteBuf body = Unpooled.buffer();
        final List<Subobject> subObjects = sero.nonnullSubobject().stream()
            .map(so -> new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930
                .reported.route.object.rro.SubobjectBuilder()
                    .setSubobjectType(so.getSubobjectType())
                    .setProtectionAvailable(so.getProtectionAvailable())
                    .setProtectionInUse(so.getProtectionInUse())
                    .build())
            .collect(Collectors.toList());
        serializeSubobject(subObjects, body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.getProcessingRule(), object.getIgnore(), body, buffer);
    }
}
