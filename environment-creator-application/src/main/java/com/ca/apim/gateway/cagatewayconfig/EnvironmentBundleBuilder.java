package com.ca.apim.gateway.cagatewayconfig;

import com.ca.apim.gateway.cagatewayconfig.tasks.zip.beans.Bundle;
import com.ca.apim.gateway.cagatewayconfig.tasks.zip.loader.EntityLoader;
import com.ca.apim.gateway.cagatewayconfig.tasks.zip.loader.EntityLoaderRegistry;

import java.util.Map;

public class EnvironmentBundleBuilder {

    private final Bundle bundle;
    private final EntityLoaderRegistry entityLoaderRegistry;

    public EnvironmentBundleBuilder(Map<String, String> environmentProperties, EntityLoaderRegistry entityLoaderRegistry) {
        bundle = new Bundle();
        this.entityLoaderRegistry = entityLoaderRegistry;
        environmentProperties.entrySet().stream().filter(e -> e.getKey().startsWith("ENV.")).forEach(e -> addEnvToBundle(e.getKey(), e.getValue()));
    }

    private void addEnvToBundle(String key, String value) {
        if (key.startsWith("ENV.")) {
            String environmentKey = key.substring(4);
            int typeEndIndex = environmentKey.indexOf('.');
            if(typeEndIndex != -1) {
                String type = environmentKey.substring(0, typeEndIndex);
                EntityLoader loader = entityLoaderRegistry.getLoader(type);
                if(loader != null) {
                    String name = environmentKey.substring(type.length() + 1);
                    loader.load(bundle, name, value);
                }
            }
        }
    }

    public Bundle getBundle() {
        return bundle;
    }
}
