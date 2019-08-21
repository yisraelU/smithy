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

package software.amazon.openapi.smithy;

import java.util.List;
import software.amazon.smithy.utils.ListUtils;

/**
 * An extension mechanism used to influence how OpenAPI models are converted
 * to Smithy models.
 *
 * <p>Implementations of this interface are discovered through Java SPI.
 */
public interface OpenApi2SmithyExtension {


    /**
     * Registers OpenApi2Smithy mappers, classes used to modify and extend the
     * process of converting a OpenAPI to Smithy Model.
     *
     * @return Returns the mappers to register.
     */
    default List<OpenApi2SmithyMapper> getOpenApiMappers() {
        return ListUtils.of();
    }

    /**
     * Registers OpenApi2Smithy Trait Mappers that are used to apply Traits to Shape
     * based on the OpenApi context.
     *
     * @return Returns the mappers to register.
     */
    default List<OpenApi2SmithyTraitMapper> getOpenApi2SmithyTraitMappers() {
        return ListUtils.of();
    }

}
