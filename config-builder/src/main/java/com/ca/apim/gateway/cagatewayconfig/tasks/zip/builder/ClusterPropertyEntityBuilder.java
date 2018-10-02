/*
 * Copyright (c) 2018 CA. All rights reserved.
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.ca.apim.gateway.cagatewayconfig.tasks.zip.builder;

import com.ca.apim.gateway.cagatewayconfig.tasks.zip.beans.Bundle;
import com.ca.apim.gateway.cagatewayconfig.util.IdGenerator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ca.apim.gateway.cagatewayconfig.tasks.zip.builder.EntityBuilderHelper.getEntityWithNameMapping;
import static com.ca.apim.gateway.cagatewayconfig.tasks.zip.builder.EntityBuilderHelper.getEntityWithOnlyMapping;
import static com.ca.apim.gateway.cagatewayconfig.util.entity.EntityTypes.CLUSTER_PROPERTY_TYPE;
import static com.ca.apim.gateway.cagatewayconfig.util.gateway.BundleElementNames.*;
import static com.ca.apim.gateway.cagatewayconfig.util.gateway.MappingProperties.FAIL_ON_EXISTING;
import static com.ca.apim.gateway.cagatewayconfig.util.properties.PropertyConstants.PREFIX_ENV;
import static com.ca.apim.gateway.cagatewayconfig.util.properties.PropertyConstants.PREFIX_GATEWAY;
import static com.ca.apim.gateway.cagatewayconfig.util.xml.DocumentUtils.createElementWithAttribute;
import static com.ca.apim.gateway.cagatewayconfig.util.xml.DocumentUtils.createElementWithTextContent;

@Singleton
public class ClusterPropertyEntityBuilder implements EntityBuilder {

    private static final Integer ORDER = 500;
    private final IdGenerator idGenerator;

    @Inject
    ClusterPropertyEntityBuilder(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    public List<Entity> build(Bundle bundle, BundleType bundleType, Document document) {
        Stream.Builder<Entity> streamBuilder = Stream.builder();
        switch (bundleType) {
            case DEPLOYMENT:
                bundle.getStaticProperties().entrySet().stream().map(propertyEntry ->
                        buildClusterPropertyEntity(propertyEntry.getKey(), propertyEntry.getValue(), document)
                ).forEach(streamBuilder);
                bundle.getEnvironmentProperties().entrySet().stream()
                        .filter(propertyEntry -> propertyEntry.getKey().startsWith(PREFIX_GATEWAY))
                        .map(propertyEntry ->
                                getEntityWithOnlyMapping(CLUSTER_PROPERTY_TYPE, PREFIX_ENV + propertyEntry.getKey().substring(8), idGenerator.generate())
                        ).forEach(streamBuilder);
                break;
            case ENVIRONMENT:
                bundle.getEnvironmentProperties().entrySet().stream()
                        .filter(propertyEntry -> propertyEntry.getKey().startsWith(PREFIX_GATEWAY))
                        .map(propertyEntry ->
                                buildClusterPropertyEntity(PREFIX_ENV + propertyEntry.getKey().substring(8), propertyEntry.getValue(), document)
                        ).forEach(streamBuilder);
                break;
            default:
                throw new EntityBuilderException("Unknown bundle type: " + bundleType);
        }
        return streamBuilder.build().collect(Collectors.toList());
    }

    @Override
    public Integer getOrder() {
        return ORDER;
    }

    private Entity buildClusterPropertyEntity(String name, String value, Document document) {
        String id = idGenerator.generate();
        Entity entity = getEntityWithNameMapping(CLUSTER_PROPERTY_TYPE, name, id, buildClusterPropertyElement(name, id, value, document));
        entity.setMappingProperty(FAIL_ON_EXISTING, true);
        return entity;
    }

    private static Element buildClusterPropertyElement(String name, String id, String value, Document document) {
        return buildClusterPropertyElement(name, id, value, document, Collections.emptyMap());
    }

    private static Element buildClusterPropertyElement(String name, String id, String value, Document document, Map<String, String> valueAttributes) {
        Element clusterPropertyElement = createElementWithAttribute(document, CLUSTER_PROPERTY, ATTRIBUTE_ID, id);

        clusterPropertyElement.appendChild(createElementWithTextContent(document, NAME, name));

        Element valueElement = createElementWithTextContent(document, VALUE, value);
        valueAttributes.forEach(valueElement::setAttribute);
        clusterPropertyElement.appendChild(valueElement);
        return clusterPropertyElement;
    }
}
