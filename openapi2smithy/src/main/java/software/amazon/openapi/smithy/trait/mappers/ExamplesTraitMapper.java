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

import io.swagger.v3.oas.models.Operation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import software.amazon.openapi.smithy.Context;
import software.amazon.openapi.smithy.OpenApi2SmithyTraitMapper;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.ExamplesTrait;
import software.amazon.smithy.model.traits.Trait;

public final class ExamplesTraitMapper implements OpenApi2SmithyTraitMapper {

    private static final Logger LOGGER = Logger.getLogger(ExamplesTraitMapper.class.getName());

    @Override
    public Shape applyTrait(Shape shape, Object object, Context context) {
        return Optional.ofNullable(object).filter(Operation.class::isInstance)
                .map(Operation.class::cast).map(this::getExamplesTrait)
                .map(trait -> applyTraitToShape(shape, trait)).orElse(shape);
    }

    private Trait getExamplesTrait(Operation operation) {
        //TODO: Working on the logic
        ExamplesTrait.Builder exampleTraitBuilder = ExamplesTrait.builder();
        //Examples needs both input and output to be set for the model to serialize
        if (operation.getRequestBody() != null) {
            List<String> contentTypesList = new ArrayList<>(operation.getRequestBody().getContent().keySet());
            contentTypesList.forEach(contentType -> {
                if (operation.getRequestBody().getContent().get(contentType).getExamples() == null
                        || (operation.getResponses().entrySet().stream().anyMatch(response ->
                        Integer.parseInt(response.getKey()) >= 200
                                && Integer.parseInt(response.getKey()) < 300)
                        && operation.getResponses().entrySet().stream().filter(response ->
                        Integer.parseInt(response.getKey()) >= 200 && Integer.parseInt(response.getKey()) < 300)
                        .findFirst().get().getValue().getContent()
                        .get(contentType).getExamples() != null)) {
                    LOGGER.warning("Both Input and Output are needed in examples");
                }
            });
        }

        ExamplesTrait examplesTrait = exampleTraitBuilder.build();
        if (examplesTrait.getExamples() != null && !examplesTrait.getExamples().isEmpty()) {
            return null;
        }
        return null;
    }

}
