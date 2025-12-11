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

/**
 * Image Preview functionality for Apache Sling CMS
 * Handles preview switching and zoom in image preview modals
 * 
 * Uses rava.bind() to automatically handle dynamically loaded content
 * (modal content loaded via AJAX won't execute inline scripts,
 * so we use rava's mutation observer to bind events)
 */
import { logger } from './logger.js';

const rava = window.rava;

// Always log this to verify the file is loaded (not dependent on debug mode)
console.log('[cms.imagepreview] Module loaded - rava:', typeof rava);

logger.debug('cms.imagepreview.js loaded');

/**
 * Bind click events to preview-link buttons
 * When clicked, update the preview image without page refresh
 */
rava.bind('.preview-link', {
    callbacks: {
        created() {
            logger.debug('preview-link element bound:', this);
        }
    },
    events: {
        click(event) {
            logger.debug('preview-link clicked:', this);
            event.preventDefault();
            event.stopPropagation();
            
            const imageUrl = this.dataset.image;
            logger.debug('Image URL from data-image:', imageUrl);
            
            const preview = document.getElementById('previewImage');
            logger.debug('Preview element found:', preview);
            
            if (preview && imageUrl) {
                logger.debug('Updating preview image to:', imageUrl);
                
                // Fade out effect
                preview.style.opacity = '0';
                
                // Update image and fade in when loaded
                preview.onload = function() {
                    logger.debug('Image loaded successfully');
                    preview.style.opacity = '1';
                };
                
                preview.onerror = function() {
                    logger.error('Failed to load image:', imageUrl);
                    preview.style.opacity = '1';
                };
                
                preview.src = imageUrl;
            } else {
                logger.warn('Missing preview element or imageUrl', { preview, imageUrl });
            }
        }
    }
});

/**
 * Bind click events to preview image for zoom toggle
 */
rava.bind('.preview-image', {
    callbacks: {
        created() {
            logger.debug('preview-image element bound:', this);
        }
    },
    events: {
        click(event) {
            logger.debug('preview-image clicked, toggling zoom');
            event.preventDefault();
            event.stopPropagation();
            this.classList.toggle('zoomed');
            logger.debug('Zoomed class toggled, current classes:', this.className);
        }
    }
});

logger.debug('cms.imagepreview.js rava bindings registered');
