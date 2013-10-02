/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.persist;

import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.persist.api.storage.StorageAdapter;
import org.opendaylight.controller.config.persist.storage.file.FileStorageAdapter;
import org.opendaylight.netconf.util.osgi.ConfigProvider;
import org.w3c.dom.Element;

import com.google.common.base.Optional;

public class PersisterImplTest {
	@Mock
	ConfigProvider mockedConfigProvider;

	@Before
	public void setUpMocks() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testFromProperties() throws Exception {
		doReturn(MockAdapter.class.getName()).when(mockedConfigProvider).getProperty(PersisterImpl.STORAGE_ADAPTER_CLASS_PROP);

		PersisterImpl persisterImpl = PersisterImpl.createFromProperties(mockedConfigProvider).get();
		persisterImpl.persistConfig(null, null);
		persisterImpl.loadLastConfig();
		persisterImpl.persistConfig(null, null);
		persisterImpl.loadLastConfig();

		assertEquals(2, MockAdapter.persist);
		assertEquals(2, MockAdapter.load);
		assertEquals(1, MockAdapter.props);
	}

	@Test
	public void testFromProperties2() throws Exception {
		mockedConfigProvider = mock(ConfigProvider.class);
		doReturn(FileStorageAdapter.class.getName()).when(mockedConfigProvider).getProperty(PersisterImpl.STORAGE_ADAPTER_CLASS_PROP);
		doReturn("target" + File.separator + "generated-test-sources" + File.separator + "testFile").when(mockedConfigProvider).getProperty(
				FileStorageAdapter.FILE_STORAGE_PROP);
		doReturn("mockedConfigProvider").when(mockedConfigProvider).toString();
		doReturn(null).when(mockedConfigProvider).getProperty("numberOfBackups");

		PersisterImpl persisterImpl = PersisterImpl.createFromProperties(mockedConfigProvider).get();
		assertTrue(persisterImpl.getStorage() instanceof FileStorageAdapter);
	}

	@Test
	public void testFromProperties3() throws Exception {
		mockedConfigProvider = mock(ConfigProvider.class);
		doReturn(FileStorageAdapter.class.getName()).when(mockedConfigProvider).getProperty(PersisterImpl.STORAGE_ADAPTER_CLASS_PROP);
		doReturn("target" + File.separator + "generated-test-sources" + File.separator + "testFile").when(mockedConfigProvider).getProperty(
				FileStorageAdapter.FILE_STORAGE_PROP);
		doReturn("mockedConfigProvider").when(mockedConfigProvider).toString();
		doReturn("0").when(mockedConfigProvider).getProperty("numberOfBackups");
		try {
			PersisterImpl.createFromProperties(mockedConfigProvider).get();
			fail();
		} catch (RuntimeException e) {
			assertThat(e.getMessage(),
					containsString("numberOfBackups property should be either set to positive value, or ommited. Can not be set to 0."));
		}
	}

	@Test
	public void test() throws Exception {
		StorageAdapter storage = mock(StorageAdapter.class);
		doReturn(null).when(storage).loadLastConfig();
		doNothing().when(storage).persistConfig(any(Element.class), anySetOf(String.class));
		PersisterImpl persister = new PersisterImpl(storage);
		persister.loadLastConfig();
		persister.persistConfig(null, null);

		verify(storage).loadLastConfig();
		verify(storage).persistConfig(any(Element.class), anySetOf(String.class));
	}

	public static class MockAdapter implements StorageAdapter {

		static int persist = 0;

		@Override
		public void persistConfig(Element snapshot, Set<String> caps) throws IOException {
			persist++;
		}

		static int load = 0;

		@Override
		public Optional<PersistedConfig> loadLastConfig() throws IOException {
			load++;
			return null;// ?
		}

		static int props = 0;

		@Override
		public void setProperties(ConfigProvider configProvider) {
			props++;
		}

		@Override
		public void close() throws IOException {
			// TODO Auto-generated method stub

		}

	}

}
