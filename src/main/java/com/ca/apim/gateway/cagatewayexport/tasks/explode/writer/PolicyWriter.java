/*
 * Copyright (c) 2018 CA. All rights reserved.
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.ca.apim.gateway.cagatewayexport.tasks.explode.writer;

import com.ca.apim.gateway.cagatewayexport.tasks.explode.bundle.Bundle;
import com.ca.apim.gateway.cagatewayexport.tasks.explode.bundle.entity.EncassEntity;
import com.ca.apim.gateway.cagatewayexport.tasks.explode.bundle.entity.Folder;
import com.ca.apim.gateway.cagatewayexport.tasks.explode.bundle.entity.PolicyEntity;
import com.ca.apim.gateway.cagatewayexport.tasks.explode.bundle.entity.ServiceEntity;
import com.ca.apim.gateway.cagatewayexport.tasks.explode.bundle.entity.loader.EntityLoaderHelper;
import com.ca.apim.gateway.cagatewayexport.util.file.DocumentFileUtils;
import com.ca.apim.gateway.cagatewayexport.util.xml.DocumentParseException;
import com.ca.apim.gateway.cagatewayexport.util.xml.DocumentTools;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PolicyWriter implements EntityWriter {
    private static final Logger LOGGER = Logger.getLogger(PolicyWriter.class.getName());

    private final DocumentFileUtils documentFileUtils;
    private final DocumentTools documentTools;

    public PolicyWriter(DocumentFileUtils documentFileUtils, DocumentTools documentTools) {
        this.documentFileUtils = documentFileUtils;
        this.documentTools = documentTools;
    }

    @Override
    public void write(Bundle bundle, File rootFolder) {
        File policyFolder = new File(rootFolder, "policy");
        documentFileUtils.createFolder(policyFolder.toPath());

        //create folders
        bundle.getFolderTree().stream().forEach(folder -> {
            if (folder.getParentFolderId() != null) {
                Path folderFile = policyFolder.toPath().resolve(bundle.getFolderTree().getPath(folder));
                documentFileUtils.createFolder(folderFile);
            }
        });

        //create policies
        Map<String, ServiceEntity> services = bundle.getEntities(ServiceEntity.class);
        services.values().parallelStream().forEach(serviceEntity -> writePolicy(bundle, policyFolder, serviceEntity.getFolderId(), serviceEntity.getName(), serviceEntity.getPolicy()));

        Map<String, PolicyEntity> policies = bundle.getEntities(PolicyEntity.class);
        policies.values().parallelStream().forEach(policyEntity -> writePolicy(bundle, policyFolder, policyEntity.getFolderId(), policyEntity.getName(), policyEntity.getPolicy()));

    }

    private void writePolicy(Bundle bundle, File policyFolder, String folderId, String name, String policy) {
        Folder folder = bundle.getFolderTree().getFolderById(folderId);
        Path folderPath = policyFolder.toPath().resolve(bundle.getFolderTree().getPath(folder));

        Path policyPath = folderPath.resolve(name + ".xml");
        try {
            documentFileUtils.createFile(simplifyPolicyXML(WriterHelper.stringToXML(documentTools, policy), bundle), policyPath, false);
        } catch (DocumentParseException e) {
            throw new WriteException("Exception writing policy: " + policyPath + " Message: " + e.getMessage(), e);
        }
    }

    private Element simplifyPolicyXML(Element policyElement, Bundle bundle) {
        findAndSimplifyAssertion(policyElement, "L7p:Include", (element) -> simplifyIncludeAssertion(bundle, element));
        findAndSimplifyAssertion(policyElement, "L7p:Encapsulated", (element) -> simplifyEncapsulatedAssertion(bundle, element));
        findAndSimplifyAssertion(policyElement, "L7p:SetVariable", this::simplifySetVariable);
        findAndSimplifyAssertion(policyElement, "L7p:HardcodedResponse", this::simplifyHardcodedResponse);
        return policyElement;
    }

    private void simplifyHardcodedResponse(Element element) {
        Element base64ResponseBodyElement = EntityLoaderHelper.getSingleElement(element, "L7p:Base64ResponseBody");
        String base64Expression = base64ResponseBodyElement.getAttribute("stringValue");
        byte[] decoded = Base64.getDecoder().decode(base64Expression);

        Element expressionElement = element.getOwnerDocument().createElement("L7p:ResponseBody");
        expressionElement.appendChild(element.getOwnerDocument().createCDATASection(new String(decoded)));
        element.insertBefore(expressionElement, base64ResponseBodyElement);
        element.removeChild(base64ResponseBodyElement);
    }

    private void simplifySetVariable(Element element) {
        Element base64ExpressionElement = EntityLoaderHelper.getSingleElement(element, "L7p:Base64Expression");
        String base64Expression = base64ExpressionElement.getAttribute("stringValue");
        byte[] decoded = Base64.getDecoder().decode(base64Expression);

        Element expressionElement = element.getOwnerDocument().createElement("L7p:Expression");
        expressionElement.appendChild(element.getOwnerDocument().createCDATASection(new String(decoded)));
        element.insertBefore(expressionElement, base64ExpressionElement);
        element.removeChild(base64ExpressionElement);
    }

    private void simplifyEncapsulatedAssertion(Bundle bundle, Element encapsulatedAssertionElement) {
        Element encassGuidElement = EntityLoaderHelper.getSingleElement(encapsulatedAssertionElement, "L7p:EncapsulatedAssertionConfigGuid");
        String encassGuid = encassGuidElement.getAttribute("stringValue");
        Optional<EncassEntity> encassEntity = bundle.getEntities(EncassEntity.class).values().stream().filter(e -> encassGuid.equals(e.getGuid())).findAny();
        if (encassEntity.isPresent()) {
            PolicyEntity policyEntity = bundle.getEntities(PolicyEntity.class).get(encassEntity.get().getPolicyId());
            if (policyEntity != null) {
                encapsulatedAssertionElement.setAttribute("policyPath", getPolicyPath(bundle, policyEntity));
                Element encapsulatedAssertionConfigNameElement = EntityLoaderHelper.getSingleElement(encapsulatedAssertionElement, "L7p:EncapsulatedAssertionConfigName");
                encapsulatedAssertionElement.removeChild(encapsulatedAssertionConfigNameElement);
                encapsulatedAssertionElement.removeChild(encassGuidElement);
            } else {
                LOGGER.log(Level.WARNING, "Could not find referenced encass policy with id: %s", encassEntity.get().getPolicyId());
            }
        } else {
            LOGGER.log(Level.WARNING, "Could not find referenced encass with guid: %s", encassGuid);
        }
    }

    private void simplifyIncludeAssertion(Bundle bundle, Element assertionElement) {
        Element policyGuidElement = EntityLoaderHelper.getSingleElement(assertionElement, "L7p:PolicyGuid");
        String includedPolicyGuid = policyGuidElement.getAttribute("stringValue");
        Optional<PolicyEntity> policyEntity = bundle.getEntities(PolicyEntity.class).values().stream().filter(p -> includedPolicyGuid.equals(p.getGuid())).findAny();
        if (policyEntity.isPresent()) {
            policyGuidElement.setAttribute("policyPath", getPolicyPath(bundle, policyEntity.get()));
            policyGuidElement.removeAttribute("stringValue");
        } else {
            LOGGER.log(Level.WARNING, "Could not find referenced policy include with guid: %s", includedPolicyGuid);
        }
    }

    private void findAndSimplifyAssertion(Element policyElement, String assertionTagName, Consumer<Element> simplifier) {
        NodeList includeReferences = policyElement.getElementsByTagName(assertionTagName);
        for (int i = 0; i < includeReferences.getLength(); i++) {
            Node includeElement = includeReferences.item(i);
            if (!(includeElement instanceof Element)) {
                throw new WriteException("Unexpected Assertion node type: " + includeElement.getNodeType());
            }
            simplifier.accept((Element) includeElement);
        }
    }

    private String getPolicyPath(Bundle bundle, PolicyEntity policyEntity) {
        Folder folder = bundle.getFolderTree().getFolderById(policyEntity.getFolderId());
        Path folderPath = bundle.getFolderTree().getPath(folder);
        return Paths.get(folderPath.toString(), policyEntity.getName() + ".xml").toString();
    }
}
