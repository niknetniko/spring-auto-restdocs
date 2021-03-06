/*-
 * #%L
 * Spring Auto REST Docs Core
 * %%
 * Copyright (C) 2015 - 2019 Scalable Capital GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package capital.scalable.restdocs.payload;

import static capital.scalable.restdocs.SnippetRegistry.AUTO_RESPONSE_FIELDS;
import static capital.scalable.restdocs.payload.TableWithPrefixMatcher.tableWithPrefix;
import static capital.scalable.restdocs.util.FormatUtil.fixLineSeparator;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import capital.scalable.restdocs.constraints.ConstraintReader;
import capital.scalable.restdocs.javadoc.JavadocReader;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.core.EmbeddedWrapper;
import org.springframework.hateoas.core.EmbeddedWrappers;
import org.springframework.http.ResponseEntity;
import org.springframework.restdocs.AbstractSnippetTests;
import org.springframework.restdocs.snippet.SnippetException;
import org.springframework.restdocs.templates.TemplateFormat;
import org.springframework.web.method.HandlerMethod;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class JacksonResponseFieldSnippetTest extends AbstractSnippetTests {

    private ObjectMapper mapper;
    private JavadocReader javadocReader;
    private ConstraintReader constraintReader;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    public JacksonResponseFieldSnippetTest(String name, TemplateFormat templateFormat) {
        super(name, templateFormat);
    }

    @Before
    public void setup() {
        mapper = new ObjectMapper();
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.ANY));
        javadocReader = mock(JavadocReader.class);
        constraintReader = mock(ConstraintReader.class);
    }

    @Test
    public void simpleResponse() throws Exception {
        HandlerMethod handlerMethod = createHandlerMethod("getItem");
        mockFieldComment(Item.class, "field1", "A string");
        mockFieldComment(Item.class, "field2", "A decimal");
        mockOptionalMessage(Item.class, "field1", "false");
        mockConstraintMessage(Item.class, "field2", "A constraint");

        this.snippets.expect(AUTO_RESPONSE_FIELDS).withContents(
                tableWithHeader("Path", "Type", "Optional", "Description")
                        .row("field1", "String", "false", "A string.")
                        .row("field2", "Decimal", "true",
                                "A decimal.\n\nA constraint."));

        new JacksonResponseFieldSnippet().document(operationBuilder
                .attribute(HandlerMethod.class.getName(), handlerMethod)
                .attribute(ObjectMapper.class.getName(), mapper)
                .attribute(JavadocReader.class.getName(), javadocReader)
                .attribute(ConstraintReader.class.getName(), constraintReader)
                .build());
    }

    @Test
    public void listResponse() throws Exception {
        HandlerMethod handlerMethod = createHandlerMethod("getItems");
        mockFieldComment(Item.class, "field1", "A string");
        mockFieldComment(Item.class, "field2", "A decimal");

        this.snippets.expect(AUTO_RESPONSE_FIELDS).withContents(
                tableWithHeader("Path", "Type", "Optional", "Description")
                        .row("[].field1", "String", "true", "A string.")
                        .row("[].field2", "Decimal", "true", "A decimal."));

        new JacksonResponseFieldSnippet().document(operationBuilder
                .attribute(HandlerMethod.class.getName(), handlerMethod)
                .attribute(ObjectMapper.class.getName(), mapper)
                .attribute(JavadocReader.class.getName(), javadocReader)
                .attribute(ConstraintReader.class.getName(), constraintReader)
                .build());
    }

    @Test
    public void streamResponse() throws Exception {
        HandlerMethod handlerMethod = createHandlerMethod("getStream");
        mockFieldComment(Item.class, "field1", "A string");
        mockFieldComment(Item.class, "field2", "A decimal");

        this.snippets.expect(AUTO_RESPONSE_FIELDS).withContents(
                tableWithHeader("Path", "Type", "Optional", "Description")
                        .row("[].field1", "String", "true", "A string.")
                        .row("[].field2", "Decimal", "true", "A decimal."));

        new JacksonResponseFieldSnippet().document(operationBuilder
                .attribute(HandlerMethod.class.getName(), handlerMethod)
                .attribute(ObjectMapper.class.getName(), mapper)
                .attribute(JavadocReader.class.getName(), javadocReader)
                .attribute(ConstraintReader.class.getName(), constraintReader)
                .build());
    }

    @Test
    public void noResponseBody() throws Exception {
        HandlerMethod handlerMethod = createHandlerMethod("noItem");

        this.snippets.expect(AUTO_RESPONSE_FIELDS).withContents(equalTo("No response body."));

        new JacksonResponseFieldSnippet().document(operationBuilder
                .attribute(HandlerMethod.class.getName(), handlerMethod)
                .attribute(ObjectMapper.class.getName(), mapper)
                .build());
    }

    @Test
    public void noHandlerMethod() throws Exception {
        this.snippets.expect(AUTO_RESPONSE_FIELDS).withContents(equalTo("No response body."));

        new JacksonResponseFieldSnippet().document(operationBuilder
                .attribute(ObjectMapper.class.getName(), mapper)
                .build());
    }

    @Test
    public void pageResponse() throws Exception {
        HandlerMethod handlerMethod = createHandlerMethod("pagedItems");
        mockFieldComment(Item.class, "field1", "A string");
        mockFieldComment(Item.class, "field2", "A decimal");
        mockOptionalMessage(Item.class, "field1", "false");
        mockConstraintMessage(Item.class, "field2", "A constraint");

        this.snippets.expect(AUTO_RESPONSE_FIELDS).withContents(
                tableWithPrefix(paginationPrefix(),
                        tableWithHeader("Path", "Type", "Optional", "Description")
                                .row("field1", "String", "false", "A string.")
                                .row("field2", "Decimal", "true",
                                        "A decimal.\n\nA constraint.")));

        new JacksonResponseFieldSnippet().document(operationBuilder
                .attribute(HandlerMethod.class.getName(), handlerMethod)
                .attribute(ObjectMapper.class.getName(), mapper)
                .attribute(JavadocReader.class.getName(), javadocReader)
                .attribute(ConstraintReader.class.getName(), constraintReader)
                .build());
    }

    @Test
    public void responseEntityResponse() throws Exception {
        HandlerMethod handlerMethod = createHandlerMethod("responseEntityItem");
        mockFieldComment(Item.class, "field1", "A string");
        mockFieldComment(Item.class, "field2", "A decimal");
        mockOptionalMessage(Item.class, "field1", "false");
        mockConstraintMessage(Item.class, "field2", "A constraint");

        this.snippets.expect(AUTO_RESPONSE_FIELDS).withContents(
                tableWithHeader("Path", "Type", "Optional", "Description")
                        .row("field1", "String", "false", "A string.")
                        .row("field2", "Decimal", "true",
                                "A decimal.\n\nA constraint."));

        new JacksonResponseFieldSnippet().document(operationBuilder
                .attribute(HandlerMethod.class.getName(), handlerMethod)
                .attribute(ObjectMapper.class.getName(), mapper)
                .attribute(JavadocReader.class.getName(), javadocReader)
                .attribute(ConstraintReader.class.getName(), constraintReader)
                .build());
    }

    @Test
    public void responseEntityResponseWithoutGenerics() throws Exception {
        HandlerMethod handlerMethod = createHandlerMethod("responseEntityItem2");

        this.snippets.expect(AUTO_RESPONSE_FIELDS).withContents(equalTo("No response body."));

        new JacksonResponseFieldSnippet().document(operationBuilder
                .attribute(HandlerMethod.class.getName(), handlerMethod)
                .attribute(ObjectMapper.class.getName(), mapper)
                .attribute(JavadocReader.class.getName(), javadocReader)
                .attribute(ConstraintReader.class.getName(), constraintReader)
                .build());
    }

    @Test
    public void monoResponse() throws Exception {
        HandlerMethod handlerMethod = createHandlerMethod("monoItem");
        mockFieldComment(Item.class, "field1", "A string");
        mockFieldComment(Item.class, "field2", "A decimal");
        mockOptionalMessage(Item.class, "field1", "false");
        mockConstraintMessage(Item.class, "field2", "A constraint");

        this.snippets.expect(AUTO_RESPONSE_FIELDS).withContents(
                tableWithHeader("Path", "Type", "Optional", "Description")
                        .row("field1", "String", "false", "A string.")
                        .row("field2", "Decimal", "true",
                                "A decimal.\n\nA constraint."));

        new JacksonResponseFieldSnippet().document(operationBuilder
                .attribute(HandlerMethod.class.getName(), handlerMethod)
                .attribute(ObjectMapper.class.getName(), mapper)
                .attribute(JavadocReader.class.getName(), javadocReader)
                .attribute(ConstraintReader.class.getName(), constraintReader)
                .build());
    }

    @Test
    public void fluxResponse() throws Exception {
        HandlerMethod handlerMethod = createHandlerMethod("fluxItems");
        mockFieldComment(Item.class, "field1", "A string");
        mockFieldComment(Item.class, "field2", "A decimal");

        this.snippets.expect(AUTO_RESPONSE_FIELDS).withContents(
                tableWithHeader("Path", "Type", "Optional", "Description")
                        .row("[].field1", "String", "true", "A string.")
                        .row("[].field2", "Decimal", "true", "A decimal."));

        new JacksonResponseFieldSnippet().document(operationBuilder
                .attribute(HandlerMethod.class.getName(), handlerMethod)
                .attribute(ObjectMapper.class.getName(), mapper)
                .attribute(JavadocReader.class.getName(), javadocReader)
                .attribute(ConstraintReader.class.getName(), constraintReader)
                .build());
    }

    @Test
    public void resourcesResponse() throws Exception {
        HandlerMethod handlerMethod = createHandlerMethod("itemResources");

        this.snippets.expect(AUTO_RESPONSE_FIELDS).withContents(is("Body contains embedded resources."));

        new JacksonResponseFieldSnippet().document(operationBuilder
                .attribute(HandlerMethod.class.getName(), handlerMethod)
                .attribute(ObjectMapper.class.getName(), mapper)
                .attribute(JavadocReader.class.getName(), javadocReader)
                .attribute(ConstraintReader.class.getName(), constraintReader)
                .build());
    }

    @Test
    public void responseWithLinksAndEmbedded() throws Exception {
        HandlerMethod handlerMethod = createHandlerMethod("halItem");
        mockFieldComment(HalItem.class, "actualContent", "A string");

        this.snippets.expect(AUTO_RESPONSE_FIELDS).withContents(
                tableWithHeader("Path", "Type", "Optional", "Description")
                        .row("actualContent", "String", "true", "A string."));

        new JacksonResponseFieldSnippet().document(operationBuilder
                .attribute(HandlerMethod.class.getName(), handlerMethod)
                .attribute(ObjectMapper.class.getName(), mapper)
                .attribute(JavadocReader.class.getName(), javadocReader)
                .attribute(ConstraintReader.class.getName(), constraintReader)
                .build());
    }

    @Test
    public void exactResponseType() throws Exception {
        HandlerMethod handlerMethod = createHandlerMethod("processItem");
        mockFieldComment(ProcessingResponse.class, "output", "An output");

        this.snippets.expect(AUTO_RESPONSE_FIELDS).withContents(
                tableWithHeader("Path", "Type", "Optional", "Description")
                        .row("output", "String", "true", "An output."));

        new JacksonResponseFieldSnippet().responseBodyAsType(ProcessingResponse.class)
                .document(operationBuilder
                        .attribute(HandlerMethod.class.getName(), handlerMethod)
                        .attribute(ObjectMapper.class.getName(), mapper)
                        .attribute(JavadocReader.class.getName(), javadocReader)
                        .attribute(ConstraintReader.class.getName(), constraintReader)
                        .build());
    }

    @Test
    public void hasContent() throws Exception {
        HandlerMethod handlerMethod = createHandlerMethod("getItem");

        boolean hasContent = new JacksonResponseFieldSnippet().hasContent(operationBuilder
                .attribute(HandlerMethod.class.getName(), handlerMethod)
                .build());
        assertThat(hasContent, is(true));
    }

    @Test
    public void noContent() throws Exception {
        HandlerMethod handlerMethod = createHandlerMethod("noItem");

        boolean hasContent = new JacksonResponseFieldSnippet().hasContent(operationBuilder
                .attribute(HandlerMethod.class.getName(), handlerMethod)
                .build());
        assertThat(hasContent, is(false));
    }

    @Test
    public void failOnUndocumentedFields() throws Exception {
        HandlerMethod handlerMethod = createHandlerMethod("getItem");

        thrown.expect(SnippetException.class);
        thrown.expectMessage("Following response fields were not documented: [field1, field2]");

        new JacksonResponseFieldSnippet().failOnUndocumentedFields(true).document(operationBuilder
                .attribute(HandlerMethod.class.getName(), handlerMethod)
                .attribute(ObjectMapper.class.getName(), mapper)
                .attribute(JavadocReader.class.getName(), javadocReader)
                .attribute(ConstraintReader.class.getName(), constraintReader)
                .build());
    }

    @Test
    public void escaping() throws Exception {
        HandlerMethod handlerMethod = createHandlerMethod("processItem");
        mockFieldComment(ProcessingResponse.class, "output", "An output | result");

        this.snippets.expect(AUTO_RESPONSE_FIELDS).withContents(
                tableWithHeader("Path", "Type", "Optional", "Description")
                        .row("output", "String", "true", "An output \\| result."));

        new JacksonResponseFieldSnippet().responseBodyAsType(ProcessingResponse.class)
                .document(operationBuilder
                        .attribute(HandlerMethod.class.getName(), handlerMethod)
                        .attribute(ObjectMapper.class.getName(), mapper)
                        .attribute(JavadocReader.class.getName(), javadocReader)
                        .attribute(ConstraintReader.class.getName(), constraintReader)
                        .build());
    }

    @Test
    public void deprecated() throws Exception {
        HandlerMethod handlerMethod = createHandlerMethod("removeItem");
        mockFieldComment(DeprecatedItem.class, "index", "item's index");
        mockFieldComment(DeprecatedItem.class, "index2", "item's index2");
        mockFieldComment(DeprecatedItem.class, "index3", "item's index3");
        mockFieldComment(DeprecatedItem.class, "index4", "item's index4");
        mockFieldComment(DeprecatedItem.class, "index5", "item's index5");
        mockMethodComment(DeprecatedItem.class, "getIndex6", "item's index6");

        // index2 and getIndex4 have @deprecated Javadoc
        mockDeprecatedField(DeprecatedItem.class, "index2", "use something else");
        mockDeprecatedMethod(DeprecatedItem.class, "getIndex4", "use something else");

        this.snippets.expect(AUTO_RESPONSE_FIELDS).withContents(
                tableWithHeader("Path", "Type", "Optional", "Description")
                        .row("index", "Integer", "true",
                                "**Deprecated.**\n\nItem's index.")
                        .row("index2", "Integer", "true",
                                "**Deprecated.** Use something else.\n\nItem's index2.")
                        .row("index3", "Integer", "true",
                                "**Deprecated.**\n\nItem's index3.")
                        .row("index4", "Integer", "true",
                                "**Deprecated.** Use something else.\n\nItem's index4.")
                        .row("index5", "Integer", "true",
                                "**Deprecated.**\n\nItem's index5.")
                        .row("index6", "Integer", "true",
                                "**Deprecated.**\n\nItem's index6."));

        new JacksonResponseFieldSnippet().document(operationBuilder
                .attribute(HandlerMethod.class.getName(), handlerMethod)
                .attribute(ObjectMapper.class.getName(), mapper)
                .attribute(JavadocReader.class.getName(), javadocReader)
                .attribute(ConstraintReader.class.getName(), constraintReader)
                .build());
    }

    @Test
    public void comment() throws Exception {
        HandlerMethod handlerMethod = createHandlerMethod("commentItem");
        mockFieldComment(CommentedItem.class, "field", "field");
        mockFieldComment(CommentedItem.class, "field2", "field 2");
        mockFieldComment(CommentedItem.class, "field3", "field 3");
        mockMethodComment(CommentedItem.class, "getField3", "method 3"); // preferred
        mockMethodComment(CommentedItem.class, "getField4", "method 4");

        this.snippets.expect(AUTO_RESPONSE_FIELDS).withContents(
                tableWithHeader("Path", "Type", "Optional", "Description")
                        .row("field", "String", "true", "Field.")
                        .row("field2", "String", "true", "Field 2.")
                        .row("field3", "String", "true", "Method 3.")
                        .row("field4", "String", "true", "Method 4."));

        new JacksonResponseFieldSnippet().document(operationBuilder
                .attribute(HandlerMethod.class.getName(), handlerMethod)
                .attribute(ObjectMapper.class.getName(), mapper)
                .attribute(JavadocReader.class.getName(), javadocReader)
                .attribute(ConstraintReader.class.getName(), constraintReader)
                .build());
    }

    private void mockConstraintMessage(Class<?> type, String fieldName, String comment) {
        when(constraintReader.getConstraintMessages(type, fieldName))
                .thenReturn(singletonList(comment));
    }

    private void mockOptionalMessage(Class<?> type, String fieldName, String comment) {
        when(constraintReader.getOptionalMessages(type, fieldName))
                .thenReturn(singletonList(comment));
    }

    private void mockFieldComment(Class<?> type, String fieldName, String comment) {
        when(javadocReader.resolveFieldComment(type, fieldName))
                .thenReturn(comment);
    }

    private void mockMethodComment(Class<?> type, String methodName, String comment) {
        when(javadocReader.resolveMethodComment(type, methodName))
                .thenReturn(comment);
    }

    private void mockDeprecatedMethod(Class<?> type, String methodName, String comment) {
        when(javadocReader.resolveMethodTag(type, methodName, "deprecated"))
                .thenReturn(comment);
    }

    private void mockDeprecatedField(Class<?> type, String fieldName, String comment) {
        when(javadocReader.resolveFieldTag(type, fieldName, "deprecated"))
                .thenReturn(comment);
    }

    private String paginationPrefix() {
        if ("adoc".equals(templateFormat.getFileExtension())) {
            return fixLineSeparator("Standard <<overview-pagination,paging>> response where `content` field is"
                    + " list of following objects:\n\n");
        } else {
            return fixLineSeparator("Standard [paging](#overview-pagination) response where `content` field is"
                    + " list of following objects:\n\n");
        }
    }

    private HandlerMethod createHandlerMethod(String responseEntityItem)
            throws NoSuchMethodException {
        return new HandlerMethod(new TestResource(), responseEntityItem);
    }

    // actual method responses do not matter, they are here just for the illustration
    private static class TestResource {

        public Item getItem() {
            return new Item("test");
        }

        public List<Item> getItems() {
            return Collections.singletonList(new Item("test"));
        }

        public Stream<Item> getStream() {
            return Stream.of(new Item("test"));
        }

        public void noItem() {
            // NOOP
        }

        public Page<Item> pagedItems() {
            return new PageImpl<>(singletonList(new Item("test")));
        }

        public ResponseEntity<Item> responseEntityItem() {
            return ResponseEntity.ok(new Item("test"));
        }

        public ResponseEntity responseEntityItem2() {
            return ResponseEntity.ok(new Item("test"));
        }

        public String processItem() {
            return "";
        }

        public DeprecatedItem removeItem() {
            return new DeprecatedItem();
        }

        public CommentedItem commentItem() {
            return new CommentedItem();
        }

        public Mono<Item> monoItem() {
            return Mono.just(new Item("x"));
        }

        public Flux<Item> fluxItems() {
            return Flux.just(new Item("a"), new Item("b"));
        }

        public Resources<Item> itemResources() {
            return new Resources<>(singletonList(new Item("y")));
        }

        public HalItem halItem() {
            HalItem response = new HalItem("z");
            response.embed("commented", new CommentedItem());
            response.add(linkTo(methodOn(TestResource.class).getItem()).withSelfRel());
            return response;
        }
    }

    private static class Item {
        @NotBlank
        private String field1;
        @DecimalMin("1")
        @DecimalMax("10")
        private BigDecimal field2;

        public Item(String field1) {
            this.field1 = field1;
        }
    }

    private static class DeprecatedItem {
        // dep. annot on field, no getter
        @Deprecated
        private int index;

        // dep. Javadoc on field, no getter
        private int index2;

        // field, dep. annot on getter
        private int index3;

        // field, dep. Javadoc on getter
        private int index4;

        // dep. annot on field, getter
        @Deprecated
        private int index5;

        @Deprecated
        public int getIndex3() {
            return index3;
        }

        // deprecation Javadoc
        public int getIndex4() {
            return index4;
        }

        public int getIndex5() {
            return index5;
        }

        // no real field
        @Deprecated
        public int getIndex6() {
            return 0;
        }
    }

    private static class CommentedItem {
        // comment on field only, no getter
        private String field;

        // comment on field, empty on getter
        private String field2;

        // comment on field and getter
        private String field3;

        public String getField2() {
            return field2;
        }

        public String getField3() {
            return field3;
        }

        // comment only on getter
        public String getField4() {
            return null;
        }
    }

    private static class ProcessingResponse {
        private String output;
    }

    private static class HalItem extends ResourceSupport {

        private String actualContent;

        HalItem(String actualContent) {
            this.actualContent = actualContent;
        }

        @JsonIgnore
        private List<EmbeddedWrapper> embedded = new ArrayList<>();

        @JsonIgnore
        private EmbeddedWrappers wrapper = new EmbeddedWrappers(false);

        @JsonInclude(NON_EMPTY)
        @JsonUnwrapped
        public Resources<EmbeddedWrapper> getEmbeddedResources() {
            return new Resources(embedded);
        }

        public void embed(String rel, Object resource) {
            if (resource != null) {
                embedded.add(wrapper.wrap(resource, rel));
            }
        }
    }
}
