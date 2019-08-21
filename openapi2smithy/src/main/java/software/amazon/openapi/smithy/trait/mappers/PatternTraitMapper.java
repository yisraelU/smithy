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

import io.swagger.v3.oas.models.media.Schema;
import java.util.Optional;
import java.util.logging.Logger;
import software.amazon.openapi.smithy.Context;
import software.amazon.openapi.smithy.OpenApi2SmithyConstants;
import software.amazon.openapi.smithy.OpenApi2SmithyTraitMapper;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.PatternTrait;
import software.amazon.smithy.model.traits.Trait;

public final class PatternTraitMapper implements OpenApi2SmithyTraitMapper {

    private static final Logger LOGGER = Logger.getLogger(PatternTraitMapper.class.getName());

    @Override
    public Shape applyTrait(Shape shape, Object object, Context context) {
        return Optional.ofNullable(object).filter(Schema.class::isInstance).map(Schema.class::cast)
                .map(schema -> getPatternTrait(schema, shape))
                .map(trait -> applyTraitToShape(shape, trait))
                .orElse(shape);
    }

    private Trait getPatternTrait(Schema schema, Shape shape) {
        if (schema.getPattern() != null) {
            return new PatternTrait(schema.getPattern());
        }
        String patternValue = "";
        String type = schema.getType() == null ? "" : schema.getType();
        if (type.equals(OpenApi2SmithyConstants.OPENAPI_TYPE_STRING)) {
            String format = schema.getFormat() == null ? "" : schema.getFormat();
            //TODO: Add patterns for email, ipv6 uuid, uri etc
            switch (format) {
                case "ipv4":
                    patternValue = "^(([01]?\\d\\d?|2[0-4]\\\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
                    break;
                case "email":
                    LOGGER.warning("Email patterns is currently not supported.");
                    break;
                case "ipv6":
                    LOGGER.warning("IPV6 patterns is currently not supported");
                    break;
                default:
                    patternValue = "";
                    break;
            }
        }
        if (!patternValue.isEmpty()) {
            if (!shape.getType().equals(ShapeType.STRING)) {
                LOGGER.warning(String.format(
                        "The %s trait cannot be applied to %s .",
                        PatternTrait.ID, shape.getType()));
                return null;
            }
            return new PatternTrait(patternValue);
        }
        return null;
    }
}
