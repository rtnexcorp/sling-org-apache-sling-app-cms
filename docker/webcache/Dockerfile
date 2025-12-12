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
# Apache Sling CMS Web Cache Dockerfile

FROM ubuntu:24.04

ARG DEBIAN_FRONTEND=noninteractive

# Install Apache and enable required modules
RUN apt-get update && \
    apt-get install -y --no-install-recommends apache2 && \
    a2enmod rewrite proxy proxy_http cache cache_disk expires headers && \
    rm -rf /var/lib/apt/lists/*

# Configure sites
RUN mkdir -p /var/www/vhosts/sling-cms /var/www/vhosts/sling
COPY *.conf /etc/apache2/sites-enabled/

EXPOSE 80

CMD ["apachectl", "-D", "FOREGROUND"]
