package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.RouteAttributeContainer.routeAttributeContainerFalse;
import static org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.ExportAttributeTestUtil.CLUSTER;
import static org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.ExportAttributeTestUtil.IPV4;
import static org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.ExportAttributeTestUtil.LOCAL_AS;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.EncapsulationTunnelType.Vxlan;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.impl.PolicyRIBBaseParametersImpl;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.RouteAttributeContainer;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.Statement;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.ExtendedCommunitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as._4.spec.common.As4SpecificCommonBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.As4RouteOriginExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.EncapsulationCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.as._4.route.origin.extended.community._case.As4RouteOriginExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.encapsulation._case.EncapsulationExtendedCommunityBuilder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class MatchExtComTest extends AbstractStatementRegistryConsumerTest {
    @Mock
    private BGPRouteEntryExportParameters exportParameters;
    private List<Statement> basicStatements;
    private PolicyRIBBaseParametersImpl baseAttributes;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.basicStatements = loadStatement("ext-community-statements-test");
        this.baseAttributes = new PolicyRIBBaseParametersImpl(LOCAL_AS, IPV4, CLUSTER);
    }

    @Test
    public void testExtComAny() {
        doReturn(new YangInstanceIdentifier.NodeIdentifierWithPredicates(Ipv4Route.QNAME,
                ImmutableMap.of(QName.create(Ipv4Route.QNAME, "prefix").intern(), "10.3.191.0/22")))
                .when(this.exportParameters).getRouteId();
        Statement statement = this.basicStatements.stream()
                .filter(st -> st.getName().equals("ext-community-any-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(
                new AttributesBuilder().build());
        RouteAttributeContainer result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNotNull(result.getAttributes());


        attributeContainer = routeAttributeContainerFalse(new AttributesBuilder()
                .setExtendedCommunities(Collections.singletonList(new ExtendedCommunitiesBuilder()
                        .setExtendedCommunity(new As4RouteOriginExtendedCommunityCaseBuilder()
                                .setAs4RouteOriginExtendedCommunity(new As4RouteOriginExtendedCommunityBuilder()
                                        .setAs4SpecificCommon(new As4SpecificCommonBuilder()
                                                .setAsNumber(AsNumber.getDefaultInstance("65000"))
                                                .setLocalAdministrator(123)
                                                .build()).build()).build()).build())).build());

        result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNull(result.getAttributes());
    }

    @Test
    public void testExtComAll() {
        doReturn(new YangInstanceIdentifier.NodeIdentifierWithPredicates(Ipv4Route.QNAME,
                ImmutableMap.of(QName.create(Ipv4Route.QNAME, "prefix").intern(), "10.3.191.0/22")))
                .when(this.exportParameters).getRouteId();
        Statement statement = this.basicStatements.stream()
                .filter(st -> st.getName().equals("ext-community-all-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(
                new AttributesBuilder().build());
        RouteAttributeContainer result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNotNull(result.getAttributes());


        attributeContainer = routeAttributeContainerFalse(new AttributesBuilder().setExtendedCommunities(Arrays.asList(
                new ExtendedCommunitiesBuilder().setExtendedCommunity(new As4RouteOriginExtendedCommunityCaseBuilder()
                        .setAs4RouteOriginExtendedCommunity(
                                new As4RouteOriginExtendedCommunityBuilder()
                                        .setAs4SpecificCommon(new As4SpecificCommonBuilder()
                                                .setAsNumber(AsNumber.getDefaultInstance("65000"))
                                                .setLocalAdministrator(123)
                                                .build()).build()).build()).build(),
                new ExtendedCommunitiesBuilder().setExtendedCommunity(new EncapsulationCaseBuilder()
                        .setEncapsulationExtendedCommunity(new EncapsulationExtendedCommunityBuilder()
                                .setTunnelType(Vxlan).build()).build()).build())).build());

        result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNull(result.getAttributes());
    }

    @Test
    public void testExtComInvert() {
        doReturn(new YangInstanceIdentifier.NodeIdentifierWithPredicates(Ipv4Route.QNAME,
                ImmutableMap.of(QName.create(Ipv4Route.QNAME, "prefix").intern(), "10.3.191.0/22")))
                .when(this.exportParameters).getRouteId();
        Statement statement = this.basicStatements.stream()
                .filter(st -> st.getName().equals("ext-community-invert-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(
                new AttributesBuilder().build());
        RouteAttributeContainer result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNull(result.getAttributes());

        attributeContainer = routeAttributeContainerFalse(new AttributesBuilder()
                .setExtendedCommunities(Collections.singletonList(new ExtendedCommunitiesBuilder()
                        .setExtendedCommunity(new As4RouteOriginExtendedCommunityCaseBuilder()
                                .setAs4RouteOriginExtendedCommunity(new As4RouteOriginExtendedCommunityBuilder()
                                        .setAs4SpecificCommon(new As4SpecificCommonBuilder()
                                                .setAsNumber(AsNumber.getDefaultInstance("65000"))
                                                .setLocalAdministrator(123)
                                                .build()).build()).build()).build())).build());

        result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNotNull(result.getAttributes()); }
}