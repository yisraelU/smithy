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

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeIndex;

/**
 * Provides a plugin infrastructure used to hook into the OpenAPI to Smithy
 * conversion process and map over the result.
 *
 * <p>The methods of a plugin are invoked by {@link OpenApi2SmithyConverter} during
 * the conversion of a model. There is no need to invoke these manually.
 * Implementations may choose to leverage configuration options of the
 * provided context to determine whether or not to enact the plugin.
 */
public interface OpenApi2SmithyMapper {

    /**
     * Gets the sort order of the plugin from -128 to 127.
     *
     * <p>Plugins are applied according to this sort order. Lower values
     * are executed before higher values (for example, -128 comes before 0,
     * comes before 127). Plugins default to 0, which is the middle point
     * between the minimum and maximum order values.
     *
     * @return Returns the sort order, defaulting to 0.
     */
    default byte getOrder() {
        return 0;
    }


    default Shape updateShape(Shape shape, Schema schema, Context context) {
        return shape;
    }

    default Shape updateOperationShape(Shape operationShape, Operation operation, Context context) {
        return operationShape;
    }

    default Shape updateServiceShape(Shape serviceShape, OpenAPI openApi, Context context) {
        return serviceShape;
    }

    default Shape updateResourceShape(Shape resourceShape, OpenAPI openApi, Context context) {
        return resourceShape;
    }

    default void before(ShapeIndex.Builder builder, Context context) {
    }

    default Model after(Model model, Context context) {
        return model;
    }

    static OpenApi2SmithyMapper compose(List<OpenApi2SmithyMapper> mappers) {
        List<OpenApi2SmithyMapper> sorted = new ArrayList<>(mappers);
        sorted.sort(Comparator.comparingInt(OpenApi2SmithyMapper::getOrder));

        return new OpenApi2SmithyMapper() {
            @Override
            public Shape updateShape(Shape shape, Schema schema, Context context) {
                for (OpenApi2SmithyMapper plugin : sorted) {
                    if (shape == null) {
                        return null;
                    }
                    shape = plugin.updateShape(shape, schema, context);
                }
                return shape;
            }

            @Override
            public Shape updateOperationShape(Shape operationShape, Operation operation, Context context) {
                for (OpenApi2SmithyMapper plugin : sorted) {
                    if (operationShape == null) {
                        return null;
                    }
                    operationShape = plugin.updateOperationShape(operationShape, operation, context);
                }
                return operationShape;
            }

            @Override
            public Shape updateServiceShape(Shape serviceShape, OpenAPI openApi, Context context) {
                for (OpenApi2SmithyMapper plugin : sorted) {
                    if (serviceShape == null) {
                        return null;
                    }
                    serviceShape = plugin.updateServiceShape(serviceShape, openApi, context);
                }
                return serviceShape;
            }

            @Override
            public Shape updateResourceShape(Shape resourceShape, OpenAPI openApi, Context context) {
                for (OpenApi2SmithyMapper plugin : sorted) {
                    if (resourceShape == null) {
                        return null;
                    }
                    resourceShape = plugin.updateResourceShape(resourceShape, openApi, context);
                }
                return resourceShape;
            }

            @Override
            public void before(ShapeIndex.Builder builder, Context context) {
                for (OpenApi2SmithyMapper plugin : sorted) {
                    plugin.before(builder, context);
                }
            }

            @Override
            public Model after(Model model, Context context) {
                for (OpenApi2SmithyMapper plugin : sorted) {
                    model = plugin.after(model, context);
                }
                return model;
            }
        };
    }

}
