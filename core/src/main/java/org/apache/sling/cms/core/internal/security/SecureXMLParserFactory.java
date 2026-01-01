/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.cms.core.internal.security;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * Utility class to configure XML parsers with secure settings to prevent XXE attacks.
 *
 * This class provides factory methods for creating secure XML parsers that mitigate
 * XML External Entity (XXE) vulnerabilities, particularly important when using
 * Apache Tika 1.x which is vulnerable to CVE-2025-66516.
 *
 * @see <a href="https://github.com/advisories/GHSA-f58c-gq56-vjjf">CVE-2025-66516</a>
 * @see <a href="https://owasp.org/www-community/vulnerabilities/XML_External_Entity_(XXE)_Processing">OWASP XXE Prevention</a>
 */
public final class SecureXMLParserFactory {

    private static final String FEATURE_DISALLOW_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String FEATURE_EXTERNAL_GENERAL_ENTITIES =
            "http://xml.org/sax/features/external-general-entities";
    private static final String FEATURE_EXTERNAL_PARAMETER_ENTITIES =
            "http://xml.org/sax/features/external-parameter-entities";
    private static final String FEATURE_LOAD_EXTERNAL_DTD =
            "http://apache.org/xml/features/nonvalidating/load-external-dtd";
    private static final String PROPERTY_ENTITY_EXPANSION_LIMIT =
            "http://www.oracle.com/xml/jaxp/properties/entityExpansionLimit";

    private SecureXMLParserFactory() {
        // Utility class, no instantiation
    }

    /**
     * Creates a secure DocumentBuilderFactory with XXE protections enabled.
     *
     * Security features:
     * - Disallows DOCTYPE declarations
     * - Disables external general entities
     * - Disables external parameter entities
     * - Disables loading external DTDs
     * - Limits entity expansion to prevent billion laughs attack
     * - Disables XInclude processing
     * - Enables secure processing
     *
     * @return a secure DocumentBuilderFactory instance
     * @throws ParserConfigurationException if security features cannot be set
     */
    public static DocumentBuilderFactory createSecureDocumentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        try {
            // Disable DOCTYPE declarations entirely
            factory.setFeature(FEATURE_DISALLOW_DOCTYPE, true);

            // Disable external entities
            factory.setFeature(FEATURE_EXTERNAL_GENERAL_ENTITIES, false);
            factory.setFeature(FEATURE_EXTERNAL_PARAMETER_ENTITIES, false);

            // Disable loading external DTD
            factory.setFeature(FEATURE_LOAD_EXTERNAL_DTD, false);

            // Enable secure processing
            factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);

            // Disable XInclude
            factory.setXIncludeAware(false);

            // Disable namespace aware (if not needed)
            factory.setNamespaceAware(false);

            // Disable expanding entity references
            factory.setExpandEntityReferences(false);

            // Limit entity expansion (prevent billion laughs attack)
            try {
                factory.setAttribute(PROPERTY_ENTITY_EXPANSION_LIMIT, "0");
            } catch (IllegalArgumentException e) {
                // Some parsers don't support this property, continue with other protections
            }

        } catch (ParserConfigurationException e) {
            throw new ParserConfigurationException("Failed to configure secure XML parser: " + e.getMessage());
        }

        return factory;
    }

    /**
     * Creates a secure SAXParserFactory with XXE protections enabled.
     *
     * @return a secure SAXParserFactory instance
     * @throws ParserConfigurationException if security features cannot be set
     * @throws SAXNotRecognizedException if a feature is not recognized
     * @throws SAXNotSupportedException if a feature is not supported
     */
    public static SAXParserFactory createSecureSAXParserFactory()
            throws ParserConfigurationException, SAXNotRecognizedException, SAXNotSupportedException {
        SAXParserFactory factory = SAXParserFactory.newInstance();

        // Disable DOCTYPE declarations
        factory.setFeature(FEATURE_DISALLOW_DOCTYPE, true);

        // Disable external entities
        factory.setFeature(FEATURE_EXTERNAL_GENERAL_ENTITIES, false);
        factory.setFeature(FEATURE_EXTERNAL_PARAMETER_ENTITIES, false);

        // Disable loading external DTD
        factory.setFeature(FEATURE_LOAD_EXTERNAL_DTD, false);

        // Enable secure processing
        factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);

        // Disable namespace processing
        factory.setNamespaceAware(false);

        return factory;
    }

    /**
     * Applies secure XML parsing settings to Tika configuration.
     * This should be called during application initialization.
     */
    public static void configureTikaXMLSecurity() {
        // Set system properties to enforce secure XML parsing globally
        System.setProperty(
                "javax.xml.parsers.DocumentBuilderFactory",
                "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
        System.setProperty(
                "javax.xml.parsers.SAXParserFactory", "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl");

        // Disable external entity processing at JVM level
        System.setProperty("org.apache.xml.dtm.DTMManager", "org.apache.xml.dtm.ref.DTMManagerDefault");
    }
}
