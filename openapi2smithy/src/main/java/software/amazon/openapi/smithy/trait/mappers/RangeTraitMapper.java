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
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import software.amazon.openapi.smithy.Context;
import software.amazon.openapi.smithy.OpenApi2SmithyTraitMapper;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.RangeTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.ListUtils;

public final class RangeTraitMapper implements OpenApi2SmithyTraitMapper {

    private static final Logger LOGGER = Logger.getLogger(RangeTraitMapper.class.getName());

    private static final List<ShapeType> SUPPORTED_RANGE_TRAIT_SHAPE_TYPES =
            Collections.unmodifiableList(ListUtils.of(ShapeType.BYTE, ShapeType.SHORT, ShapeType.INTEGER,
                    ShapeType.LONG, ShapeType.FLOAT, ShapeType.DOUBLE, ShapeType.BIG_DECIMAL, ShapeType.BIG_INTEGER));

    @Override
    public Shape applyTrait(Shape shape, Object object, Context context) {
        return Optional.ofNullable(object).filter(Schema.class::isInstance).map(Schema.class::cast)
                .map(schema -> getRangeTrait(schema, shape))
                .map(trait -> applyTraitToShape(shape, trait))
                .orElse(shape);
    }


    private Trait getRangeTrait(Schema schema, Shape shape) {
        BigDecimal min = schema.getMinimum();
        BigDecimal max = schema.getMaximum();

        if (schema.getExclusiveMinimum() != null || schema.getExclusiveMaximum() != null) {
            LOGGER.warning("The properties exclusiveMinimum and exclusiveMinimum for are not supported"
                    + "All value bounds are inclusive.");
        }

        if (min != null || max != null) {
            RangeTrait.Builder rangeTraitBuilder = RangeTrait.builder();
            if (!SUPPORTED_RANGE_TRAIT_SHAPE_TYPES.contains(shape.getType())) {
                LOGGER.warning(String.format(
                        "The %s trait cannot be applied to %s .",
                        RangeTrait.ID, shape.getType()));
                return null;
            }
            if (min != null) {
                rangeTraitBuilder.min(min);
            }
            if (max != null) {
                rangeTraitBuilder.max(max);
            }
            return rangeTraitBuilder.build();
        }
        return null;
    }
}
