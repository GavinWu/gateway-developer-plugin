/*
 * Copyright (c) 2018 CA. All rights reserved.
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.ca.apim.gateway.cagatewayconfig.tasks.zip.beans;

import java.util.HashMap;
import java.util.Map;

public class Bundle {
    private final Map<String, Service> services = new HashMap<>();
    private final Map<String, Policy> policies = new HashMap<>();
    private final Map<String, Folder> folders = new HashMap<>();

    public Map<String, Service> getServices() {
        return services;
    }

    public void putAllServices(Map<String, Service> services) {
        this.services.putAll(services);
    }

    public Map<String, Policy> getPolicies() {
        return policies;
    }

    public void putAllPolicies(Map<String, Policy> policies) {
        this.policies.putAll(policies);
    }

    public void putAllFolders(Map<String, Folder> folders) {
        this.folders.putAll(folders);
    }

    public Map<String, Folder> getFolders() {
        return folders;
    }
}