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

import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.OPENAPI_TYPE_ARRAY;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.UNIQUE_ITEMS_AS_LIST;

import io.swagger.v3.oas.models.media.Schema;
import java.util.Optional;
import java.util.logging.Logger;
import software.amazon.openapi.smithy.Context;
import software.amazon.openapi.smithy.OpenApi2SmithyTraitMapper;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.UniqueItemsTrait;

public final class UniqueItemsTraitMapper implements OpenApi2SmithyTraitMapper {

    private static final Logger LOGGER = Logger.getLogger(UniqueItemsTraitMapper.class.getName());

    @Override
    public Shape applyTrait(Shape shape, Object object, Context context) {
        //TODO: get the UniqueItemsAsList property from config
        boolean uniqueItemsAsList =
                context.getSmithyModelGenerator().getConfig().getBooleanMemberOrDefault(UNIQUE_ITEMS_AS_LIST,
                        false);
        return Optional.ofNullable(object).filter(Schema.class::isInstance).map(Schema.class::cast)
                .map(schema -> getUniqueItemsTrait(schema, shape, uniqueItemsAsList))
                .map(trait -> applyTraitToShape(shape, trait))
                .orElse(shape);
    }


    private Trait getUniqueItemsTrait(Schema schema, Shape shape, boolean uniqueItemsAsList) {
        boolean uniqueItems = schema.getUniqueItems() == null ? false : schema.getUniqueItems();
        String type = schema.getType() == null ? "" : schema.getType();
        if (type.equals(OPENAPI_TYPE_ARRAY) && shape.getType().equals(ShapeType.LIST)
                && uniqueItemsAsList && uniqueItems) {
            return  new UniqueItemsTrait();
        }
        return null;
    }
}
