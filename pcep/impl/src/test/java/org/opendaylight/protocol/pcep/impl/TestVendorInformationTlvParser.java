/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.parser.tlv.AbstractVendorInformationTlvParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.VendorInformation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.vendor.information.EnterpriseSpecificInformation;
import org.opendaylight.yangtools.binding.CaseObject;
import org.opendaylight.yangtools.binding.lib.AbstractAugmentable;
import org.opendaylight.yangtools.binding.lib.CodeHelpers;
import org.opendaylight.yangtools.yang.common.Uint32;

public class TestVendorInformationTlvParser extends AbstractVendorInformationTlvParser {
    private static final EnterpriseNumber TEST_ENTERPRISE_NUMBER = new EnterpriseNumber(Uint32.ZERO);

    @Override
    public void serializeEnterpriseSpecificInformation(
            final EnterpriseSpecificInformation enterpriseSpecificInformation, final ByteBuf buffer) {
        if (enterpriseSpecificInformation instanceof TestEnterpriseSpecificInformation) {
            buffer.writeInt(((TestEnterpriseSpecificInformation) enterpriseSpecificInformation).getValue());
        }
    }

    @Override
    public EnterpriseSpecificInformation parseEnterpriseSpecificInformation(final ByteBuf buffer) {
        return new TestEnterpriseSpecificInformation(buffer.readInt());
    }

    @Override
    public EnterpriseNumber getEnterpriseNumber() {
        return TEST_ENTERPRISE_NUMBER;
    }

    protected static final class TestEnterpriseSpecificInformation
            extends AbstractAugmentable<TestEnterpriseSpecificInformation>
            implements CaseObject<VendorInformation, EnterpriseSpecificInformation, TestEnterpriseSpecificInformation>,
                       EnterpriseSpecificInformation {
        private final int value;

        public TestEnterpriseSpecificInformation(final int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        @Override
        public Class<TestEnterpriseSpecificInformation> implementedInterface() {
            return TestEnterpriseSpecificInformation.class;
        }

        @Override
        public int javaHC() {
            return Integer.hashCode(value);
        }

        @Override
        public boolean javaEQ(final TestEnterpriseSpecificInformation obj) {
            return value == obj.value;
        }

        @Override
        public String javaTS() {
            return CodeHelpers.jcTS1(this, "value", value);
        }
    }
}
