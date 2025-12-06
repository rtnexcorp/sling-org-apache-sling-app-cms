/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.cms.distribution.impl;

import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.cms.distribution.ContentDistributionService;
import org.apache.sling.cms.publication.INSTANCE_TYPE;
import org.apache.sling.cms.publication.PUBLICATION_MODE;
import org.apache.sling.cms.publication.PublicationManager;
import org.apache.sling.cms.publication.PublicationManagerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = { PublicationManagerFactory.class, AdapterFactory.class }, property = {
        AdapterFactory.ADAPTABLE_CLASSES + "=org.apache.sling.api.SlingHttpServletRequest",
        AdapterFactory.ADAPTABLE_CLASSES + "=org.apache.sling.api.resource.ResourceResolver",
        AdapterFactory.ADAPTER_CLASSES + "=org.apache.sling.cms.publication.PublicationManager" })
@Designate(ocd = PublicationConfig.class)
public class PublicationManagerFactoryImpl implements PublicationManagerFactory, AdapterFactory {

    private static final Logger log = LoggerFactory.getLogger(PublicationManagerFactoryImpl.class);

    @Reference
    private EventAdmin eventAdmin;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
    private ContentDistributionService contentDistributionService;

    private INSTANCE_TYPE instanceType;

    private PUBLICATION_MODE publicationMode;

    @Activate
    public void activate(PublicationConfig config) {
        this.instanceType = config.instanceType();
        this.publicationMode = config.publicationMode();
        log.info("PublicationManagerFactory activated with mode: {}, type: {}", publicationMode, instanceType);
    }

    @Override
    public INSTANCE_TYPE getInstanceType() {
        return this.instanceType;
    }

    @Override
    public PUBLICATION_MODE getPublicationMode() {
        return this.publicationMode;
    }

    @Override
    public PublicationManager getPublicationManager() {
        if (publicationMode == PUBLICATION_MODE.STANDALONE) {
            log.debug("Using StandalonePublicationManager");
            return new StandalonePublicationManager(eventAdmin);
        } else if (contentDistributionService != null) {
            log.debug("Using HttpDistributionPublicationManager");
            return new HttpDistributionPublicationManager(contentDistributionService, eventAdmin);
        } else {
            log.warn("Content Distribution Service not available, falling back to StandalonePublicationManager");
            return new StandalonePublicationManager(eventAdmin);
        }
    }

    @Override
    public <T> T getAdapter(Object adaptable, Class<T> type) {
        return type.cast(getPublicationManager());
    }

}
