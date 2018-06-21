/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.linkstate.spi.BindingSubTlvsParser;
import org.opendaylight.protocol.bgp.linkstate.spi.BindingSubTlvsSerializer;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.BindingSubTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.Ipv6EroBackupCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.Ipv6EroBackupCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.Ipv6EroCase;

public final class Ipv6BackupEro implements BindingSubTlvsParser, BindingSubTlvsSerializer {
    private static final int BACKUP_ERO_IPV6 = 1167;

    @Override
    public BindingSubTlv parseSubTlv(final ByteBuf slice, final ProtocolId protocolId) {
        final Ipv6EroCase ipv6backup = Ipv6EroParser.parseIpv6EroCase(slice);
        return new Ipv6EroBackupCaseBuilder().setAddress(ipv6backup.getAddress()).setLoose(ipv6backup.isLoose()).build();
    }

    @Override
    public int getType() {
        return BACKUP_ERO_IPV6;
    }

    @Override
    public void serializeSubTlv(final BindingSubTlv bindingSubTlv, final ByteBuf aggregator) {
        Preconditions.checkArgument(bindingSubTlv instanceof Ipv6EroBackupCase, "Wrong BindingSubTlv instance expected", bindingSubTlv);
        final Ipv6EroBackupCase ipv6Backup = (Ipv6EroBackupCase) bindingSubTlv;
        TlvUtil.writeTLV(getType(), Ipv6EroParser.serializeIpv6EroCase(ipv6Backup.isLoose(), ipv6Backup.getAddress()), aggregator);
    }
}
