/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.Conditions1;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.Conditions1Builder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.conditions.BgpConditions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.conditions.BgpConditionsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.BGP;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.MatchSetOptionsRestrictedType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.TagType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.TagTypeBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.RoutingPolicyTop;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.generic.actions.route.disposition.AcceptRouteBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.generic.conditions.MatchNeighborSetBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.generic.conditions.MatchPrefixSetBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.generic.conditions.MatchTagSetBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.generic.defined.sets.NeighborSets;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.generic.defined.sets.NeighborSetsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.generic.defined.sets.PrefixSets;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.generic.defined.sets.PrefixSetsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.generic.defined.sets.TagSets;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.generic.defined.sets.TagSetsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.igp.actions.IgpActionsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.igp.conditions.IgpConditionsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.neighbor.set.NeighborSet;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.neighbor.set.NeighborSetBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.neighbor.set.neighbor.set.NeighborBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.prefix.set.PrefixSet;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.prefix.set.PrefixSetBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.prefix.set.prefix.set.Prefix;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.prefix.set.prefix.set.PrefixBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.RoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.RoutingPolicyBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.DefinedSets;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.DefinedSetsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.PolicyDefinitions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.PolicyDefinitionsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.PolicyDefinition;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.PolicyDefinitionBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.Statements;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.StatementsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.Statement;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.StatementBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Actions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.ActionsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Conditions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.ConditionsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.tag.set.TagSet;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.tag.set.TagSetBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.tag.set.tag.set.TagBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;

public class RoutingPolicyProviderTest extends AbstractDataBrokerTest {
    private static final IpAddress NEIGHBOR_ADDRESS = new IpAddress(new Ipv4Address("127.0.0.1"));
    private static final TagType TAG_TYPE = TagTypeBuilder.getDefaultInstance("ab:cd");
    private static final TagType TAG_TYPE_2 = TagTypeBuilder.getDefaultInstance("aa:bb");

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        final ModuleInfoBackedContext moduleInfoBackedContext = ModuleInfoBackedContext.create();
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(RoutingPolicyTop.class));
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(BgpConditions.class));
        setupWithSchema(moduleInfoBackedContext.tryToCreateSchemaContext().get());
    }

    @Test
    public void testRoutingPolicyProvider() throws TransactionCommitFailedException {
        final RoutingPolicy routingPolicies = new RoutingPolicyBuilder()
            .setDefinedSets(buildDefinedSets())
            .setPolicyDefinitions(createPolicyDefinitions())
            .build();

        final DataBroker dataBroker = getDataBroker();
        final OpenconfigRoutingPolicy routingPolicyProvider = new OpenconfigRoutingPolicy(dataBroker);

        final WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(RoutingPolicy.class), routingPolicies);
        wTx.submit().checkedGet();

    }

    private DefinedSets buildDefinedSets() {
        return new DefinedSetsBuilder()
            .setNeighborSets(buildNeighborSet())
            .setPrefixSets(buildPrefixSets())
            .setTagSets(buildTagSets()).build();
    }

    private TagSets buildTagSets() {
        final TagSet tagSet = new TagSetBuilder()
            .setTagSetName("tags-set-test")
            .setTag(Collections.singletonList(new TagBuilder().setValue(TAG_TYPE).build()))
            .build();
        return new TagSetsBuilder().setTagSet(Collections.singletonList(tagSet)).build();
    }

    private PrefixSets buildPrefixSets() {
        final Prefix prefix = new PrefixBuilder()
            .setIpPrefix(new IpPrefix(new Ipv4Prefix("10.3.192.0/21"))).setMasklengthRange("21..24").build();
        final Prefix prefixExact = new PrefixBuilder()
            .setIpPrefix(new IpPrefix(new Ipv4Prefix("10.2.192.0/21"))).setMasklengthRange("exact").build();
        final PrefixSet prefixSet = new PrefixSetBuilder()
            .setPrefixSetName("prefix-set-test").setPrefix(Arrays.asList(prefix, prefixExact)).build();
        return new PrefixSetsBuilder().setPrefixSet(Collections.singletonList(prefixSet)).build();
    }

    private NeighborSets buildNeighborSet() {
        final NeighborSet neighborSet = new NeighborSetBuilder()
            .setNeighborSetName("neighbor-set-test")
            .setNeighbor(Collections.singletonList(new NeighborBuilder().setAddress(NEIGHBOR_ADDRESS).build()))
            .build();
        return new NeighborSetsBuilder().setNeighborSet(Collections.singletonList(neighborSet)).build();
    }

    /**
     * Creates List of Policy Definitions
     */
    private PolicyDefinitions createPolicyDefinitions() {
        final List<PolicyDefinition> policyDefinitionList = Collections.singletonList(buildPolicy());
        return new PolicyDefinitionsBuilder().setPolicyDefinition(policyDefinitionList).build();
    }

    /**
     * Creates Policy containing a list of statements
     */
    private PolicyDefinition buildPolicy() {
        return new PolicyDefinitionBuilder().setName("policyDefinition1").setStatements(createStatements()).build();
    }

    /**
     * Create list of policy Statements
     */
    private Statements createStatements() {
        return new StatementsBuilder().setStatement(Collections.singletonList(createStatement())).build();
    }

    /**
     * Create Statement
     * Tupla Condition-Action
     */
    private Statement createStatement() {
        return new StatementBuilder()
            .setName("Condition-Action")
            .setConditions(buildConditions())
            .setActions(buildActions())
            .build();
    }

    private Conditions buildConditions() {
        return new ConditionsBuilder()
            .setCallPolicy("policyDefinition2")
            .setIgpConditions(new IgpConditionsBuilder().build())
            .setInstallProtocolEq(BGP.class)
            .setMatchNeighborSet(new MatchNeighborSetBuilder()
                .setMatchSetOptions(MatchSetOptionsRestrictedType.ANY).setNeighborSet("neighbor-set-test").build())
            .setMatchPrefixSet(new MatchPrefixSetBuilder().setPrefixSet("prefix-set-test")
                .setMatchSetOptions(MatchSetOptionsRestrictedType.INVERT).build())
            .setMatchTagSet(new MatchTagSetBuilder()
                .setMatchSetOptions(MatchSetOptionsRestrictedType.ANY).setTagSet("tags-set-test").build())
            .addAugmentation(Conditions1.class, buildBgpConditions())
            .build();
    }

    private Conditions1 buildBgpConditions() {
        return new Conditions1Builder()
            .setBgpConditions(new BgpConditionsBuilder()
                .build()).build();
    }

    private Actions buildActions() {
        return new ActionsBuilder()
            .setIgpActions(new IgpActionsBuilder().setSetTag(TAG_TYPE_2).build())
            .setRouteDisposition(new AcceptRouteBuilder().setAcceptRoute(true).build())
            .build();
    }
}