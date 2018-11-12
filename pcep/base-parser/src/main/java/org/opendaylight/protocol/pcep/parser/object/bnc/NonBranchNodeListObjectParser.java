/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.object.bnc;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.parser.object.AbstractEROWithSubobjectsParser;
import org.opendaylight.protocol.pcep.spi.EROSubobjectRegistry;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
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
        Preconditions.checkArgument(bytes != null && bytes.isReadable(),
            "Array of bytes is mandatory. Can't be null or empty.");
        final NonBranchNodeListBuilder builder = new NonBranchNodeListBuilder();
        builder.setIgnore(header.isIgnore());
        builder.setProcessingRule(header.isProcessingRule());
        builder.setSubobject(BNCUtil.toBncSubobject(parseSubobjects(bytes)));
        return builder.build();
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof NonBranchNodeList,
            "Wrong instance of PCEPObject. Passed %s. Needed NonBranchNodeList.", object.getClass());
        final NonBranchNodeList nbnc = ((NonBranchNodeList) object);
        final ByteBuf body = Unpooled.buffer();
        serializeSubobject(BNCUtil.toIroSubject(nbnc.getSubobject()), body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }
}
