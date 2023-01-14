/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.object.bnc;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.parser.object.AbstractEROWithSubobjectsParser;
import org.opendaylight.protocol.pcep.spi.EROSubobjectRegistry;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.non.branch.node.object.NonBranchNodeList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.non.branch.node.object.NonBranchNodeListBuilder;

public final class NonBranchNodeListObjectParser extends AbstractEROWithSubobjectsParser {
    private static final int CLASS = 31;
    private static final int TYPE = 2;

    public NonBranchNodeListObjectParser(final EROSubobjectRegistry subobjReg) {
        super(subobjReg, CLASS, TYPE);
    }

    @Override
    public NonBranchNodeList parseObject(final ObjectHeader header, final ByteBuf bytes)
            throws PCEPDeserializerException {
        checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        return new NonBranchNodeListBuilder()
            .setIgnore(header.getIgnore())
            .setProcessingRule(header.getProcessingRule())
            .setSubobject(BNCUtil.toBncSubobject(parseSubobjects(bytes)))
            .build();
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        checkArgument(object instanceof NonBranchNodeList,
            "Wrong instance of PCEPObject. Passed %s. Needed NonBranchNodeList.", object.getClass());
        final NonBranchNodeList nbnc = (NonBranchNodeList) object;
        final ByteBuf body = Unpooled.buffer();
        serializeSubobject(BNCUtil.toIroSubject(nbnc.getSubobject()), body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.getProcessingRule(), object.getIgnore(), body, buffer);
    }
}
