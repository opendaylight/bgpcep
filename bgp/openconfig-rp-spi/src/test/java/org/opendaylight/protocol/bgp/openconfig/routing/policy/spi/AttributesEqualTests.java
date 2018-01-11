package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.RouteAttributeContainer.routeAttributeContainerFalse;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.RouteAttributeContainer;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.Statement;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.MultiExitDiscBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv6.next.hop._case.Ipv6NextHopBuilder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class AttributesEqualTests extends AbstractStatementRegistryTest {
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
    public void testMedEq() {
        doReturn(new YangInstanceIdentifier.NodeIdentifierWithPredicates(Ipv4Route.QNAME,
                ImmutableMap.of(QName.create(Ipv4Route.QNAME, "prefix").intern(), "10.3.191.0/22")))
                .when(this.exportParameters).getRouteId();
        doReturn(PeerRole.Ibgp).when(this.exportParameters).getFromPeerRole();

        Statement statement = this.basicStatements.stream()
                .filter(st -> st.getName().equals("med-eq-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(new AttributesBuilder()
                .setMultiExitDisc(new MultiExitDiscBuilder().setMed(200L).build())
                .build());

        RouteAttributeContainer result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNotNull(result.getAttributes());


        attributeContainer = routeAttributeContainerFalse(new AttributesBuilder()
                .setMultiExitDisc(new MultiExitDiscBuilder().setMed(100L).build())
                .build());
        result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNull(result.getAttributes());
    }

    @Test
    public void testOriginEq() {
        doReturn(new YangInstanceIdentifier.NodeIdentifierWithPredicates(Ipv4Route.QNAME,
                ImmutableMap.of(QName.create(Ipv4Route.QNAME, "prefix").intern(), "10.3.191.0/22")))
                .when(this.exportParameters).getRouteId();
        doReturn(PeerRole.Ibgp).when(this.exportParameters).getFromPeerRole();

        Statement statement = this.basicStatements.stream()
                .filter(st -> st.getName().equals("origin-eq-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(new AttributesBuilder()
                .setOrigin(new OriginBuilder().setValue(BgpOrigin.Egp).build())
                .build());

        RouteAttributeContainer result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNotNull(result.getAttributes());


        attributeContainer = routeAttributeContainerFalse(new AttributesBuilder()
                .setOrigin(new OriginBuilder().setValue(BgpOrigin.Igp).build())
                .build());
        result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNull(result.getAttributes());
    }

    @Test
    public void testNextHopIn() {
        doReturn(new YangInstanceIdentifier.NodeIdentifierWithPredicates(Ipv4Route.QNAME,
                ImmutableMap.of(QName.create(Ipv4Route.QNAME, "prefix").intern(), "10.3.191.0/22")))
                .when(this.exportParameters).getRouteId();
        doReturn(PeerRole.Ibgp).when(this.exportParameters).getFromPeerRole();

        Statement statement = this.basicStatements.stream()
                .filter(st -> st.getName().equals("nexthop-in-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(new AttributesBuilder()
                .setCNextHop(new Ipv6NextHopCaseBuilder().setIpv6NextHop(new Ipv6NextHopBuilder()
                        .setGlobal(new Ipv6Address("2001:db8::1")).build()).build())
                .build());

        RouteAttributeContainer result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNotNull(result.getAttributes());


        attributeContainer = routeAttributeContainerFalse(new AttributesBuilder()
                .setCNextHop(new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder()
                        .setGlobal(new Ipv4Address("42.42.42.42")).build()).build())
                .build());
        result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNull(result.getAttributes());
    }

    @Test
    public void testLocalPref() {
        doReturn(new YangInstanceIdentifier.NodeIdentifierWithPredicates(Ipv4Route.QNAME,
                ImmutableMap.of(QName.create(Ipv4Route.QNAME, "prefix").intern(), "10.3.191.0/22")))
                .when(this.exportParameters).getRouteId();
        doReturn(PeerRole.Ibgp).when(this.exportParameters).getFromPeerRole();

        Statement statement = this.basicStatements.stream()
                .filter(st -> st.getName().equals("local-pref-eq-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(new AttributesBuilder()
                .setLocalPref(new LocalPrefBuilder().setPref(350L).build())
                .build());

        RouteAttributeContainer result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNotNull(result.getAttributes());


        attributeContainer = routeAttributeContainerFalse(new AttributesBuilder()
                .setLocalPref(new LocalPrefBuilder().setPref(100L).build())
                .build());
        result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNull(result.getAttributes());
    }
}
