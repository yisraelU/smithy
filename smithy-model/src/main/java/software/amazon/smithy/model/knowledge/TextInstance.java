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

package software.amazon.smithy.model.knowledge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.ListUtils;

public final class TextInstance {
    public enum TextLocation {
        SHAPE,
        APPLIED_TRAIT,
        NAMESPACE
    }

    public enum PathElementType {
        KEY,
        ARRAY_INDEX
    }

    public static final class PathElement {
        private final PathElementType elementType;
        private final String key;
        private final int index;

        private PathElement(String key) {
            elementType = PathElementType.KEY;
            this.key = key;
            this.index = 0;
        }

        private PathElement(int index) {
            elementType = PathElementType.ARRAY_INDEX;
            this.key = null;
            this.index = index;
        }

        public static PathElement ofKey(String key) {
            return new PathElement(key);
        }

        public static  PathElement ofIndex(int index) {
            return new PathElement(index);
        }

        public PathElementType getElementType() {
            return elementType;
        }

        public String getKey() {
            return key;
        }

        public int getIndex() {
            return index;
        }
    }

    private final TextLocation locationType;
    private final String text;
    private final Shape shape;
    private final Trait trait;
    private final List<PathElement> traitPropertyPath;

    private TextInstance(Builder builder) {
        Objects.requireNonNull(builder.locationType, "LocationType must be specified");
        if (builder.locationType != TextLocation.NAMESPACE && builder.shape == null) {
            throw new IllegalStateException("Shape must be specified if locationType is not namespace");
        }
        Objects.requireNonNull(builder.text, "Text must be specified");
        if (builder.locationType == TextLocation.APPLIED_TRAIT) {
            if (builder.trait == null) {
                throw new IllegalStateException("Trait must be specified for locationType="
                        + builder.locationType.name());
            } else if (builder.traitPropertyPath == null) {
                throw new IllegalStateException("PropertyPath must be specified for locationType="
                        + builder.locationType.name());
            }
        }

        this.locationType = builder.locationType;
        this.text = builder.text;
        this.shape = builder.shape;
        this.trait = builder.trait;
        this.traitPropertyPath = builder.traitPropertyPath;
    }

    public TextLocation getLocationType() {
        return locationType;
    }

    public String getText() {
        return text;
    }

    public Shape getShape() {
        return shape;
    }

    public Trait getTrait() {
        return trait;
    }

    public List<PathElement> getTraitPropertyPath() {
        return traitPropertyPath;
    }

    public static Builder builder() {
        return new Builder();
    }

    static final class Builder {
        private TextLocation locationType;
        private String text;
        private Shape shape;
        private Trait trait;
        private List<PathElement> traitPropertyPath = new ArrayList<>();

        private Builder() { }

        public Builder shape(Shape shape) {
            this.shape = shape;
            return this;
        }

        public Builder trait(Trait trait) {
            this.trait = trait;
            return this;
        }

        public Builder traitPropertyPath(Deque<PathElement> traitPropertyPath) {
            this.traitPropertyPath = traitPropertyPath != null
                    ? traitPropertyPath.stream().collect(ListUtils.toUnmodifiableList())
                    : Collections.emptyList();
            return this;
        }

        public Builder locationType(TextLocation locationType) {
            this.locationType = locationType;
            return this;
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public TextInstance build() {
            return new TextInstance(this);
        }
    }
}
