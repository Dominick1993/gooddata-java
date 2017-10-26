/*
 * Copyright (C) 2007-2017, GoodData(R) Corporation. All rights reserved.
 * This source code is licensed under the BSD-style license found in the
 * LICENSE.txt file in the root directory of this source tree.
 */
package com.gooddata.executeafm.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

import java.util.List;

import static com.gooddata.util.Validate.notEmpty;

@JsonRootName(AttributeHeader.NAME)
public class AttributeHeader extends Header {

    static final String NAME = "attributeHeader";

    private final String name;
    private final String localIdentifier;
    private final String uri;
    private final String identifier;

    public AttributeHeader(final String name, final String localIdentifier, final String uri, final String identifier) {
        this.name = name;
        this.localIdentifier = localIdentifier;
        this.uri = uri;
        this.identifier = identifier;
    }

    @JsonCreator
    public AttributeHeader(@JsonProperty("name") final String name,
                           @JsonProperty("localIdentifier") final String localIdentifier,
                           @JsonProperty("uri") final String uri,
                           @JsonProperty("identifier") final String identifier,
                           @JsonProperty("totalItems") final List<TotalHeaderItem> totalHeaderItems) {
        super(totalHeaderItems);
        this.name = notEmpty(name, "name");
        this.localIdentifier = notEmpty(localIdentifier, "localIdentifier");
        this.uri = notEmpty(uri, "uri");
        this.identifier = notEmpty(identifier, "identifier");
    }

    public String getName() {
        return name;
    }

    public String getLocalIdentifier() {
        return localIdentifier;
    }

    public String getUri() {
        return uri;
    }

    public String getIdentifier() {
        return identifier;
    }
}
