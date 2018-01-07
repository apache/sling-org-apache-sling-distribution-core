package org.apache.sling.distribution.packaging.impl;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.serialization.DistributionContentSerializer;
import org.apache.sling.distribution.serialization.DistributionExportOptions;
import org.junit.Test;

public class FileDistributionPackageBuilderTest {

    @Test
    public void testDefaultTempDirectory() throws DistributionException, IOException {
        FileDistributionPackageBuilder builder = new FileDistributionPackageBuilder("test", new TestSerializer(), null, null, new String[0],
                new String[0]);
        DistributionPackage createdPackage = builder.createPackageForAdd(mock(ResourceResolver.class), mock(DistributionRequest.class));

        try {
            assertNotNull(createdPackage.createInputStream());
            DistributionPackage gotPackage = builder.getPackageInternal(mock(ResourceResolver.class), createdPackage.getId());
            assertNotNull(gotPackage.createInputStream()); // this will throw an exception when the file doesn't exist
        } finally {
            createdPackage.delete();
        }
    }

    class TestSerializer implements DistributionContentSerializer {

        @Override public void exportToStream(ResourceResolver resourceResolver, DistributionExportOptions exportOptions,
                OutputStream outputStream) throws DistributionException {
            try {
                outputStream.write("test".getBytes());
            } catch (IOException ex) {
                throw new DistributionException(ex);
            }
        }

        @Override public void importFromStream(ResourceResolver resourceResolver, InputStream inputStream) throws DistributionException {
            throw new DistributionException("unsupported");
        }

        @Override public String getName() {
            return "test";
        }

        @Override public boolean isRequestFiltering() {
            return true;
        }
    }
}
