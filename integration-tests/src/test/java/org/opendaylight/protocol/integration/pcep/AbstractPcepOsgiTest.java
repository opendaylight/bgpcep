/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.integration.pcep;

import static org.ops4j.pax.exam.CoreOptions.maven;
import javax.inject.Inject;
import org.opendaylight.controller.mdsal.it.base.AbstractMdsalTestBase;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class AbstractPcepOsgiTest extends AbstractMdsalTestBase {

    @Inject
    @Filter(timeout = 60 * 1000)
    BundleContext bundleContext;
    BindingAwareBroker broker = null;

    public BindingAwareBroker getBroker() {
        if (this.broker == null) {
            ServiceReference<BindingAwareBroker> serviceReference = this.bundleContext.getServiceReference(BindingAwareBroker.class);
            if (serviceReference == null) {
                throw new RuntimeException("BindingAwareBroker not found");
            }
            this.broker = this.bundleContext.getService(serviceReference);
        }
        return this.broker;
    }

    @Override
    public String getModuleName() {
        return "pcep-topology-provider";
    }

    @Override
    public String getInstanceName() {
        return "pcep-topology";
    }

    @Override
    public MavenUrlReference getFeatureRepo() {
        return maven().groupId("org.opendaylight.bgpcep").artifactId("features-pcep")
                .classifier("features").type("xml").versionAsInProject();
    }

    @Override
    public String getFeatureName() {
        return "odl-bgpcep-pcep-all";
    }

    TopologyKey getTopologyId(final String id) {
        return new TopologyKey(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId(id));
    }

    abstract class AbstractTestProvider implements BindingAwareProvider {
    }
}
