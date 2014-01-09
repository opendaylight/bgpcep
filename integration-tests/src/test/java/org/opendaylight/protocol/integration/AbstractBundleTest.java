/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public abstract class AbstractBundleTest {
	private static final String GROUP = "org.opendaylight.bgpcep";
	private static final String VERSION = "0.3.0-SNAPSHOT";

	@Inject
	BundleContext ctx;

	abstract protected Collection<String> prerequisiteBundles();
	abstract protected Collection<String> requiredBundles();

	private List<Option> coreBundles() {
		final List<Option> ret = new ArrayList<>();

		ret.add(mavenBundle("ch.qos.logback", "logback-classic", "1.0.9"));
		ret.add(mavenBundle("ch.qos.logback", "logback-core", "1.0.9"));
		ret.add(mavenBundle("com.google.guava", "guava", "14.0.1"));
		ret.add(mavenBundle("commons-codec", "commons-codec", "1.7"));
		ret.add(mavenBundle("io.netty", "netty-buffer", "4.0.9.Final"));
		ret.add(mavenBundle("io.netty", "netty-codec", "4.0.9.Final"));
		ret.add(mavenBundle("io.netty", "netty-common", "4.0.9.Final"));
		ret.add(mavenBundle("io.netty", "netty-transport", "4.0.9.Final"));
		ret.add(mavenBundle("org.slf4j", "slf4j-api", "1.7.2"));

		ret.add(mavenBundle("org.opendaylight.yangtools", "concepts", "0.6.0-SNAPSHOT"));
		ret.add(mavenBundle("org.opendaylight.yangtools", "yang-binding", "0.6.0-SNAPSHOT"));
		ret.add(mavenBundle("org.opendaylight.yangtools", "yang-common", "0.5.9-SNAPSHOT"));
		ret.add(mavenBundle("org.opendaylight.yangtools.model", "ietf-inet-types", "2010.09.24.2-SNAPSHOT"));

		ret.add(mavenBundle("org.javassist", "javassist", "3.17.1-GA"));
		ret.add(mavenBundle("org.opendaylight.controller", "config-api", "0.2.3-SNAPSHOT"));
		ret.add(mavenBundle("org.opendaylight.controller", "sal-common-api", "1.0-SNAPSHOT"));
		ret.add(mavenBundle("org.opendaylight.controller", "sal-binding-api", "1.0-SNAPSHOT"));
		ret.add(mavenBundle("org.opendaylight.controller", "sal-binding-broker-impl", "1.0-SNAPSHOT"));
		ret.add(mavenBundle("org.opendaylight.controller", "sal-binding-config", "1.0-SNAPSHOT"));
		ret.add(mavenBundle("org.opendaylight.controller", "sal-common", "1.0-SNAPSHOT"));
		ret.add(mavenBundle("org.opendaylight.yangtools.thirdparty", "xtend-lib-osgi", "2.4.3"));

		return ret;
	}

	private Bundle getBundle(final String name) {
		final String bn = GROUP + "." + name;
		for (Bundle b : ctx.getBundles()) {
			if (bn.equals(b.getSymbolicName())) {
				return b;
			}
		}
		return null;
	}

	private void testBundle(final String name) {
		final Bundle b = getBundle(name);
		assertNotNull("Bundle '" + name + "' not found", b);
		assertEquals("Bundle '" + name + "' is not in ACTIVE state", Bundle.ACTIVE, b.getState());
	}

	@Configuration
	public final Option[] config() {
		final List<Option> options = coreBundles();

		for (final String s : prerequisiteBundles()) {
			options.add(mavenBundle(GROUP, s, VERSION));
		}

		for (final String s : requiredBundles()) {
			options.add(mavenBundle(GROUP, s, VERSION));
		}

		options.addAll(Arrays.asList(junitBundles()));
		return options.toArray(new Option[0]);
	}

	@Test
	public final void testBundleActivation() throws BundleException {
		for (final String s : requiredBundles()) {
			testBundle(s);
		}
	}
}
