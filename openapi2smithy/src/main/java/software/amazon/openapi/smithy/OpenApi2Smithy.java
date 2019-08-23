/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.openapi.smithy;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildException;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ModelSerializer;

public class OpenApi2Smithy implements SmithyBuildPlugin {

    @Override
    public String getName() {
        return "openapi2smithy";
    }

    @Override
    public void execute(PluginContext context) {
        OpenApi2SmithyConverter converter = OpenApi2SmithyConverter.create();
        context.getSettings().getStringMap().forEach(converter::putSetting);

        updateFileManifest(context);

        String openApiServiceFileName =
                context.getSettings().getStringMemberOrDefault("openapi.filename", "");
        Path openApiServiceFilePath = null;
        if (context.getFileManifest().hasFile(openApiServiceFileName)) {
            openApiServiceFilePath =
                    context.getFileManifest().getFiles()
                            .stream()
                            .filter(
                                    file -> file.toString().contains(openApiServiceFileName))
                            .collect(Collectors.toList()
                    ).get(0);
        }
        if (openApiServiceFilePath == null) {
            throw new RuntimeException("Invalid path for service file"); //TODO:
        }
        OpenAPI openApi = new OpenAPIV3Parser().read(openApiServiceFilePath.toAbsolutePath().toString());
        Model model = converter.convert(openApi);

        context.getFileManifest().writeJson(context.getSettings().getStringMap().get("service.name") + ".json",
                ModelSerializer.builder().build().serialize(model));
    }

    private void updateFileManifest(PluginContext context) {
        context.getSettings()
                .getStringMember("openapi.file.location")
                .map(StringNode::getValue)
                .map(Paths::get)
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .ifPresent(path -> {
                    if (Files.isDirectory(path)) {
                        copyDirectoryToManifest(path, context.getFileManifest());
                    } else {
                        throw new SmithyBuildException(String.format(
                                "The provided opeapi.file.location property "
                                        + "in the %s projection is not a directory: %s",
                                context.getProjectionName(),
                                path));
                    }
                });
    }

    private static void copyDirectoryToManifest(Path src, FileManifest manifest) {
        try {
            List<Path> paths = Files.walk(src)
                    .filter(path -> !Files.isDirectory(path))
                    .collect(Collectors.toList());
            for (Path path : paths) {
                Path dest = src.relativize(path);
                manifest.writeFile(dest, Files.newInputStream(path));
            }
        } catch (IOException e) {
            throw new SmithyBuildException(e);
        }
    }
}
