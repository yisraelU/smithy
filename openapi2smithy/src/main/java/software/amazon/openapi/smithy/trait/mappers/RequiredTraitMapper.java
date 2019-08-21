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

import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import software.amazon.openapi.smithy.Context;
import software.amazon.openapi.smithy.OpenApi2SmithyTraitMapper;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.Trait;

public final class RequiredTraitMapper implements OpenApi2SmithyTraitMapper {

    private static final Logger LOGGER = Logger.getLogger(RequiredTraitMapper.class.getName());

    @Override
    public Shape applyTrait(Shape shape, Object object, Context context) {
        return Optional.ofNullable(object)
                .map(obj -> getRequiredTrait(obj, shape))
                .map(trait -> applyTraitToShape(shape, trait))
                .orElse(shape);
    }

    private static Trait getRequiredTrait(Object object, Shape shape) {
        boolean requiredFlag = false;
        if (object instanceof Schema) {
            requiredFlag = Optional.of(object)
                    .map(Schema.class::cast)
                    .map(Schema::getRequired)
                    .map(required -> required.contains(shape.getId().getName()))
                    .orElse(false);
        } else if (object instanceof Parameter) {
            requiredFlag = Optional.of(object).map(Parameter.class::cast)
                    .map(Parameter::getRequired)
                    .filter(req -> req)
                    .orElse(false);
        } else if (object instanceof Map.Entry) {
            requiredFlag = Optional.of(object)
                    .map(Map.Entry.class::cast)
                    .map(Map.Entry::getValue)
                    .filter(Header.class::isInstance)
                    .map(Header.class::cast)
                    .map(Header::getRequired)
                    .filter(req -> req)
                    .orElse(false);
        }
        return requiredFlag ? new RequiredTrait() : null;
    }
}
