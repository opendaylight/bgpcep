/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.integration.pcep;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import javax.inject.Inject;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.test.sal.binding.it.TestHelper;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.BundleContext;

//FIXME: merge with org.opendaylight.controller.test.sal.binding.it.AbstractTest ?
public class AbstractPcepOsgiTest {
    @Inject
    @Filter(timeout = 120 * 1000)
    BindingAwareBroker broker;
    @Inject
    BundleContext bundleContext;

    public BindingAwareBroker getBroker() {
        return this.broker;
    }

    public void setBroker(final BindingAwareBroker broker) {
        this.broker = broker;
    }

    public BundleContext getBundleContext() {
        return this.bundleContext;
    }

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Configuration
    public Option[] config() {
        return options(
                // TestHelper.getLoggingBundles(), //

                pcepModules(), //
                systemProperty("osgi.bundles.defaultStartLevel").value("4"),
                systemProperty("pax.exam.osgi.unresolved.fail").value("true"),
                systemPackages("sun.nio.ch"),

                mavenBundle("org.slf4j", "slf4j-api").versionAsInProject(), //
                mavenBundle("org.slf4j", "log4j-over-slf4j").versionAsInProject(), //
                mavenBundle("ch.qos.logback", "logback-core").versionAsInProject(), //
                mavenBundle("ch.qos.logback", "logback-classic").versionAsInProject(),
                mavenBundle("openexi", "nagasena").versionAsInProject(),
                mavenBundle("com.github.romix", "java-concurrent-hash-trie-map").versionAsInProject(),

                TestHelper.mdSalCoreBundles(),

                TestHelper.bindingAwareSalBundles(), TestHelper.configMinumumBundles(), TestHelper.baseModelBundles(),
                TestHelper.junitAndMockitoBundles(), TestHelper.protocolFrameworkBundles());
    }

    private Option pcepModules() {
        return new DefaultCompositeOption(
                mavenBundle("org.opendaylight.mdsal.model", "ietf-topology").versionAsInProject(), //
                mavenBundle("org.opendaylight.yangtools", "object-cache-api").versionAsInProject(), //
                mavenBundle("org.opendaylight.bgpcep", "pcep-topology-api").versionAsInProject(), //
                mavenBundle("org.opendaylight.bgpcep", "pcep-tunnel-api").versionAsInProject(), //
                mavenBundle("org.opendaylight.bgpcep", "pcep-api").versionAsInProject(), //
                mavenBundle("org.opendaylight.bgpcep", "pcep-impl").versionAsInProject(), //
                mavenBundle("org.opendaylight.bgpcep", "pcep-spi").versionAsInProject(), //
                mavenBundle("org.opendaylight.bgpcep", "pcep-ietf-stateful07").versionAsInProject(), //
                mavenBundle("org.opendaylight.tcpmd5", "tcpmd5-api").versionAsInProject(), //
                mavenBundle("org.opendaylight.tcpmd5", "tcpmd5-netty").versionAsInProject(), //
                mavenBundle("org.opendaylight.bgpcep", "topology-api").versionAsInProject(), //
                mavenBundle("org.opendaylight.bgpcep", "topology-tunnel-api").versionAsInProject(), //
                mavenBundle("org.opendaylight.bgpcep", "programming-topology-api").versionAsInProject(), //
                mavenBundle("org.opendaylight.bgpcep", "programming-tunnel-api").versionAsInProject(), //
                mavenBundle("org.opendaylight.bgpcep", "concepts").versionAsInProject(), //
                mavenBundle("org.opendaylight.bgpcep", "util").versionAsInProject(), //
                mavenBundle("org.opendaylight.bgpcep", "rsvp-api").versionAsInProject(), //
                mavenBundle("org.opendaylight.bgpcep", "programming-api").versionAsInProject());
    }

    abstract class AbstractTestProvider implements BindingAwareProvider {

    }

    TopologyKey getTopologyId(final String id) {
        return new TopologyKey(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId(id));
    }
}
