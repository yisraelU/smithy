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
import software.amazon.smithy.model.shapes.ShapeIndex;

public final class HttpTraitMapperContext {

    private final String path;
    private final Operation operation;
    private final String operationType;

    private final ShapeIndex shapeIndex;

    public HttpTraitMapperContext(String path, Operation operation, String operationType, ShapeIndex shapeIndex) {
        this.path = path;
        this.operation = operation;
        this.operationType = operationType;
        this.shapeIndex = shapeIndex;
    }

    public String getPath() {
        return path;
    }

    public Operation getOperation() {
        return operation;
    }

    public String getOperationType() {
        return operationType;
    }

    public ShapeIndex getShapeIndex() {
        return shapeIndex;
    }


}
