/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.impl;

import io.netty.buffer.ByteBuf;

import org.opendaylight.protocol.pcep.impl.TestVendorInformationTlvParser.TestEnterpriseSpecificInformation;
import org.opendaylight.protocol.pcep.parser.object.AbstractVendorInformationObjectParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.vendor.information.EnterpriseSpecificInformation;

public class TestVendorInformationObjectParser extends AbstractVendorInformationObjectParser {

    private static final EnterpriseNumber TEST_ENTERPRISE_NUMBER = new EnterpriseNumber(0L);

    public TestVendorInformationObjectParser() {
        super(0, 0);
    }

    @Override
    public void serializeEnterpriseSpecificInformation(
        final EnterpriseSpecificInformation enterpriseSpecificInformation, final ByteBuf buffer) {
        buffer.writeInt(((TestEnterpriseSpecificInformation) enterpriseSpecificInformation).getValue());
    }

    @Override
    public EnterpriseSpecificInformation parseEnterpriseSpecificInformation(final ByteBuf buffer) {
        return new TestEnterpriseSpecificInformation(buffer.readInt());
    }

    @Override
    public EnterpriseNumber getEnterpriseNumber() {
        return TEST_ENTERPRISE_NUMBER;
    }
}
