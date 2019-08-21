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

import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.HttpActionVerbs.GET;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.HttpActionVerbs.POST;

import java.util.Optional;
import software.amazon.openapi.smithy.Context;
import software.amazon.openapi.smithy.OpenApi2SmithyTraitMapper;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.CollectionTrait;
import software.amazon.smithy.model.traits.Trait;

public class CollectionTraitMapper implements OpenApi2SmithyTraitMapper {

    @Override
    public Shape applyTrait(Shape shape, Object object, Context context) {
        return Optional.ofNullable(object)
                .filter(HttpTraitMapperContext.class::isInstance)
                .map(HttpTraitMapperContext.class::cast)
                .map(httpTraitMapperContext -> getCollectionTrait(httpTraitMapperContext, shape))
                .map(trait -> applyTraitToShape(shape, trait))
                .orElse(shape);
    }

    private Trait getCollectionTrait(HttpTraitMapperContext context, Shape shape) {
        boolean collectionTraitFlag = false;
        if (context.getOperationType().equals(GET.toString())) {
            String[] pathLabels = context.getPath().split("/");
            if (!pathLabels[pathLabels.length - 1].contains("{")) {
                collectionTraitFlag = true;
            }
        } else if (context.getOperationType().equals(POST.toString())) {
            OperationShape operationShape = (OperationShape) shape;
            if (Optional.empty().equals(operationShape.getInput())) {
                collectionTraitFlag = true;
            } else {
                if (!context.getPath().contains("{")) {
                    collectionTraitFlag = true;
                }
                //TODO : Check all identifiers of Resource are in Input members
                /*collectionTraitFlag = operationShape.getInput()
                        .map(input -> context.getShapeIndex().getShape(input))
                        .map(inputShape -> {
                            if(inputShape.get() instanceof StructureShape) {
                                StructureShape operationInput = (StructureShape) inputShape.get();
                                String[] pathArr = context.getPath().split("/");
                                ResourceShape resourceShape = (ResourceShape) context.getShapeIndex()
                                .getShape(ShapeId.fromParts("smithy.test","Resource"
                                + pathArr[pathArr.length-1])).get();
                                if (resourceShape.getIdentifiers().keySet()
                                .equals(operationInput.getAllMembers().keySet())) {
                                    return true;
                                }
                            }
                            return true;
                        }).orElse(false);
                */
            }
        }
        return collectionTraitFlag ? new CollectionTrait() : null;
    }

}
