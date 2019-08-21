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
import software.amazon.openapi.smithy.Context;
import software.amazon.openapi.smithy.OpenApi2SmithyTraitMapper;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.DeprecatedTrait;

public final class DeprecatedTraitMapper implements OpenApi2SmithyTraitMapper {

    @Override
    public Shape applyTrait(Shape shape, Object object, Context context) {
        return Optional.ofNullable(object)
                .filter(Schema.class::isInstance)
                .map(Schema.class::cast)
                .map(Schema::getDeprecated)
                .filter(deprecated -> deprecated)
                .map((deprecated) -> DeprecatedTrait.builder().build())
                .map(trait -> applyTraitToShape(shape, trait)).orElse(shape);
    }

}
