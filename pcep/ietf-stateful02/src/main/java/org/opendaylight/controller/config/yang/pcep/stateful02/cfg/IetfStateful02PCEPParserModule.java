/**
 * Generated file

 * Generated from: yang module name: odl-pcep-impl-cfg  yang module local name: pcep-parser-ietf-stateful02
 * Generated by: org.opendaylight.controller.config.yangjmxgenerator.plugin.JMXGenerator
 * Generated at: Wed Jan 22 15:11:39 CET 2014
 *
 * Do not modify this file unless it is present under src/main directory
 */
package org.opendaylight.controller.config.yang.pcep.stateful02.cfg;

import org.opendaylight.protocol.pcep.ietf.stateful02.StatefulActivator;

/**
*
*/
public final class IetfStateful02PCEPParserModule extends
        org.opendaylight.controller.config.yang.pcep.stateful02.cfg.AbstractIetfStateful02PCEPParserModule {

    public IetfStateful02PCEPParserModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public IetfStateful02PCEPParserModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            final IetfStateful02PCEPParserModule oldModule, final java.lang.AutoCloseable oldInstance) {

        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new StatefulActivator();
    }
}
