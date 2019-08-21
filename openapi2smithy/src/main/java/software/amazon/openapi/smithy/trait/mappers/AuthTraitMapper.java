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

import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.API_LEVEL_SECURITY_SCHEMES;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.OPENAPI_SMITHY_AUTH_SCHEMES_MAP;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.SUPPORTED_OPENAPI_SECURITY_SCHEMES;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.SUPPORTED_SECURITY_SCHEMES;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import software.amazon.openapi.smithy.Context;
import software.amazon.openapi.smithy.OpenApi2SmithyTraitMapper;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.AuthTrait;
import software.amazon.smithy.model.traits.Trait;

public class AuthTraitMapper implements OpenApi2SmithyTraitMapper {

    private static final Logger LOGGER = Logger.getLogger(AuthTraitMapper.class.getName());

    @Override
    public Shape applyTrait(Shape shape, Object object, Context context) {
        return Optional.ofNullable(object)
                .filter(Operation.class::isInstance)
                .map(Operation.class::cast)
                .map(operation -> getAuthTrait(operation, context))
                .map(trait -> applyTraitToShape(shape, trait))
                .orElse(shape);
    }

    private Trait getAuthTrait(Operation operation, Context context) {
        Map<String, Map<String, String>> securitySchemesMap = getSecuritySchemes(context);
        List<SecurityRequirement> securityRequirementsList = operation.getSecurity();
        AuthTrait.Builder authTraitBuilder = AuthTrait.builder();

        if (securityRequirementsList != null) {
            //Operation level security overrides API level security.
            Map<String, String> supportedSecuritySchemes = securitySchemesMap.get(SUPPORTED_SECURITY_SCHEMES);
            Optional.ofNullable(supportedSecuritySchemes).ifPresent(supportedSchemes -> securityRequirementsList
                    .forEach(securityRequirement -> securityRequirement.keySet()
                            .forEach(securitySchemeName -> {
                                if (supportedSchemes.containsKey(securitySchemeName)) {
                                    authTraitBuilder.addValue(supportedSchemes.get(securitySchemeName));
                                } else {
                                    LOGGER.warning(String.format("The %s security scheme does not exist in "
                                            + "Security Schemes defined in Components", securitySchemeName));
                                }
                            })));
        } else {
            //If Operation level security is not present apply API level security if present
            Map<String, String> apiLevelSecuritySchemes = securitySchemesMap.get(API_LEVEL_SECURITY_SCHEMES);
            if (apiLevelSecuritySchemes != null) {
                apiLevelSecuritySchemes.values().forEach(authTraitBuilder::addValue);
            }
        }
        if (!authTraitBuilder.build().getValues().isEmpty()) {
            return authTraitBuilder.build();
        }
        return null;
    }

    private Map<String, Map<String, String>> getSecuritySchemes(Context context) {
        Map<String, SecurityScheme> securitySchemes = context.getOpenApi().getComponents().getSecuritySchemes();
        List<SecurityRequirement> apiSecurityList = context.getOpenApi().getSecurity();
        Map<String, Map<String, String>> securitySchemesMap = new HashMap<>();
        Map<String, String> supportedSecuritySchemeMap = new HashMap<>();
        if (securitySchemes != null) {
            securitySchemes.forEach((name, securityScheme) -> {
                SecurityScheme.Type type = securityScheme.getType();
                String scheme = securityScheme.getScheme();
                if (type != null) {
                    String auth;
                    if (SUPPORTED_OPENAPI_SECURITY_SCHEMES.contains(securityScheme.getType())) {
                        if (type.equals(SecurityScheme.Type.HTTP)) {
                            auth = OPENAPI_SMITHY_AUTH_SCHEMES_MAP.get(type + "_" + scheme);
                        } else {
                            auth = OPENAPI_SMITHY_AUTH_SCHEMES_MAP.get(type.toString());
                        }
                        supportedSecuritySchemeMap.put(name, auth);
                    }
                }
            });
        }
        securitySchemesMap.put(SUPPORTED_SECURITY_SCHEMES, supportedSecuritySchemeMap);
        Map<String, String> apiLevelSecuritySchemeMap = new HashMap<>();
        //TODO: Use Optional
        if (apiSecurityList != null) {
            apiSecurityList.forEach(apiSecurity -> apiSecurity.forEach((key, value) -> {
                if (supportedSecuritySchemeMap.containsKey(key)) {
                    apiLevelSecuritySchemeMap.put(key, supportedSecuritySchemeMap.get(key));
                }
            }));
        }
        securitySchemesMap.put(API_LEVEL_SECURITY_SCHEMES, apiLevelSecuritySchemeMap);
        return securitySchemesMap;
    }



}
