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

/* eslint-env browser */
rava.bind('.cms-workflow-dashboard-tabs', {
    callbacks: {
        created: function () {
            const tabContainer = this;
            const tabs = tabContainer.querySelectorAll('.tabs li[data-tab]');
            const panels = tabContainer.querySelectorAll('.cms-workflow-dashboard-tabs__panel[data-panel]');

            tabs.forEach(function (tab) {
                tab.addEventListener('click', function (e) {
                    e.preventDefault();
                    const targetPanel = this.getAttribute('data-tab');

                    // Update active tab
                    tabs.forEach(function (t) {
                        t.classList.remove('is-active');
                    });
                    this.classList.add('is-active');

                    // Update active panel
                    panels.forEach(function (p) {
                        p.classList.remove('is-active');
                    });
                    const panel = tabContainer.querySelector('[data-panel="' + targetPanel + '"]');
                    if (panel) {
                        panel.classList.add('is-active');
                    }
                });
            });
        }
    }
});
