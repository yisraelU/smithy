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
import io.swagger.v3.oas.models.media.XML;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import software.amazon.openapi.smithy.Context;
import software.amazon.openapi.smithy.OpenApi2SmithyTraitMapper;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.XmlAttributeTrait;
import software.amazon.smithy.utils.ListUtils;

public final class XmlAttributeTraitMapper implements OpenApi2SmithyTraitMapper {

    private static final Logger LOGGER = Logger.getLogger(XmlAttributeTraitMapper.class.getName());

    private static final List<ShapeType> XML_ATTRIBUTE_TRAIT_SUPPORTED_SHAPE_TYPES =
            Collections.unmodifiableList(ListUtils.of(ShapeType.BOOLEAN, ShapeType.STRING, ShapeType.TIMESTAMP,
                    ShapeType.BIG_DECIMAL, ShapeType.BIG_INTEGER, ShapeType.BYTE, ShapeType.DOUBLE, ShapeType.FLOAT,
                    ShapeType.INTEGER, ShapeType.LONG, ShapeType.SHORT));

    @Override
    public Shape applyTrait(Shape shape, Object object, Context context) {
        return Optional.ofNullable(object).filter(Schema.class::isInstance).map(Schema.class::cast).map(Schema::getXml)
                .map(XML::getAttribute)
                .map((attr) -> {
                    if (attr) {
                       if (XML_ATTRIBUTE_TRAIT_SUPPORTED_SHAPE_TYPES.contains(shape.getType())) {
                           return new XmlAttributeTrait();
                       } else {
                           LOGGER.warning(String.format(
                                   "The %s trait cannot be applied to %s .",
                                   XmlAttributeTrait.ID, shape.getType()));
                       }
                    }
                    return null;
                }).map(trait -> applyTraitToShape(shape, trait)).orElse(shape);
    }

}
