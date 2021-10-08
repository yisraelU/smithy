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

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.knowledge.TextInstance;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.model.validation.ValidatorService;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.StringUtils;

/**
 * <p>Validates that all shape names, and values does not contain non-inclusive terms.
 * *
 * <p>See AbstractModelTextValidator for scan implementation details.
 */
public final class NoninclusiveTermsValidator extends AbstractModelTextValidator {
    static final Map<String, List<String>> BUILT_IN_NONINCLUSIVE_TERMS = MapUtils.of(
            "master", ListUtils.of("primary", "parent", "main"),
            "slave", ListUtils.of("secondary", "replica", "clone", "child"),
            "blacklist", ListUtils.of("denylist"),
            "whitelist", ListUtils.of("allowlist")
        );

    public static final class Provider extends ValidatorService.Provider {
        public Provider() {
            super(NoninclusiveTermsValidator.class, node -> {
                NodeMapper mapper = new NodeMapper();
                return new NoninclusiveTermsValidator(
                        mapper.deserialize(node, NoninclusiveTermsValidator.Config.class));
            });
        }
    }

    /**
     * InclusiveTermsValidator validator configuration.
     */
    public static final class Config {
        private Map<String, List<String>> noninclusiveTerms = MapUtils.of();
        private boolean appendDefaults = false;

        public Map<String, List<String>> getNonInclusiveTerms() {
            return noninclusiveTerms;
        }

        public void setNoninclusiveTerms(Map<String, List<String>> terms) {
            this.noninclusiveTerms = terms;
        }

        public boolean isAppendDefaults() {
            return appendDefaults;
        }

        public void setAppendDefaults(boolean appendDefaults) {
            this.appendDefaults = appendDefaults;
        }
    }

    final Map<String, List<String>> termsMap;

    private NoninclusiveTermsValidator(Config config) {
        if (config.isAppendDefaults()) {
            termsMap = new HashMap<>(BUILT_IN_NONINCLUSIVE_TERMS);
            termsMap.putAll(config.getNonInclusiveTerms());
        } else if (!config.getNonInclusiveTerms().isEmpty()) {
            termsMap = MapUtils.copyOf(config.getNonInclusiveTerms());
        } else {
            termsMap = new HashMap<>(BUILT_IN_NONINCLUSIVE_TERMS);
        }
    }

    @Override
    protected void getValidationEvents(TextInstance instance,
                                       Consumer<ValidationEvent> validationEventConsumer) {
        for (Map.Entry<String, List<String>> termEntry : termsMap.entrySet()) {
            //lower casing the term will be more necessary when the terms are from config
            if (containsTerm(instance.getText(), termEntry.getKey())) {
                switch (instance.getLocationType()) {
                    case NAMESPACE:
                        validationEventConsumer.accept(ValidationEvent.builder()
                                .sourceLocation(SourceLocation.none())
                                .id(this.getClass().getSimpleName().replaceFirst("Validator$", ""))
                                .severity(Severity.WARNING)
                                .message(formatNonInclusiveTermsValidationMessage(termEntry, instance))
                                .build());
                        break;
                    case APPLIED_TRAIT:
                        validationEventConsumer.accept(warning(instance.getShape(),
                                instance.getTrait().getSourceLocation(),
                                formatNonInclusiveTermsValidationMessage(termEntry, instance)));
                        break;
                    case SHAPE:
                    default:
                        validationEventConsumer.accept(warning(instance.getShape(),
                                instance.getShape().getSourceLocation(),
                                formatNonInclusiveTermsValidationMessage(termEntry, instance)));
                }
            }
        }
    }

    private static boolean containsTerm(String text, String term) {
        return text.toLowerCase().contains(term.toLowerCase());
    }

    private static String formatNonInclusiveTermsValidationMessage(Map.Entry<String, List<String>> termEntry,
                                                                   TextInstance instance) {
        String replacementAddendum = termEntry.getValue().size() > 0
                ? String.format(" Consider using one of the following terms instead: %s",
                    ValidationUtils.tickedList(termEntry.getValue()))
                : "";
        switch (instance.getLocationType()) {
            case SHAPE:
                return String.format("%s shape uses a non-inclusive term `%s`.%s",
                        StringUtils.capitalize(instance.getShape().getType().toString()),
                        termEntry.getKey(), replacementAddendum);
            case NAMESPACE:
                return String.format("%s namespace uses a non-inclusive term `%s`.%s",
                        instance.getText(), termEntry.getKey(), replacementAddendum);
            case APPLIED_TRAIT:
                if (instance.getTraitPropertyPath().isEmpty()) {
                    return String.format("'%s' trait has a value that contains a non-inclusive term `%s`.%s",
                            Trait.getIdiomaticTraitName(instance.getTrait()), termEntry.getKey(),
                            replacementAddendum);
                } else {
                    String valuePropertyPathFormatted = formatPropertyPath(instance.getTraitPropertyPath());
                    return String.format("'%s' trait value at path {%s} contains a non-inclusive term `%s`.%s",
                            Trait.getIdiomaticTraitName(instance.getTrait()), valuePropertyPathFormatted,
                            termEntry.getKey(), replacementAddendum);
                }
            default:
                throw new IllegalStateException();
        }
    }

    /**
     * The complexity here is necessary to format property path indexes as parentKey[index] instead
     * of parentKey.[index]. It's subtle, but programmers are quite used to not seeing the dot separator
     * before an index.  It is not possible to conditionalize the delimiter of a string join, so
     * this logic has to be written
     */
    private static String formatPropertyPath(List<TextInstance.PathElement> propertyPath) {
        Objects.requireNonNull(propertyPath);
        Function<TextInstance.PathElement, String> elementToString = pathElement -> {
            switch (pathElement.getElementType()) {
                case KEY:
                    return pathElement.getKey();
                case ARRAY_INDEX:
                    return "[" + pathElement.getIndex() + "]";
                default:
                    throw new IllegalStateException();
            }
        };

        StringBuilder builder = new StringBuilder();
        ListIterator<TextInstance.PathElement> pathElementIterator = propertyPath.listIterator();
        while (pathElementIterator.hasNext()) {
            boolean hasPrevious = pathElementIterator.hasPrevious();
            TextInstance.PathElement pathElement = pathElementIterator.next();
            if (hasPrevious && pathElement.getElementType() != TextInstance.PathElementType.ARRAY_INDEX) {
                builder.append(".");
            }
            builder.append(elementToString.apply(pathElement));
        }

        return builder.toString();
    }
}
