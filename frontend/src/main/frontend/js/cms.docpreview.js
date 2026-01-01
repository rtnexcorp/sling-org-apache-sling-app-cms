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
 * Document Preview functionality for Apache Sling CMS
 * Handles preview of Word, Excel, PowerPoint, and Text files
 * 
 * Libraries installed via npm (loaded dynamically for code-splitting):
 * - Mammoth.js for Word documents (DOCX)
 * - SheetJS (XLSX) for Excel spreadsheets
 * - JSZip for PowerPoint presentations (PPTX)
 */
import { logger } from './logger.js';

const rava = window.rava;

logger.debug('cms.docpreview.js loaded');

/**
 * Word Document Preview (DOCX)
 * Uses Mammoth.js to convert DOCX to HTML
 */
rava.bind('[data-docpreview="word"]', {
    callbacks: {
        created() {
            logger.debug('Word preview element created:', this);
            const filePath = this.dataset.filepath;
            if (!filePath) {
                logger.error('No filepath for Word preview');
                return;
            }
            initWordPreview(this, filePath);
        }
    }
});

async function initWordPreview(container, filePath) {
    const loadingEl = container.querySelector('.docpreview-loading');
    const contentEl = container.querySelector('.docpreview-content');
    const errorEl = container.querySelector('.docpreview-error');

    try {
        // Dynamic import for code-splitting
        const mammoth = await import('mammoth');
        
        const response = await fetch(filePath);
        const arrayBuffer = await response.arrayBuffer();
        const result = await mammoth.convertToHtml({ arrayBuffer });
        
        if (loadingEl) loadingEl.style.display = 'none';
        if (contentEl) {
            contentEl.innerHTML = result.value;
            contentEl.style.display = 'block';
        }
        logger.debug('Word document loaded successfully');
    } catch (err) {
        logger.error('Word preview error:', err);
        if (loadingEl) loadingEl.style.display = 'none';
        if (errorEl) errorEl.style.display = 'block';
    }
}

/**
 * Excel Spreadsheet Preview (XLSX/XLS)
 * Uses SheetJS to parse and render spreadsheets
 */
rava.bind('[data-docpreview="excel"]', {
    callbacks: {
        created() {
            logger.debug('Excel preview element created:', this);
            const filePath = this.dataset.filepath;
            if (!filePath) {
                logger.error('No filepath for Excel preview');
                return;
            }
            initExcelPreview(this, filePath);
        }
    }
});

async function initExcelPreview(container, filePath) {
    const loadingEl = container.querySelector('.docpreview-loading');
    const tabsEl = container.querySelector('.docpreview-tabs');
    const tabsListEl = container.querySelector('.docpreview-tabs-list');
    const contentEl = container.querySelector('.docpreview-content');
    const errorEl = container.querySelector('.docpreview-error');

    try {
        // Dynamic import for code-splitting
        const XLSX = await import('xlsx');
        
        const response = await fetch(filePath);
        const arrayBuffer = await response.arrayBuffer();
        const workbook = XLSX.read(arrayBuffer, { type: 'array' });
        
        if (loadingEl) loadingEl.style.display = 'none';
        
        // Build sheet tabs if multiple sheets
        if (workbook.SheetNames.length > 1 && tabsListEl) {
            workbook.SheetNames.forEach((name, idx) => {
                const li = document.createElement('li');
                li.dataset.sheet = name;
                if (idx === 0) li.classList.add('is-active');
                
                const a = document.createElement('a');
                a.textContent = name;
                a.href = '#';
                a.onclick = (e) => {
                    e.preventDefault();
                    renderSheet(XLSX, workbook, name, contentEl, tabsListEl);
                };
                li.appendChild(a);
                tabsListEl.appendChild(li);
            });
            if (tabsEl) tabsEl.style.display = 'block';
        }
        
        if (contentEl) {
            contentEl.style.display = 'block';
            renderSheet(XLSX, workbook, workbook.SheetNames[0], contentEl, tabsListEl);
        }
        
        logger.debug('Excel spreadsheet loaded successfully');
    } catch (err) {
        logger.error('Excel preview error:', err);
        if (loadingEl) loadingEl.style.display = 'none';
        if (errorEl) errorEl.style.display = 'block';
    }
}

function renderSheet(XLSX, workbook, sheetName, contentEl, tabsListEl) {
    const html = XLSX.utils.sheet_to_html(workbook.Sheets[sheetName]);
    contentEl.innerHTML = html;
    
    // Style the table
    const table = contentEl.querySelector('table');
    if (table) {
        table.className = 'table is-bordered is-striped is-narrow is-fullwidth';
    }
    
    // Update active tab
    if (tabsListEl) {
        tabsListEl.querySelectorAll('li').forEach(li => {
            li.classList.toggle('is-active', li.dataset.sheet === sheetName);
        });
    }
}

/**
 * PowerPoint Preview (PPTX)
 * Uses JSZip to extract and render slide content
 */
rava.bind('[data-docpreview="powerpoint"]', {
    callbacks: {
        created() {
            logger.debug('PowerPoint preview element created:', this);
            const filePath = this.dataset.filepath;
            if (!filePath) {
                logger.error('No filepath for PowerPoint preview');
                return;
            }
            initPowerPointPreview(this, filePath);
        }
    }
});

async function initPowerPointPreview(container, filePath) {
    const loadingEl = container.querySelector('.docpreview-loading');
    const controlsEl = container.querySelector('.docpreview-controls');
    const contentEl = container.querySelector('.docpreview-content');
    const slideEl = container.querySelector('.docpreview-slide');
    const errorEl = container.querySelector('.docpreview-error');
    const prevBtn = container.querySelector('.docpreview-prev');
    const nextBtn = container.querySelector('.docpreview-next');
    const slideInfo = container.querySelector('.docpreview-slide-info');
    const slideSelect = container.querySelector('.docpreview-slide-select');

    let slides = [];
    let currentSlide = 0;

    function updateControls() {
        if (prevBtn) prevBtn.disabled = currentSlide === 0;
        if (nextBtn) nextBtn.disabled = currentSlide >= slides.length - 1;
        if (slideInfo) slideInfo.textContent = `Slide ${currentSlide + 1} of ${slides.length}`;
        if (slideSelect) slideSelect.value = currentSlide;
    }

    function renderSlide(idx) {
        currentSlide = idx;
        if (slideEl) {
            slideEl.innerHTML = slides[idx] || '<p class="has-text-centered has-text-grey">Empty slide</p>';
        }
        updateControls();
    }

    try {
        // Dynamic import for code-splitting
        const JSZip = await import('jszip');
        
        const response = await fetch(filePath);
        const arrayBuffer = await response.arrayBuffer();
        const zip = await JSZip.default.loadAsync(arrayBuffer);
        
        // Count slides
        let slideCount = 0;
        zip.forEach((path) => {
            if (path.match(/ppt\/slides\/slide\d+\.xml$/)) {
                slideCount++;
            }
        });
        
        // Process each slide
        const slidePromises = [];
        for (let i = 1; i <= slideCount; i++) {
            const slideNum = i;
            const promise = zip.file(`ppt/slides/slide${slideNum}.xml`).async('string')
                .then(xml => {
                    const parser = new DOMParser();
                    const doc = parser.parseFromString(xml, 'text/xml');
                    const texts = [];
                    
                    // Extract text content
                    doc.querySelectorAll('t').forEach(t => {
                        if (t.textContent.trim()) {
                            texts.push(t.textContent);
                        }
                    });
                    
                    return {
                        num: slideNum,
                        html: texts.length > 0
                            ? '<div class="content">' + texts.map(t => `<p>${escapeHtml(t)}</p>`).join('') + '</div>'
                            : '<p class="has-text-centered has-text-grey-light"><em>No text content</em></p>'
                    };
                });
            slidePromises.push(promise);
        }
        
        const slideData = await Promise.all(slidePromises);
        slideData.sort((a, b) => a.num - b.num);
        slides = slideData.map(s => s.html);
        
        if (loadingEl) loadingEl.style.display = 'none';
        
        if (slides.length === 0) {
            slides = ['<p class="has-text-centered has-text-grey">No slides found</p>'];
        }
        
        // Build slide selector
        if (slideSelect) {
            slides.forEach((_, idx) => {
                const opt = document.createElement('option');
                opt.value = idx;
                opt.textContent = `Slide ${idx + 1}`;
                slideSelect.appendChild(opt);
            });
            slideSelect.onchange = () => renderSlide(parseInt(slideSelect.value, 10));
        }
        
        // Wire up navigation
        if (prevBtn) prevBtn.onclick = () => { if (currentSlide > 0) renderSlide(currentSlide - 1); };
        if (nextBtn) nextBtn.onclick = () => { if (currentSlide < slides.length - 1) renderSlide(currentSlide + 1); };
        
        if (controlsEl) controlsEl.style.display = 'flex';
        if (contentEl) contentEl.style.display = 'block';
        renderSlide(0);
        
        logger.debug('PowerPoint loaded successfully');
    } catch (err) {
        logger.error('PowerPoint preview error:', err);
        if (loadingEl) loadingEl.style.display = 'none';
        if (errorEl) errorEl.style.display = 'block';
    }
}

/**
 * Text File Preview
 */
rava.bind('[data-docpreview="text"]', {
    callbacks: {
        created() {
            logger.debug('Text preview element created:', this);
            const filePath = this.dataset.filepath;
            if (!filePath) {
                logger.error('No filepath for Text preview');
                return;
            }
            initTextPreview(this, filePath);
        }
    }
});

async function initTextPreview(container, filePath) {
    const loadingEl = container.querySelector('.docpreview-loading');
    const contentEl = container.querySelector('.docpreview-content');
    const errorEl = container.querySelector('.docpreview-error');

    try {
        const response = await fetch(filePath);
        const text = await response.text();
        
        if (loadingEl) loadingEl.style.display = 'none';
        if (contentEl) {
            contentEl.textContent = text;
            contentEl.style.display = 'block';
        }
        logger.debug('Text file loaded successfully');
    } catch (err) {
        logger.error('Text preview error:', err);
        if (loadingEl) loadingEl.style.display = 'none';
        if (errorEl) errorEl.style.display = 'block';
    }
}

/**
 * Escape HTML special characters
 */
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

logger.debug('cms.docpreview.js rava bindings registered');
