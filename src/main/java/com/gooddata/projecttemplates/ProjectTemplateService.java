/**
 * Copyright (C) 2004-2017, GoodData(R) Corporation. All rights reserved.
 * This source code is licensed under the BSD-style license found in the
 * LICENSE.txt file in the root directory of this source tree.
 */
package com.gooddata.projecttemplates;

import com.gooddata.AbstractService;
import com.gooddata.GoodDataException;
import com.gooddata.GoodDataRestException;
import com.gooddata.dataset.DatasetManifest;
import com.gooddata.util.Validate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Service enabling access to project templates, under /projectTemplates/...
 */
public class ProjectTemplateService extends AbstractService {

    /**
     * Sets RESTful HTTP Spring template. Should be called from constructor of concrete service extending
     * this abstract one.
     *
     * @param restTemplate RESTful HTTP Spring template
     */
    public ProjectTemplateService(RestTemplate restTemplate) {
        super(restTemplate);
    }

    /**
     * List of all projects' templates
     * @return list of templates
     */
    public Collection<Template> getTemplates() {
        try {
            return restTemplate.getForObject(Templates.URI, Templates.class).getTemplates();
        } catch (GoodDataRestException | RestClientException e) {
            throw new GoodDataException("Unable to get templates", e);
        }
    }

    /**
     * Get project template by given uri.
     * @param uri uri of the template
     * @return project template
     */
    public Template getTemplate(String uri) {
        Validate.notEmpty(uri, "template uri");
        try {
            return restTemplate.getForObject(uri, Template.class);
        } catch (GoodDataRestException | RestClientException e) {
            throw new GoodDataException("Unable to get template of uri=" + uri, e);
        }
    }

    /**
     * Get manifestsof template by given uri.
     * @param uri uri of the template
     * @return manifests linked from project template
     */
    public Collection<DatasetManifest> getManifests(String uri) {
        try {
            return getTemplate(uri).getManifestsUris().stream()
                    .map(manifestUri -> restTemplate.getForObject(manifestUri, DatasetManifest.class))
                    .collect(Collectors.toList());
        } catch (GoodDataRestException | RestClientException e) {
            throw new GoodDataException("Unable to get manifests for template of uri=" + uri, e);
        }
    }
}
