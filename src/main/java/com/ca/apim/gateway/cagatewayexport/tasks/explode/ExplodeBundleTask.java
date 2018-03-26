/*
 * Copyright (c) 2018 CA. All rights reserved.
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.ca.apim.gateway.cagatewayexport.tasks.explode;

import com.ca.apim.gateway.cagatewayexport.util.file.DocumentFileUtils;
import com.ca.apim.gateway.cagatewayexport.util.json.JsonTools;
import com.ca.apim.gateway.cagatewayexport.util.xml.DocumentParseException;
import com.ca.apim.gateway.cagatewayexport.util.xml.DocumentTools;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;

public class ExplodeBundleTask extends DefaultTask {
    private final DocumentFileUtils documentFileUtils;
    private final JsonTools jsonTools;
    private DocumentTools documentTools;

    private RegularFileProperty inputBundleFile;
    private DirectoryProperty exportDir;

    @Inject
    public ExplodeBundleTask() {
        this(DocumentTools.INSTANCE, DocumentFileUtils.INSTANCE, JsonTools.INSTANCE);
    }

    private ExplodeBundleTask(final DocumentTools documentTools, final DocumentFileUtils documentFileUtils, final JsonTools jsonTools) {
        this.documentTools = documentTools;
        this.documentFileUtils = documentFileUtils;
        this.jsonTools = jsonTools;
        inputBundleFile = newInputFile();
        exportDir = newOutputDirectory();
    }

    @InputFile
    public RegularFileProperty getInputBundleFile() {
        return inputBundleFile;
    }

    @OutputDirectory
    public DirectoryProperty getExportDir() {
        return exportDir;
    }

    @TaskAction
    public void perform() throws DocumentParseException {
        ExplodeBundle explodeBundle = new ExplodeBundle(documentTools, documentFileUtils, jsonTools);
        explodeBundle.explodeBundle(inputBundleFile.getAsFile().get(), exportDir.getAsFile().get());
    }
}
