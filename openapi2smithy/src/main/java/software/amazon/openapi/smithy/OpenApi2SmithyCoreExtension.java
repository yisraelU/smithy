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
import software.amazon.openapi.smithy.mappers.RemoveCollectionTrait;
import software.amazon.openapi.smithy.trait.mappers.AuthTraitMapper;
import software.amazon.openapi.smithy.trait.mappers.CollectionTraitMapper;
import software.amazon.openapi.smithy.trait.mappers.DeprecatedTraitMapper;
import software.amazon.openapi.smithy.trait.mappers.DocumentationTraitMapper;
import software.amazon.openapi.smithy.trait.mappers.EnumTraitMapper;
import software.amazon.openapi.smithy.trait.mappers.ErrorTraitMapper;
import software.amazon.openapi.smithy.trait.mappers.ExamplesTraitMapper;
import software.amazon.openapi.smithy.trait.mappers.ExternalDocumentationTraitMapper;
import software.amazon.openapi.smithy.trait.mappers.HttpErrorTraitMapper;
import software.amazon.openapi.smithy.trait.mappers.HttpHeaderTraitMapper;
import software.amazon.openapi.smithy.trait.mappers.HttpLabelTraitMapper;
import software.amazon.openapi.smithy.trait.mappers.HttpPayloadTraitMapper;
import software.amazon.openapi.smithy.trait.mappers.HttpQueryTraitMapper;
import software.amazon.openapi.smithy.trait.mappers.HttpTraitMapper;
import software.amazon.openapi.smithy.trait.mappers.IdempotentTraitMapper;
import software.amazon.openapi.smithy.trait.mappers.LengthTraitMapper;
import software.amazon.openapi.smithy.trait.mappers.PatternTraitMapper;
import software.amazon.openapi.smithy.trait.mappers.ProtocolsTraitMapper;
import software.amazon.openapi.smithy.trait.mappers.RangeTraitMapper;
import software.amazon.openapi.smithy.trait.mappers.ReadonlyTraitMapper;
import software.amazon.openapi.smithy.trait.mappers.RequiredTraitMapper;
import software.amazon.openapi.smithy.trait.mappers.TagsTraitMapper;
import software.amazon.openapi.smithy.trait.mappers.TitleTraitMapper;
import software.amazon.openapi.smithy.trait.mappers.UniqueItemsTraitMapper;
import software.amazon.openapi.smithy.trait.mappers.XmlAttributeTraitMapper;
import software.amazon.openapi.smithy.trait.mappers.XmlFlattenedTraitMapper;
import software.amazon.openapi.smithy.trait.mappers.XmlNameTraitMapper;
import software.amazon.openapi.smithy.trait.mappers.XmlNamespaceTraitMapper;
import software.amazon.smithy.utils.ListUtils;

public class OpenApi2SmithyCoreExtension implements OpenApi2SmithyExtension {

    @Override
    public List<OpenApi2SmithyMapper> getOpenApiMappers() {
        return ListUtils.of(
                new RemoveCollectionTrait()
        );
    }

    @Override
    public List<OpenApi2SmithyTraitMapper> getOpenApi2SmithyTraitMappers() {
        //TODO: Get all classes using service loader which implement OpenApi2SmithyTraitMapper interface
        return ListUtils.of(
                new AuthTraitMapper(),
                new CollectionTraitMapper(),
                new DeprecatedTraitMapper(),
                new DocumentationTraitMapper(),
                new EnumTraitMapper(),
                new ErrorTraitMapper(),
                new ExamplesTraitMapper(),
                new ExternalDocumentationTraitMapper(),
                new HttpErrorTraitMapper(),
                new HttpHeaderTraitMapper(),
                new HttpLabelTraitMapper(),
                new HttpPayloadTraitMapper(),
                new HttpQueryTraitMapper(),
                new HttpTraitMapper(),
                new IdempotentTraitMapper(),
                new LengthTraitMapper(),
                new PatternTraitMapper(),
                new ProtocolsTraitMapper(),
                new RangeTraitMapper(),
                new ReadonlyTraitMapper(),
                new RequiredTraitMapper(),
                new TagsTraitMapper(),
                new TitleTraitMapper(),
                new UniqueItemsTraitMapper(),
                new XmlAttributeTraitMapper(),
                new XmlFlattenedTraitMapper(),
                new XmlNamespaceTraitMapper(),
                new XmlNameTraitMapper()
        );
    }
}
