/*
 * Copyright (C) 2007-2017, GoodData(R) Corporation. All rights reserved.
 * This source code is licensed under the BSD-style license found in the
 * LICENSE.txt file in the root directory of this source tree.
 */
package com.gooddata.executeafm;

import com.gooddata.AbstractGoodDataAT;
import com.gooddata.executeafm.afm.Afm;
import com.gooddata.executeafm.afm.AttributeItem;
import com.gooddata.executeafm.afm.MeasureItem;
import com.gooddata.executeafm.afm.SimpleMeasureDefinition;
import com.gooddata.executeafm.response.AttributeHeader;
import com.gooddata.executeafm.response.ExecutionResponse;
import com.gooddata.executeafm.response.MeasureGroupHeader;
import com.gooddata.executeafm.response.ResultDimension;
import com.gooddata.executeafm.result.DataList;
import com.gooddata.executeafm.result.DataValue;
import com.gooddata.executeafm.result.ExecutionResult;
import com.gooddata.executeafm.result.ResultHeaderItem;
import com.gooddata.md.visualization.Bucket;
import com.gooddata.md.visualization.Measure;
import com.gooddata.md.visualization.VOSimpleMeasureDefinition;
import com.gooddata.md.visualization.VisualizationAttribute;
import com.gooddata.md.visualization.VisualizationClass;
import com.gooddata.md.visualization.VisualizationObject;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.List;

import static com.gooddata.md.Restriction.identifier;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

public class ExecuteAfmServiceAT extends AbstractGoodDataAT {

    private static final String GDC_TABLE_VISUALIZATION_CLASS_ID = "gdc.visualization.table";
    private static final String ATTRIBUTE_LOCAL_IDENTIFIER = "a1";
    private static final String MEASURE_LOCAL_IDENTIFIER = "m1";

    private ExecutionResponse afmResponse;
    private ExecutionResponse visResponse;

    @Test(groups = "executeAfm", dependsOnGroups = {"model", "md", "dataset"})
    public void testExecuteAfm() {
        final Execution execution = new Execution(new Afm()
                .addAttribute(new AttributeItem(new IdentifierObjQualifier(attr.getDefaultDisplayForm().getIdentifier()),
                        ATTRIBUTE_LOCAL_IDENTIFIER))
                .addMeasure(new MeasureItem(new SimpleMeasureDefinition(new UriObjQualifier(metric.getUri())),
                        MEASURE_LOCAL_IDENTIFIER))
        );

        afmResponse = gd.getExecuteAfmService().executeAfm(project, execution);

        checkExecutionResponse(afmResponse);
    }

    @Test(groups = "executeAfm", dependsOnGroups = {"model", "md", "dataset"})
    public void testExecuteVisualization() {
        final VisualizationObject vizObject = createVisualizationObject();
        final VisualizationExecution execution = new VisualizationExecution(vizObject.getUri());

        visResponse = gd.getExecuteAfmService().executeVisualization(project, execution);

        checkExecutionResponse(visResponse);
    }

    @Test(groups = "executeAfm", dependsOnMethods = "testExecuteAfm")
    public void testGetAfmExecutionResult() {
        final ExecutionResult afmResult = gd.getExecuteAfmService().getResult(afmResponse).get();
        checkExecutionResult(afmResult);
    }

    @Test(groups = "executeAfm", dependsOnMethods = "testExecuteVisualization")
    public void testGetVisualizationExecutionResult() {
        final ExecutionResult visResult = gd.getExecuteAfmService().getResult(visResponse).get();
        checkExecutionResult(visResult);
    }

    private VisualizationObject createVisualizationObject() {
        final String vizClassUri = gd.getMetadataService().getObjUri(project, VisualizationClass.class,
                identifier(GDC_TABLE_VISUALIZATION_CLASS_ID));

        final VisualizationObject vizObject = new VisualizationObject("some title", vizClassUri);
        vizObject.setBuckets(asList(
                new Bucket("vizObjBucket1", singletonList(new VisualizationAttribute(
                        new UriObjQualifier(attr.getDefaultDisplayForm().getUri()), ATTRIBUTE_LOCAL_IDENTIFIER))),
                new Bucket("vizObjBucket2", singletonList(new Measure(
                        new VOSimpleMeasureDefinition(new UriObjQualifier(metric.getUri())), MEASURE_LOCAL_IDENTIFIER)))
        ));
        return gd.getMetadataService().createObj(project, vizObject);
    }

    private static void checkExecutionResponse(final ExecutionResponse response) {
        assertThat(response, notNullValue());
        assertThat("should have 2 dimensions", response.getDimensions(), hasSize(2));

        final ResultDimension firstDim = response.getDimensions().get(0);
        assertThat("1st dim should have 1 header", firstDim.getHeaders(), hasSize(1));
        assertThat("1st dim 1st header should be AttributeHeader", firstDim.getHeaders().get(0), instanceOf(AttributeHeader.class));
        final AttributeHeader attrHeader = (AttributeHeader) firstDim.getHeaders().get(0);
        assertThat("header's formOf should point to given attribute", attrHeader.getFormOf().getUri(), is(attr.getUri()));

        final ResultDimension secondDim = response.getDimensions().get(1);
        assertThat("2nd dim should have 1 header", secondDim.getHeaders(), hasSize(1));
        assertThat("2nd dim 1st header should be MeasureGroupHeader", secondDim.getHeaders().get(0), instanceOf(MeasureGroupHeader.class));
        final MeasureGroupHeader measureGroupHeader = (MeasureGroupHeader) secondDim.getHeaders().get(0);
        assertThat(measureGroupHeader.getItems(), hasSize(1));
        assertThat("the only measureHeader should point to given metric", measureGroupHeader.getItems().get(0).getUri(), is(metric.getUri()));
    }

    private static void checkExecutionResult(final ExecutionResult result) {
        assertThat(result, notNullValue());

        final List<ResultHeaderItem> firstDimHeaders = result.getHeaderItems().get(0).get(0);
        assertThat("1st dim should have two header items", firstDimHeaders, hasSize(2));
        assertThat(headerItemsNames(firstDimHeaders), hasItems("HR", "DevOps"));

        final List<ResultHeaderItem> secondDimHeaders = result.getHeaderItems().get(1).get(0);
        assertThat("2nd dim should have one header item", secondDimHeaders, hasSize(1));
        assertThat(headerItemsNames(secondDimHeaders), hasItems(metric.getTitle()));

        assertThat(result.getData(), is(new DataList(asList(
                new DataList(singletonList(new DataValue("41"))),
                new DataList(singletonList(new DataValue("36")))))));
    }

    private static Collection<String> headerItemsNames(final List<ResultHeaderItem> items) {
        return items.stream().map(ResultHeaderItem::getName).collect(toList());
    }
}