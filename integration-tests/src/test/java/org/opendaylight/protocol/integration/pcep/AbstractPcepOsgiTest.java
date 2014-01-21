/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.integration.pcep;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.protocol.integration.TestHelper;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.BundleContext;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

public class AbstractPcepOsgiTest {
	@Inject
	@Filter(timeout=60*1000)
	BindingAwareBroker broker;
	@Inject
	BundleContext bundleContext;

	public BindingAwareBroker getBroker() {
		return broker;
	}

	public void setBroker(BindingAwareBroker broker) {
		this.broker = broker;
	}

	public BundleContext getBundleContext() {
		return bundleContext;
	}

	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	@Configuration
	public Option[] config() {
		return options(TestHelper.getLoggingBundles(), //

				pcepModules(), //
				systemProperty("osgi.bundles.defaultStartLevel").value("4"),
				systemProperty("pax.exam.osgi.unresolved.fail").value("true"),

				TestHelper.mdSalCoreBundles(),

				TestHelper.bindingAwareSalBundles(),
				TestHelper.configMinumumBundles(),
				TestHelper.baseModelBundles(),
				TestHelper.flowCapableModelBundles(),
                TestHelper.junitAndMockitoBundles());
	}

	private Option pcepModules() {
		return new DefaultCompositeOption(mavenBundle("org.opendaylight.yangtools.model", "ietf-topology").versionAsInProject(), //
				mavenBundle("org.opendaylight.bgpcep", "pcep-topology-api").versionAsInProject(), //
				mavenBundle("org.opendaylight.bgpcep", "pcep-tunnel-api").versionAsInProject(), //
				mavenBundle("org.opendaylight.bgpcep", "pcep-api").versionAsInProject(), //
				mavenBundle("org.opendaylight.bgpcep", "pcep-spi").versionAsInProject(), //
				mavenBundle("org.opendaylight.bgpcep", "pcep-ietf-stateful02").versionAsInProject(), //
				mavenBundle("org.opendaylight.bgpcep", "pcep-ietf-stateful07").versionAsInProject(), //
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

		@Override
		public Collection<? extends RpcService> getImplementations() {
			return Collections.emptySet();
		}

		@Override
		public Collection<? extends ProviderFunctionality> getFunctionality() {
			return Collections.emptySet();
		}

		@Override
		public void onSessionInitialized(BindingAwareBroker.ConsumerContext session) {}

	}

	TopologyKey getTopologyId(String id) {
		return new TopologyKey(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId(id));
	}
}
