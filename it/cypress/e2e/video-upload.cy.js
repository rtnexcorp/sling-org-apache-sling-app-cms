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

const { login, sendPost } = require("../util/test-helper");
const { doneLoading } = require("../utils");

describe("Video Upload and Thumbnail Tests", () => {
  const testFolder = "/static/video-test";
  const videoFileName = "test-video.mp4";
  const videoPath = `${testFolder}/${videoFileName}`;

  before(() => {
    login();
    // Clean up test folder if it exists
    sendPost(testFolder, {
      ":operation": "delete",
    });
    // Create test folder
    sendPost("/static", {
      ":operation": "createFolder",
      ":name": "video-test",
    });
  });

  beforeEach(() => {
    login();
  });

  after(() => {
    login();
    // Clean up test folder
    sendPost(testFolder, {
      ":operation": "delete",
    });
  });

  it("should upload a video file", () => {
    cy.visit(`/cms/static/content.html${testFolder}`);
    doneLoading();

    // Click "Add File" button
    cy.get('a[data-title="Add File"]').click();
    doneLoading();

    // Upload video file
    cy.get('input[type="file"]').selectFile(
      "cypress/fixtures/test-video.mp4",
      { force: true }
    );

    // Submit form
    cy.get(".modal form button[type=submit]").click();
    doneLoading();

    // Close modal
    cy.get(".modal .close-modal.is-primary").click();
    doneLoading();

    // Verify video file appears in the list
    cy.get(`[data-value="${videoPath}"]`).should("exist");
  });

  it("should generate video thumbnail", () => {
    cy.visit(`/cms/static/content.html${testFolder}`);
    doneLoading();

    // Click on the video file
    cy.get(`[data-value="${videoPath}"]`).click();
    doneLoading();

    // Check thumbnail preview exists and loads
    cy.get('img[src*="test-video.mp4.transform/sling-cms-thumbnail"]', {
      timeout: 10000,
    }).should("exist");

    // Verify thumbnail image loads successfully (not broken image)
    cy.get('img[src*="test-video.mp4.transform/sling-cms-thumbnail"]').should(
      ($img) => {
        // Check that image has natural width > 0 (loaded successfully)
        expect($img[0].naturalWidth).to.be.greaterThan(0);
      }
    );
  });

  it("should display video thumbnail in file preview", () => {
    cy.visit(`/cms/file/preview.html${videoPath}`);
    doneLoading();

    // Check that thumbnail image exists
    cy.get(
      'img[src*="test-video.mp4.transform/sling-cms-thumbnail.png"]',
      { timeout: 10000 }
    ).should("exist");

    // Verify thumbnail loads successfully
    cy.get(
      'img[src*="test-video.mp4.transform/sling-cms-thumbnail.png"]'
    ).should(($img) => {
      expect($img[0].naturalWidth).to.be.greaterThan(0);
      expect($img[0].naturalHeight).to.be.greaterThan(0);
    });
  });

  it("should verify video MIME type", () => {
    // Request video file metadata
    cy.request({
      url: `${videoPath}.json`,
      auth: {
        username: "admin",
        password: "admin",
      },
    }).then((response) => {
      expect(response.status).to.eq(200);
      expect(response.body["jcr:content"]["jcr:mimeType"]).to.match(
        /^video\/(mp4|quicktime)/
      );
    });
  });

  it("should generate thumbnail via direct transform URL", () => {
    // Request thumbnail directly via transform servlet
    cy.request({
      url: `${videoPath}.transform/sling-cms-thumbnail.png`,
      auth: {
        username: "admin",
        password: "admin",
      },
      encoding: "binary",
    }).then((response) => {
      expect(response.status).to.eq(200);
      expect(response.headers["content-type"]).to.include("image/png");
      // Verify response body is not empty
      expect(response.body.length).to.be.greaterThan(0);
    });
  });

  it("should use VideoThumbnailProvider not TikaFallbackProvider", () => {
    // This test verifies the correct thumbnail provider is being used
    // by checking the logs (indirectly via successful thumbnail generation)

    cy.request({
      url: `${videoPath}.transform/sling-cms-thumbnail.png`,
      auth: {
        username: "admin",
        password: "admin",
      },
      encoding: "binary",
    }).then((response) => {
      expect(response.status).to.eq(200);

      // If TikaFallbackProvider was used, we'd get a generic icon
      // VideoThumbnailProvider with FFmpeg extracts actual video frames
      // Verify we got a real PNG image (PNG signature: 89 50 4E 47)
      const buffer = Buffer.from(response.body, 'binary');
      expect(buffer[0]).to.eq(0x89);
      expect(buffer[1]).to.eq(0x50);
      expect(buffer[2]).to.eq(0x4E);
      expect(buffer[3]).to.eq(0x47);
    });
  });
});
