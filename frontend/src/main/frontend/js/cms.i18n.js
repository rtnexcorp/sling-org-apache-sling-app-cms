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
 * Deletes an i18n entry (key) from all languages
 * @param {HTMLElement} button - The delete button that was clicked
 * @param {string} dictionaryPath - Path to the i18n dictionary
 * @param {string} key - The translation key to delete
 */
function deleteI18nEntry(button, dictionaryPath, key) {
    if (!confirm('Are you sure you want to delete the entry "' + key + '" from all languages?')) {
        return;
    }

    button.disabled = true;
    button.classList.add('is-loading');

    // Get all language folders
    const languageCells = button.closest('tr').querySelectorAll('td[role="group"]');
    const deletions = [];

    // Build delete requests for each language
    languageCells.forEach(cell => {
        const languageCode = cell.getAttribute('aria-labelledby').replace('Column-', '');
        const entryName = cell.querySelector('input[type="text"]')?.name.split('/')[1];

        if (entryName) {
            deletions.push({
                language: languageCode,
                path: dictionaryPath + '/' + languageCode + '/' + entryName
            });
        }
    });

    // Execute deletions
    Promise.all(deletions.map(del =>
        fetch(del.path, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: ':operation=delete'
        })
    ))
    .then(responses => {
        const allSuccess = responses.every(r => r.ok);
        if (allSuccess) {
            // Remove the row from the table
            button.closest('tr').remove();

            // Show success message
            const notification = document.createElement('div');
            notification.className = 'notification is-success';
            notification.textContent = 'Entry "' + key + '" deleted successfully from all languages';
            document.querySelector('.reload-container').prepend(notification);

            setTimeout(() => notification.remove(), 3000);
        } else {
            throw new Error('Some deletions failed');
        }
    })
    .catch(error => {
        alert('Error deleting entry: ' + error.message);
        button.disabled = false;
        button.classList.remove('is-loading');
    });
}

/**
 * Deletes an empty i18n entry for a specific language
 * @param {HTMLElement} button - The delete button that was clicked
 * @param {string} dictionaryPath - Path to the i18n dictionary
 * @param {string} languageName - The language code (e.g., 'en', 'fr')
 * @param {string} entryName - The entry name/ID to delete
 */
function deleteI18nLanguageEntry(button, dictionaryPath, languageName, entryName) {
    if (!confirm('Are you sure you want to delete this empty entry for ' + languageName + '?')) {
        return;
    }

    button.disabled = true;
    button.classList.add('is-loading');

    const entryPath = dictionaryPath + '/' + languageName + '/' + entryName;

    fetch(entryPath, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: ':operation=delete'
    })
    .then(response => {
        if (response.ok) {
            // Hide the delete button and clear the input
            const fieldContainer = button.closest('.field');
            const input = fieldContainer.querySelector('input[type="text"]');
            if (input) {
                input.value = '';
            }
            button.parentElement.remove();

            // Show success message
            const notification = document.createElement('div');
            notification.className = 'notification is-success';
            notification.textContent = 'Empty entry deleted successfully for ' + languageName;
            document.querySelector('.reload-container').prepend(notification);

            setTimeout(() => notification.remove(), 3000);
        } else {
            throw new Error('Deletion failed with status: ' + response.status);
        }
    })
    .catch(error => {
        alert('Error deleting entry: ' + error.message);
        button.disabled = false;
        button.classList.remove('is-loading');
    });
}

// Make functions available globally
window.deleteI18nEntry = deleteI18nEntry;
window.deleteI18nLanguageEntry = deleteI18nLanguageEntry;
