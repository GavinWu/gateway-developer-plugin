package com.ca.apim.gateway.cagatewayexport.tasks.explode.filter.entityfilters;

import com.ca.apim.gateway.cagatewayexport.tasks.explode.bundle.Bundle;
import com.ca.apim.gateway.cagatewayexport.tasks.explode.bundle.entity.Dependency;
import com.ca.apim.gateway.cagatewayexport.tasks.explode.bundle.entity.JdbcConnectionEntity;
import com.ca.apim.gateway.cagatewayexport.tasks.explode.bundle.entity.ListenPortEntity;
import com.ca.apim.gateway.cagatewayexport.tasks.explode.bundle.entity.PolicyEntity;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ListenPortFilterTest {
    @Test
    void filterNoEntities() {
        ListenPortFilter filter = new ListenPortFilter();

        Bundle filteredBundle = new Bundle();
        Bundle bundle = FilterTestUtils.getBundle();
        bundle.setDependencies(Collections.emptyMap());

        List<ListenPortEntity> filteredEntities = filter.filter("/my/folder/path", bundle, filteredBundle);

        assertEquals(0, filteredEntities.size());
    }

    @Test
    void filter() {
        ListenPortFilter filter = new ListenPortFilter();

        Bundle filteredBundle = new Bundle();
        filteredBundle.addEntity(new PolicyEntity("my-policy", "1", "", "", null, ""));
        Bundle bundle = FilterTestUtils.getBundle();
        bundle.setDependencies(
                ImmutableMap.of(
                        new Dependency("1", PolicyEntity.class), Arrays.asList(new Dependency("2", ListenPortEntity.class), new Dependency("3", ListenPortEntity.class)),
                        new Dependency("2", PolicyEntity.class), Collections.singletonList(new Dependency("4", ListenPortEntity.class))));
        bundle.addEntity(new ListenPortEntity.Builder().name("lp1").id("1").build());
        bundle.addEntity(new ListenPortEntity.Builder().name("lp2").id("2").build());
        bundle.addEntity(new ListenPortEntity.Builder().name("lp3").id("3").build());
        bundle.addEntity(new ListenPortEntity.Builder().name("lp4").id("4").build());

        List<ListenPortEntity> filteredEntities = filter.filter("/my/folder/path", bundle, filteredBundle);

        //TODO: filter out listen ports
        assertEquals(4, filteredEntities.size());
        assertTrue(filteredEntities.stream().anyMatch(c -> "lp2".equals(c.getName())));
        assertTrue(filteredEntities.stream().anyMatch(c -> "lp3".equals(c.getName())));
    }
}