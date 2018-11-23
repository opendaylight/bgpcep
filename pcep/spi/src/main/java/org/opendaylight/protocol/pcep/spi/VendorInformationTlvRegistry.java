/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.spi;

import io.netty.buffer.ByteBuf;

import java.util.Optional;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.vendor.information.tlvs.VendorInformationTlv;

public interface VendorInformationTlvRegistry {

    Optional<VendorInformationTlv> parseVendorInformationTlv(final EnterpriseNumber enterpriseNumber, final ByteBuf buffer) throws PCEPDeserializerException;

    void serializeVendorInformationTlv(final VendorInformationTlv viTlv, final ByteBuf buffer);

}
