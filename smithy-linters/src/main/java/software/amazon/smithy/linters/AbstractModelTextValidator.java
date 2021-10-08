/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.linters;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TextIndex;
import software.amazon.smithy.model.knowledge.TextInstance;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * <p>Base class for any validator that wants to perform a full text search on a Model.
 *
 * <p>Does a full scan of the model and gathers all text along with location data
 * and uses a generic function to decide what to do with each text occurrence. The
 * full text search traversal has no knowledge of what it is looking for and descends
 * fully into the structure of all traits.
 *
 * <p>Prelude shape definitions are not examined, however all values
 */
abstract class AbstractModelTextValidator extends AbstractValidator {

    /**
     * Sub-classes must implement this method to perform the following:
     *   1) Decide if the text instance is at a relevant location to validate.
     *   2) Analyze the text for whatever validation event it may or may not publish.
     *   3) Produce a validation event, if necessary, and push it to the ValidationEvent consumer
     *
     * @param occurrence text occurrence found in the body of the model
     * @param validationEventConsumer consumer to push ValidationEvents into
     */
    protected abstract void getValidationEvents(TextInstance occurrence,
                                                Consumer<ValidationEvent> validationEventConsumer);

    /**
     * Runs a full text scan on a given model and stores the resulting TextOccurrences objects.
     *
     * Namespaces are checked against a global set per model.
     *
     * @param model Model to validate.
     * @return a list of ValidationEvents found by the implementer of getValidationEvents per the
     *          TextOccurrences provided by this traversal.
     */
    @Override
    public List<ValidationEvent> validate(Model model) {
        TextIndex textIndex = TextIndex.of(model);

        List<ValidationEvent> validationEvents = new ArrayList<>();
        for (TextInstance text : textIndex.getTextInstances()) {
            getValidationEvents(text, validationEvent -> {
                validationEvents.add(validationEvent);
            });
        }
        return validationEvents;
    }
}
