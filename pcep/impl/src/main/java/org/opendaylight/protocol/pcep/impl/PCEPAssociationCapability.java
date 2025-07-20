/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.collect.ImmutableSet;
import java.net.InetSocketAddress;
import java.util.Arrays;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.config.rev250930.AssociationCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.open.object.open.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.AssociationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.association.type.list.tlv.AssociationTypeList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.association.type.list.tlv.AssociationTypeListBuilder;

public final class PCEPAssociationCapability extends PCEPCapability {
    private final AssociationTypeList associationList;

    public PCEPAssociationCapability() {
        this(true, true, true, true, true, true);
    }

    public PCEPAssociationCapability(final boolean pathProtection, final boolean disjoint, final boolean policy,
            final boolean singleSideLSP, final boolean doubleSideLSP, final boolean srPolicy) {
        associationList = new AssociationTypeListBuilder()
            .setAssociationType(constructAssociationList(pathProtection, disjoint, policy,
                    singleSideLSP, doubleSideLSP, srPolicy)).build();
    }

    public PCEPAssociationCapability(final AssociationCapabilities config) {
        this(config.getPathProtection(), config.getDisjointPath(), config.getPolicy(), config.getSingleSideLsp(),
                config.getDoubleSideLsp(), config.getSrPolicy());
    }

    @Override
    public void setCapabilityProposal(final InetSocketAddress address, final TlvsBuilder builder) {
        builder.setAssociationTypeList(associationList);
    }

    private static @NonNull ImmutableSet<AssociationType> constructAssociationList(final boolean pathProtection,
            final boolean disjoint, final boolean policy, final boolean singleSideLSP, final boolean doubleSideLSP,
            final boolean srPolicy) {
        final var list = ImmutableSet.<AssociationType>builder();
        if (pathProtection) {
            list.add(AssociationType.PathProtection);
        }
        if (disjoint) {
            list.add(AssociationType.Disjoint);
        }
        if (policy) {
            list.add(AssociationType.Policy);
        }
        if (singleSideLSP) {
            list.add(AssociationType.SingleSideLsp);
        }
        if (doubleSideLSP) {
            list.add(AssociationType.DoubleSideLsp);
        }
        if (srPolicy) {
            list.add(AssociationType.SrPolicy);
        }
        return list.build();
    }

    public AssociationTypeList getAssociationTypeList() {
        return associationList;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(associationList.getAssociationType().toArray());
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || obj instanceof PCEPAssociationCapability other
            && associationList.getAssociationType().equals(other.getAssociationTypeList().getAssociationType());
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        associationList.getAssociationType().forEach(type -> helper.addValue(type.getName()));
        return helper;
    }
}
