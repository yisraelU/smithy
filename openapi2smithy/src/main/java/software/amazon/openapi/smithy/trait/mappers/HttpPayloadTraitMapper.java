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

import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.openapi.smithy.Context;
import software.amazon.openapi.smithy.OpenApi2SmithyTraitMapper;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.ListUtils;

public final class HttpPayloadTraitMapper implements OpenApi2SmithyTraitMapper {

    List<ShapeId> traitList = ListUtils.of(HttpLabelTrait.ID, HttpQueryTrait.ID, HttpHeaderTrait.ID);

    @Override
    public Shape applyTrait(Shape shape, Object object, Context context) {
        return Optional.ofNullable(object).filter(Map.Entry.class::isInstance).map(Map.Entry.class::cast)
                .filter(entry -> entry.getValue() instanceof ApiResponse)
                .map(entry -> getHttpPayloadTrait(entry, shape))
                .orElse(shape);
    }

    private Shape getHttpPayloadTrait(Map.Entry entry, Shape shape) {
        //TODO :  Better to enclose to everything under output shape
        if (!shape.getId().getName().contains("Output")) {
            return shape;
        }
        String responseCode = (String) entry.getKey();
        if (!responseCode.equals("default")) {
            int responseCodeVal = Integer.parseInt(responseCode);
            if (responseCodeVal >= 200 && responseCodeVal < 300) {
                if (shape.isStructureShape()) {
                    StructureShape output = (StructureShape) shape;
                    Map<String, MemberShape> members = output.getAllMembers();
                    StructureShape.Builder builder = output.toBuilder();
                    members.values().forEach(memberShape -> {
                        MemberShape newMemberShape = applyPayloadTrait(memberShape);
                        builder.addMember(newMemberShape);
                    });
                    return builder.build();
                }
            }
        }
        return shape;
    }

    private MemberShape applyPayloadTrait(MemberShape shape) {
        Map<ShapeId, Trait> traitMap = shape.getAllTraits();
        boolean flag = false;
        for (ShapeId shapeId : traitMap.keySet()) {
            if (traitList.contains(shapeId)) {
                flag = true;
                break;
            }
        }
        if (!flag) {
            return (MemberShape) applyTraitToShape(shape, new HttpPayloadTrait());
        }
        return shape;
    }


}
