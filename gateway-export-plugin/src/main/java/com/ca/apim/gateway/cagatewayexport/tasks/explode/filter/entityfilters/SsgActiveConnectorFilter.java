package com.ca.apim.gateway.cagatewayexport.tasks.explode.filter.entityfilters;

import com.ca.apim.gateway.cagatewayconfig.beans.Bundle;
import com.ca.apim.gateway.cagatewayconfig.beans.SsgActiveConnector;
import com.ca.apim.gateway.cagatewayexport.tasks.explode.filter.EntityFilter;
import com.ca.apim.gateway.cagatewayexport.tasks.explode.filter.FilterConfiguration;
import com.ca.apim.gateway.cagatewayexport.util.gateway.DependencyUtils;
import org.jetbrains.annotations.NotNull;

import javax.inject.Singleton;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class SsgActiveConnectorFilter implements EntityFilter<SsgActiveConnector> {
    private static final Set<Class<? extends EntityFilter>> FILTER_DEPENDENCIES = Stream.of(
            PolicyFilter.class,
            ServiceFilter.class).collect(Collectors.toSet());
    private static final String ENTITY_NAME = "activeConnectors";

    @Override
    public @NotNull Collection<Class<? extends EntityFilter>> getDependencyEntityFilters() {
        return FILTER_DEPENDENCIES;
    }

    @Override
    public List<SsgActiveConnector> filter(String folderPath, FilterConfiguration filterConfiguration, Bundle bundle, Bundle filteredBundle) {
        List<SsgActiveConnector> ssgActiveConnectors = DependencyUtils.filterDependencies(SsgActiveConnector.class, bundle, filteredBundle, e -> filterConfiguration.getRequiredEntityNames(ENTITY_NAME).contains(e.getName()));
        DependencyUtils.validateEntitiesInList(ssgActiveConnectors, filterConfiguration.getRequiredEntityNames(ENTITY_NAME), "Ssg Active Connector(s)");
        return ssgActiveConnectors;
    }

    @Override
    public String getFilterableEntityName() {
        return ENTITY_NAME;
    }
}
