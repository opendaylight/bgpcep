/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.protocol.bgp.rib.impl.spi.Codecs;
import org.opendaylight.protocol.bgp.rib.impl.spi.CodecsRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContext;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

class RIBSupportContextImpl extends RIBSupportContext {
    private final RIBSupport<?, ?, ?, ?> ribSupport;
    private final Codecs codecs;

    RIBSupportContextImpl(final RIBSupport<?, ?, ?, ?> ribSupport, final CodecsRegistry codecs) {
        this.ribSupport = requireNonNull(ribSupport);
        this.codecs = codecs.getCodecs(this.ribSupport);
    }

    @Override
    public Collection<NodeIdentifierWithPredicates> writeRoutes(final DOMDataTreeWriteTransaction tx,
                                                                final YangInstanceIdentifier tableId,
                                                                final MpReachNlri nlri,
                                                                final Attributes attributes) {
        final ContainerNode domNlri = this.codecs.serializeReachNlri(nlri);
        final ContainerNode routeAttributes = this.codecs.serializeAttributes(attributes);
        return this.ribSupport.putRoutes(tx, tableId, domNlri, routeAttributes);
    }

    @Override
    public void createEmptyTableStructure(final DOMDataTreeWriteTransaction tx, final YangInstanceIdentifier tableId) {
        tx.put(LogicalDatastoreType.OPERATIONAL, tableId, this.ribSupport.emptyTable());
    }

    @Override
    public void deleteRoutes(final DOMDataTreeWriteTransaction tx, final YangInstanceIdentifier tableId,
            final MpUnreachNlri nlri) {
        this.ribSupport.deleteRoutes(tx, tableId, this.codecs.serializeUnreachNlri(nlri));
    }

    @Override
    @SuppressWarnings("unchecked")
    @SuppressFBWarnings("NM_CONFUSING")
    public <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>,
        R extends Route & ChildOf<? super S> & Identifiable<I>, I extends Identifier<R>>
            RIBSupport<C, S, R, I> getRibSupport() {
        return (RIBSupport<C, S, R, I>) this.ribSupport;
    }
}
