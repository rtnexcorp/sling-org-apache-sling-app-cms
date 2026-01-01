#
#		 Licensed to the Apache Software Foundation (ASF) under one or more contributor license
#        agreements. See the NOTICE file distributed with this work for additional information
#        regarding copyright ownership. The ASF licenses this file to you under the Apache License,
#        Version 2.0 (the "License"); you may not use this file except in compliance with the
#        License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
#        Unless required by applicable law or agreed to in writing, software distributed under the
#        License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
#        either express or implied. See the License for the specific language governing permissions
#        and limitations under the License.
#
# Apache Sling CMS Dockerfile

# Pull base image - Java 21 LTS (matches project requirement)
FROM eclipse-temurin:21-jre

# Build arguments
ARG CMS_GROUP_ID=org.apache.sling
ARG CMS_ARTIFACT_ID=org.apache.sling.cms.feature
ARG CMS_VERSION=1.1.9-SNAPSHOT
ARG RUNMODE=standalone

# Environment variables for runtime
ENV CMS_GROUP_ID=${CMS_GROUP_ID}
ENV CMS_ARTIFACT_ID=${CMS_ARTIFACT_ID}
ENV CMS_VERSION=${CMS_VERSION}
ENV RUNMODE=${RUNMODE}

# Configure directories
WORKDIR /opt/slingcms

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Copy Maven settings
RUN mkdir -p /root/.m2
COPY settings.xml /root/.m2/

# Download and setup Sling CMS
COPY download-dependencies.sh /opt/slingcms/setup/
RUN /bin/bash /opt/slingcms/setup/download-dependencies.sh

# Expose endpoints
EXPOSE 8080

# Start Sling CMS
CMD ["java", "-jar", "slingcms.jar"]