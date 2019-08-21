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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import software.amazon.openapi.smithy.Context;
import software.amazon.openapi.smithy.OpenApi2SmithyTraitMapper;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.ListUtils;

public final class LengthTraitMapper implements OpenApi2SmithyTraitMapper {

    private static final Logger LOGGER = Logger.getLogger(LengthTraitMapper.class.getName());

    private static final List<ShapeType> SUPPORTED_LENGTH_TRAIT_SHAPE_TYPES =
            Collections.unmodifiableList(ListUtils.of(ShapeType.LIST, ShapeType.MAP, ShapeType.STRING, ShapeType.BLOB));

    @Override
    public Shape applyTrait(Shape shape, Object object, Context context) {
        return Optional.ofNullable(object).filter(Schema.class::isInstance).map(Schema.class::cast)
                .map(schema -> getLengthTrait(schema, shape))
                .map(trait -> applyTraitToShape(shape, trait)).orElse(shape);
    }

    private Trait getLengthTrait(Schema schema, Shape shape) {
        Integer minLength = schema.getMinLength();
        Integer maxLength = schema.getMaxLength();

        Integer minItems = schema.getMinItems();
        Integer maxItems = schema.getMaxItems();

        Integer min = minLength != null ? minLength : minItems;
        Integer max = maxLength != null ? maxLength : maxItems;

        if (min != null || max != null) {
            LengthTrait.Builder lengthTraitBuilder = LengthTrait.builder();
            if (!SUPPORTED_LENGTH_TRAIT_SHAPE_TYPES.contains(shape.getType())) {
                LOGGER.warning(String.format(
                        "The %s trait cannot be applied to %s .",
                        LengthTrait.ID, shape));
                return null;
            }
            if (min != null) {
                lengthTraitBuilder.min(Long.valueOf(min));
            }
            if (max != null) {
                lengthTraitBuilder.max(Long.valueOf(max));
            }
            return lengthTraitBuilder.build();
        }
        return null;
    }

}
