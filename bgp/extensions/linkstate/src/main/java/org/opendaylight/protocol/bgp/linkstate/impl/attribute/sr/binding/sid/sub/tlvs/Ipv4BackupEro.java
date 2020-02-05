/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.linkstate.spi.BindingSubTlvsParser;
import org.opendaylight.protocol.bgp.linkstate.spi.BindingSubTlvsSerializer;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.BindingSubTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.Ipv4EroBackupCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.ipv4.ero.backup._case.Ipv4EroBackup;

public final class Ipv4BackupEro implements BindingSubTlvsParser, BindingSubTlvsSerializer {
    private static final int BACKUP_ERO_IPV4 = 1166;

    @Override
    public BindingSubTlv parseSubTlv(final ByteBuf slice, final ProtocolId protocolId) {
        return Ipv4EroParser.parseIpv4EroBackupCase(slice);
    }

    @Override
    public int getType() {
        return BACKUP_ERO_IPV4;
    }

    @Override
    public void serializeSubTlv(final BindingSubTlv bindingSubTlv, final ByteBuf aggregator) {
        checkArgument(bindingSubTlv instanceof Ipv4EroBackupCase, "Wrong BindingSubTlv instance expected",
            bindingSubTlv);
        final Ipv4EroBackup ipv4Backup = ((Ipv4EroBackupCase) bindingSubTlv).getIpv4EroBackup();
        TlvUtil.writeTLV(getType(), Ipv4EroParser.serializeIpv4EroCase(ipv4Backup.isLoose(), ipv4Backup.getAddress()),
            aggregator);
    }
}
