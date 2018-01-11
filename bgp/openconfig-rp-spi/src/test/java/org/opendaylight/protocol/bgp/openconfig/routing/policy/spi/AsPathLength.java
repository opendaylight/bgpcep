package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.RouteAttributeContainer.routeAttributeContainerFalse;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.RouteAttributeContainer;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.Statement;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.as.path.SegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class AsPathLength extends AbstractStatementRegistryTest {
    @Mock
    private BGPRouteEntryExportParameters exportParameters;
    private List<Statement> basicStatements;
    @Mock
    private RouteEntryBaseAttributes baseAttributes;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.basicStatements = loadStatement("basic-statements-test");
        doReturn(CLUSTER).when(this.baseAttributes).getClusterId();
        doReturn(LOCAL_AS).when(this.baseAttributes).getLocalAs();
        doReturn(IPV4).when(this.baseAttributes).getOriginatorId();
    }

    @Test
    public void testASPathLengthEq() {
        doReturn(new YangInstanceIdentifier.NodeIdentifierWithPredicates(Ipv4Route.QNAME,
                ImmutableMap.of(QName.create(Ipv4Route.QNAME, "prefix").intern(), "10.3.191.0/22")))
                .when(this.exportParameters).getRouteId();
        doReturn(PeerRole.Ibgp).when(this.exportParameters).getFromPeerRole();

        final AsPathBuilder asPath = new AsPathBuilder();
        asPath.setSegments(Collections.singletonList(new SegmentsBuilder()
                .setAsSequence(Arrays.asList(
                        AsNumber.getDefaultInstance("1"),
                        AsNumber.getDefaultInstance("2"),
                        AsNumber.getDefaultInstance("3")))
                .build()));

        Statement statement = this.basicStatements.stream()
                .filter(st -> st.getName().equals("as-path-eq-length-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(new AttributesBuilder()
                .setAsPath(asPath.build())
                .build());

        RouteAttributeContainer result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNotNull(result.getAttributes());

        asPath.setSegments(Collections.singletonList(new SegmentsBuilder()
                .setAsSequence(Arrays.asList(
                        AsNumber.getDefaultInstance("1"),
                        AsNumber.getDefaultInstance("3")))
                .build()));

        attributeContainer = routeAttributeContainerFalse(new AttributesBuilder().setAsPath(asPath.build()).build());
        result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNull(result.getAttributes());
    }

    @Test
    public void testASPathLengthGe() {
        doReturn(new YangInstanceIdentifier.NodeIdentifierWithPredicates(Ipv4Route.QNAME,
                ImmutableMap.of(QName.create(Ipv4Route.QNAME, "prefix").intern(), "10.3.191.0/22")))
                .when(this.exportParameters).getRouteId();
        doReturn(PeerRole.Ibgp).when(this.exportParameters).getFromPeerRole();

        final AsPathBuilder asPath = new AsPathBuilder();
        asPath.setSegments(Collections.singletonList(new SegmentsBuilder()
                .setAsSequence(Arrays.asList(
                        AsNumber.getDefaultInstance("1"),
                        AsNumber.getDefaultInstance("2"),
                        AsNumber.getDefaultInstance("3")))
                .build()));

        Statement statement = this.basicStatements.stream()
                .filter(st -> st.getName().equals("as-path-ge-length-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(new AttributesBuilder()
                .setAsPath(asPath.build())
                .build());

        RouteAttributeContainer result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNull(result.getAttributes());

        asPath.setSegments(Collections.singletonList(new SegmentsBuilder()
                .setAsSequence(Arrays.asList(
                        AsNumber.getDefaultInstance("3")))
                .build()));

        attributeContainer = routeAttributeContainerFalse(new AttributesBuilder().setAsPath(asPath.build()).build());
        result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNotNull(result.getAttributes());
    }

    @Test
    public void testASPathLengthLe() {
        doReturn(new YangInstanceIdentifier.NodeIdentifierWithPredicates(Ipv4Route.QNAME,
                ImmutableMap.of(QName.create(Ipv4Route.QNAME, "prefix").intern(), "10.3.191.0/22")))
                .when(this.exportParameters).getRouteId();
        doReturn(PeerRole.Ibgp).when(this.exportParameters).getFromPeerRole();

        final AsPathBuilder asPath = new AsPathBuilder();
        asPath.setSegments(Collections.singletonList(new SegmentsBuilder()
                .setAsSequence(Arrays.asList(
                        AsNumber.getDefaultInstance("1"),
                        AsNumber.getDefaultInstance("2"),
                        AsNumber.getDefaultInstance("3")))
                .build()));

        Statement statement = this.basicStatements.stream()
                .filter(st -> st.getName().equals("as-path-le-length-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(new AttributesBuilder()
                .setAsPath(asPath.build())
                .build());

        RouteAttributeContainer result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNotNull(result.getAttributes());

        asPath.setSegments(Collections.singletonList(new SegmentsBuilder()
                .setAsSequence(Arrays.asList(
                        AsNumber.getDefaultInstance("3")))
                .build()));

        attributeContainer = routeAttributeContainerFalse(new AttributesBuilder().setAsPath(asPath.build()).build());
        result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNull(result.getAttributes());
    }
}
