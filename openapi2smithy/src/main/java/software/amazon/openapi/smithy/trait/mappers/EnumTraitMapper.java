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
import software.amazon.openapi.smithy.OpenApi2SmithyTraitMapper;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.EnumConstantBody;
import software.amazon.smithy.model.traits.EnumTrait;

public final class EnumTraitMapper implements OpenApi2SmithyTraitMapper {

    private static final Logger LOGGER = Logger.getLogger(EnumTraitMapper.class.getName());

    @Override
    public Shape applyTrait(Shape shape, Object object, Context context) {
        return Optional.ofNullable(object).filter(Schema.class::isInstance).map(Schema.class::cast)
                .map(Schema::getEnum)
                .map(enumList -> {
                    if (!shape.getType().equals(ShapeType.STRING)) {
                        LOGGER.warning(String.format(
                                "The %s trait cannot be applied to %s .",
                                EnumTrait.ID, shape.getType()));
                        return null;
                    }
                    EnumTrait.Builder builder = EnumTrait.builder();
                    enumList.forEach(enumValue ->
                            builder.addEnum(enumValue.toString(), EnumConstantBody.builder().build()));
                    return builder.build();
                }).map(enumTrait -> applyTraitToShape(shape, enumTrait)).orElse(shape);
    }
}
