package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTestCustomizer;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeSchemaAwareBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableUnkeyedListNodeBuilder;

public class BUGDemo extends AbstractConcurrentDataBrokerTest {
    private static final QName DATA_QNAME = QName.create("urn:opendaylight:params:xml:ns:yang:bgp-inet", "2013-09-19", "attributes").intern();
    private static final QName ASPATH = QName.create("urn:opendaylight:params:xml:ns:yang:bgp-inet", "2013-09-19", "as-path").intern();
    private static final QName SEGMENT = QName.create("urn:opendaylight:params:xml:ns:yang:bgp-inet", "2013-09-19", "segments").intern();
    private static final QName NEXTHOP = QName.create("urn:opendaylight:params:xml:ns:yang:bgp-inet", "2013-09-19", "c-next-hop").intern();
    private static final QName IPV4NH = QName.create("urn:opendaylight:params:xml:ns:yang:bgp-inet", "2013-09-19", "ipv4-next-hop").intern();
    private static final QName ORIGIN = QName.create("urn:opendaylight:params:xml:ns:yang:bgp-inet", "2013-09-19", "origin").intern();

    private BindingNormalizedNodeSerializer bindingSerializer;

    private static DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> createContBuilder(final QName qname) {
        return ImmutableContainerNodeSchemaAwareBuilder.create().withNodeIdentifier(new NodeIdentifier(qname));
    }

    private static <T> ImmutableLeafNodeBuilder<T> createValueBuilder(final T value, final QName qname, final String localName) {
        final ImmutableLeafNodeBuilder<T> valueBuilder = new ImmutableLeafNodeBuilder<>();
        valueBuilder.withNodeIdentifier(new NodeIdentifier(QName.create(qname, localName))).withValue(value);
        return valueBuilder;
    }

    private static ContainerNode createAttributes() {
        DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> dataContExpected = createContBuilder(DATA_QNAME);

        // as-path pref
        final ContainerNode asPath = createContBuilder(ASPATH).addChild(ImmutableUnkeyedListNodeBuilder.create()
                .withNodeIdentifier(new NodeIdentifier(SEGMENT)).build()).build();
        dataContExpected.addChild(asPath);
        // c-next-hop pref
        final DataContainerNodeBuilder<NodeIdentifier, ChoiceNode> nextHop = Builders.choiceBuilder();
        nextHop.withNodeIdentifier(new NodeIdentifier(NEXTHOP));
        final ContainerNode cNextHop = createContBuilder(IPV4NH)
                .addChild(createValueBuilder("199.20.160.41", IPV4NH, "global").build()).build();
        final ChoiceNode resultNextHop = nextHop.addChild(cNextHop).build();
        dataContExpected.addChild(resultNextHop);
        // origin pref
        final ContainerNode origin = createContBuilder(ORIGIN)
                .addChild(createValueBuilder("igp", ORIGIN, "value").build()).build();
        dataContExpected.addChild(origin);
        return dataContExpected.build();
    }

    protected final AbstractDataBrokerTestCustomizer createDataBrokerTestCustomizer() {
        AbstractDataBrokerTestCustomizer customizer = super.createDataBrokerTestCustomizer();
        this.bindingSerializer = customizer.getBindingToNormalized();
        return customizer;
    }

    @Test
    public void testUseCase() {
        final NodeIdentifier routeAttributesIdentifier
                = new NodeIdentifier(QName.create(Ipv4Route.QNAME.intern(), Attributes.QNAME.getLocalName().intern()));

        final NodeIdentifierWithPredicates routeIdentifier = new NodeIdentifierWithPredicates(Ipv4Route.QNAME,
                QName.create(Ipv4Route.QNAME, "prefix").intern(),
                new Ipv4Prefix("1.1.1.0/24"));

        final YangInstanceIdentifier yii = YangInstanceIdentifier.builder()
                .node(Ipv4Routes.QNAME)
                .append(routeIdentifier)
                .node(Ipv4Route.QNAME)
                .node(routeAttributesIdentifier)
                .build();


        final ContainerNode attributes = createAttributes();

        assertNotNull(this.bindingSerializer.fromNormalizedNode(yii, attributes).getValue());
    }
}
