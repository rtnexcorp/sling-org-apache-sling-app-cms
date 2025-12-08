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

// Import dependencies FIRST and make them globally available
import rava from 'rava';
import autoComplete from 'js-autocomplete';

// Make dependencies globally available BEFORE any modules that use them
window.rava = rava;
window.autoComplete = autoComplete;

// Import SCSS (will be bundled into CSS)
import '../scss/cms.scss';

// Import CSS dependencies
import 'bulma/css/bulma.min.css';
import 'jam-icons/css/jam.min.css';

// Import TipTap bundle
import './tiptap-bundle.js';

// Import CMS modules (these can now safely use window.rava and window.autoComplete)
import './cms-module.js';
import './cms.draggable.js';
import './cms.fields.js';
import './cms.form.js';
import './cms.job.js';
import './cms.labelfield.js';
import './cms.modal.js';
import './cms.nav.js';
import './cms.page.js';
import './cms.pathfield.js';
import './cms.tiptap.js';
import './cms.toggle.js';

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
  console.log('CMS initialized with Vite');
});
