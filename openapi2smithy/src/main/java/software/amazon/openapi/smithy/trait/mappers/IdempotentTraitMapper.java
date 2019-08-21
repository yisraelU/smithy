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

import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.HttpActionVerbs.DELETE;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.HttpActionVerbs.POST;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.HttpActionVerbs.PUT;

import java.util.List;
import java.util.Optional;
import software.amazon.openapi.smithy.Context;
import software.amazon.openapi.smithy.OpenApi2SmithyTraitMapper;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.IdempotentTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.ListUtils;

public class IdempotentTraitMapper implements OpenApi2SmithyTraitMapper {

    List<String> idempotentMethodsList = ListUtils.of(POST.toString(), PUT.toString(), DELETE.toString());

    @Override
    public Shape applyTrait(Shape shape, Object object, Context context) {
        return Optional.ofNullable(object).filter(HttpTraitMapperContext.class::isInstance)
                .map(HttpTraitMapperContext.class::cast)
                .map(HttpTraitMapperContext::getOperationType)
                .map(this::getIdempotentTrait)
                .map(trait -> applyTraitToShape(shape, trait))
                .orElse(shape);
    }

    private Trait getIdempotentTrait(String operationType) {
        /*if (operationType.equals(POST.toString())) {
            return new IdempotentTrait();
        }*/
        if (idempotentMethodsList.contains(operationType)) {
            return new IdempotentTrait();
        }
        return null;
    }
}
