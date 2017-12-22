/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.impl.app;

import static org.opendaylight.protocol.bmp.impl.app.TablesUtil.BMP_ATTRIBUTES_QNAME;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTree;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv4.prefixes.DestinationIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv4.prefixes.destination.ipv4.Ipv4Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv4.prefixes.destination.ipv4.Ipv4PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.UpdateMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BmpRibInWriter {

    private static final Logger LOG = LoggerFactory.getLogger(BmpRibInWriter.class);

    private static final LeafNode<Boolean> ATTRIBUTES_UPTODATE_FALSE =
            ImmutableNodes.leafNode(QName.create(BMP_ATTRIBUTES_QNAME, "uptodate"), Boolean.FALSE);
    private static final LeafNode<Boolean> ATTRIBUTES_UPTODATE_TRUE =
            ImmutableNodes.leafNode(ATTRIBUTES_UPTODATE_FALSE.getNodeType(), Boolean.TRUE);

    private final DOMTransactionChain chain;
    private final Map<TablesKey, TableContext> tables;


    private BmpRibInWriter(final YangInstanceIdentifier tablesRoot, final DOMTransactionChain chain,
            final RIBExtensionConsumerContext ribExtensions,
            final Set<TablesKey> tableTypes,  final BindingCodecTree tree) {
        this.chain = chain;
        final DOMDataWriteTransaction tx = this.chain.newWriteOnlyTransaction();
        this.tables = createTableInstance(tableTypes, tablesRoot, tx, ribExtensions, tree).build();

        LOG.debug("New RIB table {} structure installed.", tablesRoot.toString());
        tx.submit();
    }

    public static BmpRibInWriter create(@Nonnull final YangInstanceIdentifier tablesRootPath,
            @Nonnull final DOMTransactionChain chain,
            @Nonnull final RIBExtensionConsumerContext extensions, @Nonnull final Set<TablesKey> tableTypes,
            @Nonnull  final BindingCodecTree tree) {
        return new BmpRibInWriter(tablesRootPath, chain, extensions, tableTypes, tree);
    }

    /**
     * Write on DS Adj-RIBs-In.
     */
    public void onMessage(final UpdateMessage message) {

        if (!checkEndOfRib(message)) {
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path
                    .attributes.Attributes attrs = message.getAttributes();
            MpReachNlri mpReach = null;
            if (message.getNlri() != null) {
                mpReach = prefixesToMpReach(message);
            } else if (attrs != null && attrs.getAugmentation(Attributes1.class) != null) {
                mpReach = attrs.getAugmentation(Attributes1.class).getMpReachNlri();
            }
            if (mpReach != null) {
                addRoutes(mpReach, attrs);
                return;
            }

            MpUnreachNlri mpUnreach = null;
            if (message.getWithdrawnRoutes() != null) {
                mpUnreach = prefixesToMpUnreach(message);
            } else if (attrs != null && attrs.getAugmentation(Attributes2.class) != null) {
                mpUnreach = attrs.getAugmentation(Attributes2.class).getMpUnreachNlri();
            }
            if (mpUnreach != null) {
                removeRoutes(mpUnreach);
            }
        }
    }

    /**
     * Create new table instance.
     */
    private static ImmutableMap.Builder<TablesKey, TableContext> createTableInstance(final Set<TablesKey> tableTypes,
            final YangInstanceIdentifier yangTableRootIId, final DOMDataWriteTransaction tx,
            final RIBExtensionConsumerContext ribExtensions, final BindingCodecTree tree) {

        final ImmutableMap.Builder<TablesKey, TableContext> tb = ImmutableMap.builder();
        for (final TablesKey k : tableTypes) {
            final RIBSupport rs = ribExtensions.getRIBSupport(k);
            if (rs == null) {
                LOG.warn("No support for table type {}, skipping it", k);
                continue;
            }
            final InstanceIdentifierBuilder idb = YangInstanceIdentifier.builder(yangTableRootIId);
            final NodeIdentifierWithPredicates key = TablesUtil.toYangTablesKey(k);
            idb.nodeWithKey(key.getNodeType(), key.getKeyValues());
            final TableContext ctx = new TableContext(rs, idb.build(), tree);
            ctx.createTable(tx);

            tx.put(LogicalDatastoreType.OPERATIONAL, ctx.getTableId().node(BMP_ATTRIBUTES_QNAME)
                    .node(ATTRIBUTES_UPTODATE_FALSE.getNodeType()), ATTRIBUTES_UPTODATE_FALSE);
            LOG.debug("Created table instance {}", ctx.getTableId());
            tb.put(k, ctx);
        }
        return tb;
    }

    private synchronized void addRoutes(final MpReachNlri nlri, final org.opendaylight.yang.gen.v1.urn.opendaylight
            .params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes attributes) {
        final TablesKey key = new TablesKey(nlri.getAfi(), nlri.getSafi());
        final TableContext ctx = this.tables.get(key);

        if (ctx == null) {
            LOG.debug("No table for {}, not accepting NLRI {}", key, nlri);
            return;
        }

        final DOMDataWriteTransaction tx = this.chain.newWriteOnlyTransaction();
        ctx.writeRoutes(tx, nlri, attributes);
        LOG.trace("Write routes {}", nlri);
        tx.submit();
    }

    private synchronized void removeRoutes(final MpUnreachNlri nlri) {
        final TablesKey key = new TablesKey(nlri.getAfi(), nlri.getSafi());
        final TableContext ctx = this.tables.get(key);

        if (ctx == null) {
            LOG.debug("No table for {}, not accepting NLRI {}", key, nlri);
            return;
        }
        LOG.trace("Removing routes {}", nlri);
        final DOMDataWriteTransaction tx = this.chain.newWriteOnlyTransaction();
        ctx.removeRoutes(tx, nlri);
        tx.submit();
    }

    /**
     * Creates MPReach for the prefixes to be handled in the same way as linkstate routes.
     *
     * @param message Update message containing prefixes in NLRI
     * @return MpReachNlri with prefixes from the nlri field
     */
    private static MpReachNlri prefixesToMpReach(final UpdateMessage message) {
        final List<Ipv4Prefixes> prefixes = message.getNlri().stream()
                .map(n -> new Ipv4PrefixesBuilder().setPrefix(n.getPrefix()).setPathId(n.getPathId()).build())
                .collect(Collectors.toList());
        final MpReachNlriBuilder b = new MpReachNlriBuilder().setAfi(Ipv4AddressFamily.class).setSafi(
            UnicastSubsequentAddressFamily.class).setAdvertizedRoutes(
            new AdvertizedRoutesBuilder().setDestinationType(
                new DestinationIpv4CaseBuilder().setDestinationIpv4(
                    new DestinationIpv4Builder().setIpv4Prefixes(prefixes).build()).build()).build());
        if (message.getAttributes() != null) {
            b.setCNextHop(message.getAttributes().getCNextHop());
        }
        return b.build();
    }

    /**
     * Create MPUnreach for the prefixes to be handled in the same way as linkstate routes.
     *
     * @param message Update message containing withdrawn routes
     * @return MpUnreachNlri with prefixes from the withdrawn routes field
     */
    private static MpUnreachNlri prefixesToMpUnreach(final UpdateMessage message) {
        final List<Ipv4Prefixes> prefixes = new ArrayList<>();
        message.getWithdrawnRoutes().forEach(
            w -> prefixes.add(new Ipv4PrefixesBuilder().setPrefix(w.getPrefix()).setPathId(w.getPathId()).build()));
        return new MpUnreachNlriBuilder().setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class)
                .setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.update
                                .attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                                .DestinationIpv4CaseBuilder().setDestinationIpv4(new DestinationIpv4Builder()
                                .setIpv4Prefixes(prefixes).build()).build()).build()).build();
    }

    /**
     * For each received Update message, the upd sync variable needs to be updated to true, for particular AFI/SAFI
     * combination. Currently we only assume Unicast SAFI. From the Update message we have to extract the AFI. Each
     * Update message can contain BGP Object with one type of AFI. If the object is BGP Link, BGP Node or a BGPPrefix
     * the AFI is Linkstate. In case of BGPRoute, the AFI depends on the IP Address of the prefix.
     *
     * @param msg received Update message
     */
    private boolean checkEndOfRib(final UpdateMessage msg) {
        TablesKey type = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
        boolean isEOR = false;
        if (msg.getNlri() == null && msg.getWithdrawnRoutes() == null) {
            if (msg.getAttributes() != null) {
                if (msg.getAttributes().getAugmentation(Attributes1.class) != null) {
                    final Attributes1 pa = msg.getAttributes().getAugmentation(Attributes1.class);
                    if (pa.getMpReachNlri() != null) {
                        type = new TablesKey(pa.getMpReachNlri().getAfi(), pa.getMpReachNlri().getSafi());
                    }
                } else if (msg.getAttributes().getAugmentation(Attributes2.class) != null) {
                    final Attributes2 pa = msg.getAttributes().getAugmentation(Attributes2.class);
                    if (pa.getMpUnreachNlri() != null) {
                        type = new TablesKey(pa.getMpUnreachNlri().getAfi(), pa.getMpUnreachNlri().getSafi());
                    }
                    if (pa.getMpUnreachNlri().getWithdrawnRoutes() == null) {
                        // EOR message contains only MPUnreach attribute and no NLRI
                        isEOR = true;
                    }
                }
            } else {
                // true for empty Update Message
                isEOR = true;
            }
        }

        if (isEOR) {
            markTableUptodated(type);
            LOG.debug("BMP Synchronization finished for table {} ", type);
        }

        return isEOR;
    }

    private synchronized void markTableUptodated(final TablesKey tableTypes) {
        final DOMDataWriteTransaction tx = this.chain.newWriteOnlyTransaction();
        final TableContext ctxPre = this.tables.get(tableTypes);
        tx.merge(LogicalDatastoreType.OPERATIONAL, ctxPre.getTableId().node(BMP_ATTRIBUTES_QNAME)
                        .node(ATTRIBUTES_UPTODATE_TRUE.getNodeType()),
                ATTRIBUTES_UPTODATE_TRUE);
        tx.submit();
    }
}
