/*
 * Copyright (c) 2018 CA. All rights reserved.
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.ca.apim.gateway.cagatewayconfig.bundle.builder;

import com.ca.apim.gateway.cagatewayconfig.beans.Bundle;
import com.ca.apim.gateway.cagatewayconfig.beans.PrivateKey;
import com.ca.apim.gateway.cagatewayconfig.util.gateway.CertificateUtils;
import com.ca.apim.gateway.cagatewayconfig.util.keystore.KeystoreHelper;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ca.apim.gateway.cagatewayconfig.util.entity.EntityTypes.PRIVATE_KEY_TYPE;
import static com.ca.apim.gateway.cagatewayconfig.util.gateway.BuilderUtils.buildAndAppendPropertiesElement;
import static com.ca.apim.gateway.cagatewayconfig.util.gateway.BundleElementNames.*;
import static com.ca.apim.gateway.cagatewayconfig.util.gateway.CertificateUtils.createCertDataElementFromCert;
import static com.ca.apim.gateway.cagatewayconfig.util.gateway.MappingActions.NEW_OR_EXISTING;
import static com.ca.apim.gateway.cagatewayconfig.util.gateway.MappingProperties.FAIL_ON_NEW;
import static com.ca.apim.gateway.cagatewayconfig.util.xml.DocumentUtils.createElementWithAttributes;
import static com.ca.apim.gateway.cagatewayconfig.util.xml.DocumentUtils.createElementWithChildren;
import static java.util.stream.Collectors.toList;

@Singleton
public class PrivateKeyEntityBuilder implements EntityBuilder {

    private static final Logger LOGGER = Logger.getLogger(PolicyEntityBuilder.class.getName());

    private static final Integer ORDER = 800;
    private static final String DUMMY_CERTIFICATE = "MIIBfTCCASegAwIBAgIJAPH69zKKw4ixMA0GCSqGSIb3DQEBBQUAMA8xDTALBgNVBAMTBHRlc3QwHhcNMTgxMDEzMDMyODI1WhcNMzgxMDA4MDMyODI1WjAPMQ0wCwYDVQQDEwR0ZXN0MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAIS+Vr8zPOBmSclkUtW/z0UXaMjhg7dix6IUZs+UoSiw/2GXfU2vc3renVAbn3AZaJEqnxgrcX4nldqt0WBIP4sCAwEAAaNmMGQwDgYDVR0PAQH/BAQDAgXgMBIGA1UdJQEB/wQIMAYGBFUdJQAwHQYDVR0OBBYEFN/aeDDEAB6MTxZhMhf/eJKnmaE5MB8GA1UdIwQYMBaAFN/aeDDEAB6MTxZhMhf/eJKnmaE5MA0GCSqGSIb3DQEBBQUAA0EAdolvh7bMX5ZMkM/yntJlBdzS8ukM/ULh8I11wKd6dDltyMuk9rOP0iEk1nsSFuFL0uQ4kIe12KyDwr8ns7VKvQ==";

    private final KeystoreHelper keystoreHelper;

    @Inject
    PrivateKeyEntityBuilder(final KeystoreHelper keystoreHelper) {
        this.keystoreHelper = keystoreHelper;
    }

    @Override
    public List<Entity> build(Bundle bundle, BundleType bundleType, Document document) {
        Map<String, PrivateKey> privateKeyMap = Optional.ofNullable(bundle.getPrivateKeys()).orElse(Collections.emptyMap());
        return buildEntities(bundle, bundleType, privateKeyMap, document);
    }

    private List<Entity> buildEntities(Bundle bundle, BundleType bundleType, Map<String, ?> entities,
                                       Document document) {
        switch (bundleType) {
            case DEPLOYMENT:
                return entities.entrySet().stream()
                               .map(e -> EntityBuilderHelper.getEntityWithOnlyMapping(PRIVATE_KEY_TYPE, e.getKey(), generatePrivateKeyId((PrivateKey) e.getValue())))
                               .collect(Collectors.toList());
            case ENVIRONMENT:
                return entities.entrySet().stream().map(e -> buildPrivateKeyEntity(bundle, e.getKey(),
                        (PrivateKey) e.getValue(), document)).collect(toList());
            default:
                throw new EntityBuilderException("Unknown bundle type: " + bundleType);
        }
    }

    private Entity buildPrivateKeyEntity(Bundle bundle, String alias, PrivateKey privateKey, Document document) {
        privateKey.setId(generatePrivateKeyId(privateKey));
        if (privateKey.getPrivateKeyFile() == null) {
            privateKey.setPrivateKeyFile(bundle.getPrivateKeyFiles().get(alias));
        }
        final Element privateKeyElem = createElementWithAttributes(
                document,
                PRIVATE_KEY,
                ImmutableMap.of(
                        ATTRIBUTE_ID, privateKey.getId(),
                        ATTRIBUTE_KEYSTORE_ID, privateKey.getKeyStoreType().getId(),
                        ATTRIBUTE_ALIAS, alias)
        );
        buildAndAppendCertificateChainElement(privateKey, privateKeyElem, document);
        buildAndAppendPropertiesElement(ImmutableMap.of(ATTRIBUTE_KEY_ALGORITHM, privateKey.getAlgorithm()), document, privateKeyElem);

        Entity entity = EntityBuilderHelper.getEntityWithNameMapping(PRIVATE_KEY_TYPE, alias, privateKey.getId(), privateKeyElem);
        entity.setMappingAction(NEW_OR_EXISTING);
        entity.setMappingProperty(FAIL_ON_NEW, true);
        return entity;
    }

    private void buildAndAppendCertificateChainElement(PrivateKey privateKey, Element privateKeyElem, Document document) {
        if (privateKey.getPrivateKeyFile() != null) {
            if (StringUtils.isBlank(privateKey.getKeyPassword())) {
                LOGGER.log(Level.WARNING, "PrivateKey password not provided, attempting with blank password.");
                privateKey.setKeyPassword("");
            }
            final KeyStore keyStore = keystoreHelper.loadKeyStore(privateKey);
            final Certificate[] certificates = keystoreHelper.loadCertificatesForPrivateKey(privateKey, keyStore);
            final Element[] certificatesElements = Stream.of(certificates)
                    .map(c -> createCertDataElementFromCert((X509Certificate) c, document)).toArray(Element[]::new);
            privateKeyElem.appendChild(createElementWithChildren(document, CERTIFICATE_CHAIN, certificatesElements));
        } else {
            //add a dummy certificate. This is needed for the gateway to be able to parse the bundle xml, it is not actually used
            privateKeyElem.appendChild(createElementWithChildren(document, CERTIFICATE_CHAIN, CertificateUtils.createCertDataElementFromCert("", BigInteger.valueOf(0), "", DUMMY_CERTIFICATE, document)));
        }
    }

    private String generatePrivateKeyId(PrivateKey privateKey) {
        return privateKey.getKeyStoreType().generateKeyId(privateKey.getAlias());
    }

    @Override
    public @NotNull Integer getOrder() {
        return ORDER;
    }
}
