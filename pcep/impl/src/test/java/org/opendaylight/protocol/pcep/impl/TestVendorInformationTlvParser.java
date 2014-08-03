/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.impl;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.impl.tlv.AbstractVendorInformationTlvParser;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vendor.information.EnterpriseSpecificInformation;
import org.opendaylight.yangtools.yang.binding.DataContainer;

public class TestVendorInformationTlvParser extends AbstractVendorInformationTlvParser {

    private static final long TEST_ENTERPRISE_NUMBER = 0;

    @Override
    public void serializeEnterpriseSpecificInformation(final EnterpriseSpecificInformation enterpriseSpecificInformation,
            final ByteBuf buffer) {
        if (enterpriseSpecificInformation instanceof TestEnterpriseSpecificInformation) {
            buffer.writeInt(((TestEnterpriseSpecificInformation) enterpriseSpecificInformation).getValue());
        }
    }

    @Override
    public EnterpriseSpecificInformation parseEnterpriseSpecificInformation(final ByteBuf buffer)
            throws PCEPDeserializerException {
        return new TestEnterpriseSpecificInformation(buffer.readInt());
    }

    @Override
    public long getEnterpriseNumber() {
        return TEST_ENTERPRISE_NUMBER;
    }

    protected static final class TestEnterpriseSpecificInformation implements EnterpriseSpecificInformation {

        private final int value;

        public TestEnterpriseSpecificInformation(final int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            return TestEnterpriseSpecificInformation.class;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + value;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            TestEnterpriseSpecificInformation other = (TestEnterpriseSpecificInformation) obj;
            if (value != other.value)
                return false;
            return true;
        }
    }

}
