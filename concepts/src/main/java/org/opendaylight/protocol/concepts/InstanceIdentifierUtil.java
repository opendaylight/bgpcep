/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import java.util.List;

import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.IdentifiableItem;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;

/**
 *
 */
public final class InstanceIdentifierUtil {
	private static final Logger LOG = LoggerFactory.getLogger(InstanceIdentifierUtil.class);

	private InstanceIdentifierUtil() {

	}

	public static <T extends DataObject> InstanceIdentifier<T> firstIdentifierOf(final InstanceIdentifier<?> id, final Class<T> type) {
		final List<PathArgument> p = id.getPath();

		int i = 1;
		for (final PathArgument a : p) {
			if (type.equals(a.getType())) {
				new InstanceIdentifier<>(p.subList(0, i), type);
			}

			++i;
		}

		LOG.debug("Identifier {} does not contain type {}", id, type);
		return null;
	}

	public static <N extends Identifiable<K> & DataObject, K extends Identifier<N>> K firstKeyOf(final InstanceIdentifier<?> id,  final Class<N> listItem, final Class<K> listKey) {
		for (PathArgument i : id.getPath()) {
			if (i.getType().equals(listItem)) {
				@SuppressWarnings("unchecked")
				final K ret = ((IdentifiableItem<N, K>)i).getKey();
				return ret;
			}
		}

		LOG.debug("Identifier {} does not contain type {}", id, listItem);
		return null;
	}

	public static <N extends Identifiable<K> & DataObject, K extends Identifier<N>> K keyOf(final InstanceIdentifier<N> id) {
		@SuppressWarnings("unchecked")
		final K ret = ((IdentifiableItem<N, K>)Iterables.getLast(id.getPath())).getKey();
		return ret;
	}
}
