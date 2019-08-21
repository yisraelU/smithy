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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.openapi.smithy.Context;
import software.amazon.openapi.smithy.OpenApi2SmithyTraitMapper;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.Trait;

public final class HttpTraitMapper implements OpenApi2SmithyTraitMapper {

    @Override
    public Shape applyTrait(Shape shape, Object object, Context context) {
        return Optional.ofNullable(object).filter(HttpTraitMapperContext.class::isInstance)
                .map(HttpTraitMapperContext.class::cast)
                .map(this::getHttpTrait)
                .map(trait -> applyTraitToShape(shape, trait))
                .orElse(shape);
    }

    private Trait getHttpTrait(HttpTraitMapperContext context) {
        HttpTrait.Builder httpTraitBuilder = HttpTrait.builder();

        httpTraitBuilder.method(context.getOperationType());
        httpTraitBuilder.uri(UriPattern.parse(context.getPath()));

        //In case of multiple success responses fet the first one.
        List<String> successResponses = context.getOperation().getResponses().keySet()
                .stream().filter(code ->
                        !code.equals("default") && Integer.parseInt(code) >= 200 && Integer.parseInt(code) <= 300)
                .collect(Collectors.toList());
        String code;
        if (!successResponses.isEmpty()) {
            if (successResponses.size() > 1) {
                code = successResponses.stream().findFirst().get();
            } else {
                code = successResponses.get(0);
            }
            httpTraitBuilder.code(Integer.parseInt(code));
        }

        return httpTraitBuilder.build();
    }

}
