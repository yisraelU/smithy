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

package software.amazon.openapi.smithy.mappers;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.openapi.smithy.Context;
import software.amazon.openapi.smithy.OpenApi2SmithyMapper;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.CollectionTrait;

/**
 * This class removes collection trait applied to Get Operations that are bind to a resource that has no identifiers.
 */
public class RemoveCollectionTrait implements OpenApi2SmithyMapper {

    @Override
    public byte getOrder() {
        return -128;
    }

    @Override
    public Model after(Model model, Context context) {
        Set<ShapeId> operationShapeIds = model.getShapeIndex().shapes()
                .filter(shape -> shape.getType().equals(ShapeType.RESOURCE))
                .map(ResourceShape.class::cast)
                .map(resourceShape -> {
                    if (!resourceShape.hasIdentifiers()) {
                        Set<ShapeId> operationIds = resourceShape.getAllOperations();
                        return operationIds.stream().map(shapeId -> {
                            OperationShape operationShape =
                                    (OperationShape) model.getShapeIndex().getShape(shapeId).get();
                            if (operationShape.hasTrait(CollectionTrait.ID)) {
                                return shapeId;
                            }
                            return null;
                        }).filter(Objects::nonNull).collect(Collectors.toSet());
                    }
                    return null;
                }).filter(Objects::nonNull).flatMap(Collection::stream).collect(Collectors.toSet());

        List<OperationShape> operationShapeList = operationShapeIds.stream().map(shapeId -> {
            OperationShape operationShape = (OperationShape) model.getShapeIndex().getShape(shapeId).get();
            return operationShape.toBuilder().removeTrait(CollectionTrait.ID).build();

        }).collect(Collectors.toList());

        ShapeIndex.Builder builder = model.getShapeIndex().toBuilder();
        operationShapeList.forEach(builder::addShape);
        return Model.builder().shapeIndex(builder.build()).build();

    }
}
