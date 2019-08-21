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

package software.amazon.openapi.smithy.trait.mappers;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import software.amazon.openapi.smithy.Context;
import software.amazon.openapi.smithy.OpenApi2SmithyConstants;
import software.amazon.openapi.smithy.OpenApi2SmithyTraitMapper;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.Protocol;
import software.amazon.smithy.model.traits.ProtocolsTrait;
import software.amazon.smithy.model.traits.Trait;

public class ProtocolsTraitMapper implements OpenApi2SmithyTraitMapper {

    private static final Logger LOGGER = Logger.getLogger(ProtocolsTraitMapper.class.getName());

    @Override
    public Shape applyTrait(Shape shape, Object object, Context context) {
        return Optional.ofNullable(object).filter(OpenAPI.class::isInstance).map(OpenAPI.class::cast)
                .map(this::getProtocolsTrait)
                .map(trait -> applyTraitToShape(shape, trait))
                .orElse(shape);
    }

    private Trait getProtocolsTrait(OpenAPI openApi) {
        Map<String, SecurityScheme> securitySchemes = openApi.getComponents().getSecuritySchemes();
        Set<String> serializationFormats = new HashSet<>();
        openApi.getPaths().values().forEach(pathItem -> {
            serializationFormats.addAll(getContentTypesFromOperation(pathItem.getGet()));
            serializationFormats.addAll(getContentTypesFromOperation(pathItem.getPost()));
            serializationFormats.addAll(getContentTypesFromOperation(pathItem.getPut()));
            serializationFormats.addAll(getContentTypesFromOperation(pathItem.getDelete()));
            serializationFormats.addAll(getContentTypesFromOperation(pathItem.getPatch()));
            serializationFormats.addAll(getContentTypesFromOperation(pathItem.getTrace()));
            serializationFormats.addAll(getContentTypesFromOperation(pathItem.getOptions()));
            serializationFormats.addAll(getContentTypesFromOperation(pathItem.getHead()));
        });

        List<Protocol> protocolList = new ArrayList<>();
        serializationFormats.forEach(serializationFormat -> {
            Protocol.Builder protocolBuilder = Protocol.builder().name("rest-" + serializationFormat.split("/")[1]);
            Optional.ofNullable(securitySchemes)
                    .ifPresent(stringSecuritySchemeMap -> stringSecuritySchemeMap.forEach((s, securityScheme) -> {
                SecurityScheme.Type type = securityScheme.getType();
                String scheme = securityScheme.getScheme();
                if (type != null) {
                    String auth;
                    if (OpenApi2SmithyConstants.SUPPORTED_OPENAPI_SECURITY_SCHEMES.contains(securityScheme.getType())) {
                        if (type.equals(SecurityScheme.Type.HTTP)) {
                            auth = OpenApi2SmithyConstants.OPENAPI_SMITHY_AUTH_SCHEMES_MAP.get(type + "_" + scheme);
                        } else {
                            auth = OpenApi2SmithyConstants.OPENAPI_SMITHY_AUTH_SCHEMES_MAP.get(type.toString());
                        }
                        protocolBuilder.addAuth(auth);
                    } else {
                        LOGGER.warning(String.format("The %s security scheme is not supported by default in Smithy ",
                                securityScheme.getType()));
                    }
                }
            }));
            protocolList.add(protocolBuilder.build());
        });
        if (!protocolList.isEmpty()) {
            ProtocolsTrait.Builder protocolTraitBuilder = ProtocolsTrait.builder();
            protocolList.forEach(protocolTraitBuilder::addProtocol);
            return protocolTraitBuilder.build();
        }
        return null;
    }

    private Set<String> getContentTypesFromOperation(Operation operation) {
        Set<String> contentTypeSet = new HashSet<>();

        Optional.ofNullable(operation)
                .map(Operation::getRequestBody)
                .map(RequestBody::getContent)
                .ifPresent(content -> contentTypeSet.addAll(content.keySet()));

        Optional.ofNullable(operation)
                .map(Operation::getResponses)
                .map(LinkedHashMap::values)
                .ifPresent(apiResponses -> apiResponses
                        .forEach(apiResponse -> Optional.ofNullable(apiResponse.getContent())
                                .ifPresent(content -> contentTypeSet.addAll(content.keySet()))));
        return contentTypeSet;
    }

}
