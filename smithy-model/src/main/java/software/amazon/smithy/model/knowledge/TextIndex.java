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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.ReferencesTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.validators.TraitValueValidator;

public final class TextIndex implements KnowledgeIndex {
    private List<TextInstance> textInstanceList;

    public TextIndex(Model model) {
        textInstanceList = new ArrayList<>();
        Set<String> visitedNamespaces = new HashSet<>();
        Node validatePreludeNode = model.getMetadata().get(TraitValueValidator.VALIDATE_PRELUDE);
        boolean validatePrelude = validatePreludeNode != null
                ? validatePreludeNode.expectBooleanNode().getValue()
                : false;

        model.shapes().filter(shape -> validatePrelude || !Prelude.isPreludeShape(shape)).forEach(shape -> {
            visitedNamespaces.add(shape.getId().getNamespace());
            getTextInstances(shape, textInstanceList, model);
        });

        for (String namespace : visitedNamespaces) {
            textInstanceList.add(TextInstance.builder()
                    .locationType(TextInstance.TextLocation.NAMESPACE)
                    .text(namespace)
                    .build());
        }

        //no point in allowing the list to change
        textInstanceList = Collections.unmodifiableList(textInstanceList);
    }

    public static TextIndex of(Model model) {
        return model.getKnowledge(TextIndex.class, TextIndex::new);
    }

    public Collection<TextInstance> getTextInstances() {
        return textInstanceList;
    }

    private static void getTextInstances(Shape shape,
                                           Collection<TextInstance> textInstances,
                                           Model model) {
        TextInstance.Builder builder = TextInstance.builder()
                .locationType(TextInstance.TextLocation.SHAPE)
                .shape(shape);
        if (shape.isMemberShape()) {
            builder.text(((MemberShape) shape).getMemberName());
        } else {
            builder.text(shape.getId().getName());
        }
        textInstances.add(builder.build());

        for (Trait trait : shape.getAllTraits().values()) {
            Shape traitShape = model.expectShape(trait.toShapeId());
            getTextInstancesForAppliedTrait(trait.toNode(), trait, shape, textInstances,
                    new ArrayDeque<>(), model, traitShape);
        }
    }

    private static void getTextInstancesForAppliedTrait(Node node,
                                                          Trait trait,
                                                          Shape parentShape,
                                                          Collection<TextInstance> textInstances,
                                                          Deque<TextInstance.PathElement> propertyPath,
                                                          Model model,
                                                          Shape currentTraitPropertyShape) {
        if (trait.toShapeId().equals(ReferencesTrait.ID)) {
            //Skip ReferenceTrait because it is referring to other shape names already being checked
        } else if (node.isStringNode()) {
            textInstances.add(TextInstance.builder()
                    .locationType(TextInstance.TextLocation.APPLIED_TRAIT)
                    .shape(parentShape)
                    .trait(trait)
                    .traitPropertyPath(propertyPath)
                    .text(node.expectStringNode().getValue())
                    .build());
        } else if (node.isObjectNode()) {
            ObjectNode objectNode = node.expectObjectNode();
            objectNode.getMembers().entrySet().forEach(memberEntry -> {
                propertyPath.offerLast(TextInstance.PathElement.ofKey(memberEntry.getKey().getValue()));
                Shape memberTypeShape = getChildMemberShapeType(memberEntry.getKey().getValue(),
                        model, currentTraitPropertyShape);
                if (memberTypeShape == null) {
                    //This means the "property" key value isn't modeled in the trait's structure/shape definition
                    //and this text instance is unique
                    textInstances.add(TextInstance.builder()
                            .locationType(TextInstance.TextLocation.APPLIED_TRAIT)
                            .shape(parentShape)
                            .trait(trait)
                            .text(memberEntry.getKey().getValue())
                            .traitPropertyPath(propertyPath)
                            .build());
                }
                getTextInstancesForAppliedTrait(memberEntry.getValue(), trait, parentShape, textInstances,
                        propertyPath, model, memberTypeShape);
                propertyPath.removeLast();
            });
        } else if (node.isArrayNode()) {
            int index = 0;
            for (Node nodeElement : node.expectArrayNode().getElements()) {
                propertyPath.offerLast(TextInstance.PathElement.ofIndex(index));
                Shape memberTypeShape = getChildMemberShapeType(null,
                        model, currentTraitPropertyShape);
                getTextInstancesForAppliedTrait(nodeElement, trait, parentShape, textInstances,
                        propertyPath, model, memberTypeShape);
                propertyPath.removeLast();
                ++index;
            }
        }
    }

    private static Shape getChildMemberShapeType(String memberKey, Model model, Shape fromShape) {
        if (fromShape != null) {
            for (MemberShape member : fromShape.members()) {
                if (member.getMemberName().equals(memberKey)) {
                    return model.getShape(member.getTarget()).get();
                }
            }
        }
        return null;
    }
}
