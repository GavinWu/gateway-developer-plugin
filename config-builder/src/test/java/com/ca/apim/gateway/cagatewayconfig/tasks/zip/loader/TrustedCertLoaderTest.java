/*
 * Copyright (c) 2018 CA. All rights reserved.
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.ca.apim.gateway.cagatewayconfig.tasks.zip.loader;

import com.ca.apim.gateway.cagatewayconfig.tasks.zip.beans.Bundle;
import com.ca.apim.gateway.cagatewayconfig.tasks.zip.beans.TrustedCert;
import com.ca.apim.gateway.cagatewayconfig.util.file.FileUtils;
import com.ca.apim.gateway.cagatewayconfig.util.json.JsonTools;
import io.github.glytching.junit.extension.folder.TemporaryFolder;
import io.github.glytching.junit.extension.folder.TemporaryFolderExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extensions;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.shaded.com.google.common.io.Files;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;

@Extensions({ @ExtendWith(MockitoExtension.class), @ExtendWith(TemporaryFolderExtension.class) })
public class TrustedCertLoaderTest {

    private TemporaryFolder rootProjectDir;
    private JsonTools jsonTools;
    @Mock
    private FileUtils fileUtils;

    @BeforeEach
    void setUp(TemporaryFolder rootProjectDir) {
        jsonTools = new JsonTools(fileUtils);
        this.rootProjectDir = rootProjectDir;
    }

    @Test
    void loadTrustedCertUrlYml() throws IOException {
        final TrustedCertLoader trustedCertLoader = new TrustedCertLoader(jsonTools);
        final String json = "https://ca.com:\n" +
                "    verifyHostname: false\n" +
                "    trustedForSsl: true\n" +
                "    trustedAsSamlAttestingEntity: false\n" +
                "    trustAnchor: true\n" +
                "    revocationCheckingEnabled: true\n" +
                "    trustedForSigningClientCerts: true\n" +
                "    trustedForSigningServerCerts: true\n" +
                "    trustedAsSamlIssuer: false\n";
        final File configFolder = rootProjectDir.createDirectory("config");
        final File identityProvidersFile = new File(configFolder, "trusted-certs.yml");
        Files.touch(identityProvidersFile);

        Mockito.when(fileUtils.getInputStream(Mockito.any(File.class))).thenReturn(new ByteArrayInputStream(json.getBytes(Charset.forName("UTF-8"))));

        final Bundle bundle = new Bundle();
        trustedCertLoader.load(bundle, rootProjectDir.getRoot());
        assertEquals(1, bundle.getTrustedCerts().size());
        final TrustedCert trustedCert = bundle.getTrustedCerts().get("https://ca.com");
        assertEquals(8, trustedCert.createProperties().size());
    }

    @Test
    void loadTrustedCertUrlJson() throws IOException {
        final TrustedCertLoader trustedCertLoader = new TrustedCertLoader(jsonTools);
        final String json = "{\n" +
                "  \"https://ca.com\" : {\n" +
                "      \"verifyHostname\" : false,\n" +
                "      \"trustedForSsl\" : true,\n" +
                "      \"trustedAsSamlAttestingEntity\" : false,\n" +
                "      \"trustAnchor\" : true,\n" +
                "      \"revocationCheckingEnabled\" : true,\n" +
                "      \"trustedForSigningClientCerts\" : true,\n" +
                "      \"trustedForSigningServerCerts\" : true,\n" +
                "      \"trustedAsSamlIssuer\" : false\n" +
                "  }\n" +
                "}";
        final File configFolder = rootProjectDir.createDirectory("config");
        final File identityProvidersFile = new File(configFolder, "trusted-certs.json");
        Files.touch(identityProvidersFile);

        Mockito.when(fileUtils.getInputStream(Mockito.any(File.class))).thenReturn(new ByteArrayInputStream(json.getBytes(Charset.forName("UTF-8"))));

        final Bundle bundle = new Bundle();
        trustedCertLoader.load(bundle, rootProjectDir.getRoot());
        assertEquals(1, bundle.getTrustedCerts().size());
        final TrustedCert trustedCert = bundle.getTrustedCerts().get("https://ca.com");
        assertEquals(8, trustedCert.createProperties().size());
    }

    @Test
    void loadTrustedCertYml() throws IOException {
        final TrustedCertLoader trustedCertLoader = new TrustedCertLoader(jsonTools);
        final String yml = "fake-cert:\n" +
                "    verifyHostname: false\n" +
                "    trustedForSsl: true\n" +
                "    trustedAsSamlAttestingEntity: false\n" +
                "    trustAnchor: true\n" +
                "    revocationCheckingEnabled: true\n" +
                "    trustedForSigningClientCerts: true\n" +
                "    trustedForSigningServerCerts: true\n" +
                "    trustedAsSamlIssuer: false";
        final File configFolder = rootProjectDir.createDirectory("config");
        final File identityProvidersFile = new File(configFolder, "trusted-certs.yml");
        Files.touch(identityProvidersFile);

        Mockito.when(fileUtils.getInputStream(Mockito.any(File.class))).thenReturn(new ByteArrayInputStream(yml.getBytes(Charset.forName("UTF-8"))));

        final Bundle bundle = new Bundle();
        trustedCertLoader.load(bundle, rootProjectDir.getRoot());
        assertEquals(1, bundle.getTrustedCerts().size());
        final TrustedCert trustedCert = bundle.getTrustedCerts().get("fake-cert");
        assertEquals(8, trustedCert.createProperties().size());
    }

}