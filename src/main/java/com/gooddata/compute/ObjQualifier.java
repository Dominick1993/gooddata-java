/*
 * Copyright (C) 2017, GoodData(R) Corporation. All rights reserved.
 */
package com.gooddata.compute;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Qualifies metadata {@link com.gooddata.md.Obj}
 */
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ObjUriQualifier.class, name = "uri"),
        @JsonSubTypes.Type(value = ObjIdentifierQualifier.class, name = "identifier")
})
public interface ObjQualifier {

    /**
     * Returns the qualifier in the form of uri. Default implementation throws {@link UnsupportedOperationException}
     * @return uri qualifier
     */
    default String getUri() {
        throw new UnsupportedOperationException("This qualifier has no URI");
    }
}