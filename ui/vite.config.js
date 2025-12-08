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

import { defineConfig } from 'vite';
import * as sass from 'sass-embedded';
import { resolve } from 'path';
import { readdirSync, readFileSync, readdirSync as readDir, unlinkSync, statSync } from 'fs';

// Apache License header
const apacheLicense = `/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
`;

export default defineConfig({
  root: 'src/main/frontend',
  base: '/static/sling-cms/',
  build: {
    // Output to JCR resources directory
    outDir: resolve(__dirname, 'src/main/resources/jcr_root/static/sling-cms'),
    emptyOutDir: false,
    
    rollupOptions: {
      input: {
        cms: resolve(__dirname, 'src/main/frontend/js/cms-entry.js'),
        editor: resolve(__dirname, 'src/main/frontend/js/editor-entry.js'),
        starter: resolve(__dirname, 'src/main/frontend/js/starter-entry.js'),
      },
      output: {
        // Output format: ES modules for modern browsers (requires type="module" in script tags)
        format: 'es',
        
        // JS bundles
        entryFileNames: 'js/[name].bundle.min.js',
        chunkFileNames: 'js/[name]-[hash].min.js',
        
        // Manual chunks for code splitting
        manualChunks: (id) => {
          // Keep rava and autoComplete in the main bundle (no chunking)
          // This ensures they're available before any code tries to use them
          if (id.includes('node_modules/rava') || id.includes('node_modules/js-autocomplete')) {
            return undefined; // Keep in entry chunk
          }
          
          // TipTap and its extensions in separate chunk
          if (id.includes('@tiptap')) {
            return 'cms.tiptap';
          }
          // Bulma CSS framework in separate chunk
          if (id.includes('bulma')) {
            return 'bulma';
          }
        },
        
        // CSS, fonts, images
        assetFileNames: (assetInfo) => {
          const name = assetInfo.name;
          if (name.endsWith('.css')) {
            return `css/${name.slice(0, -4)}.min.css`;
          }
          if (/\.(woff|woff2|ttf|eot|svg)$/.test(name)) {
            return `fonts/[name].[ext]`;
          }
          if (/\.(jpg|jpeg|png|gif|svg)$/.test(name)) {
            return `img/[name].[ext]`;
          }
          return `assets/[name].[ext]`;
        },
      },
    },
    
    // CSS optimization - Enable code splitting to generate separate CSS files per entry
    cssCodeSplit: true,
    
    // Source maps
    sourcemap: process.env.NODE_ENV !== 'production',
    
    // Minification
    minify: 'terser',
    terserOptions: {
      compress: { drop_console: true },
      format: { comments: false },
    },
  },
  
  css: {
    preprocessorOptions: {
      scss: {
        api: 'modern-compiler',
        silenceDeprecations: ['legacy-js-api'],
      },
    },
  },
  
  plugins: [
    // Clean old chunk files before build
    {
      name: 'clean-old-chunks',
      buildStart() {
        const jsDir = resolve(__dirname, 'src/main/resources/jcr_root/static/sling-cms/js');
        try {
          const files = readdirSync(jsDir);
          // Remove old chunk files (with hash in name)
          files.forEach(file => {
            if (file.includes('-') && file.endsWith('.min.js')) {
              const filePath = resolve(jsDir, file);
              try {
                unlinkSync(filePath);
                console.log(`Cleaned old chunk: ${file}`);
              } catch (e) {
                // Ignore errors for files that can't be deleted
              }
            }
          });
        } catch (e) {
          // Directory doesn't exist yet, ignore
        }
      }
    },
    // Copy Open Sans fonts from source
    {
      name: 'copy-open-sans-fonts',
      generateBundle() {
        try {
          const fontFiles = readdirSync('src/main/frontend/fonts');
          fontFiles.forEach(file => {
            if (/\.(woff2|woff|ttf|eot|svg)$/.test(file)) {
              const source = readFileSync(`src/main/frontend/fonts/${file}`);
              this.emitFile({
                type: 'asset',
                fileName: `fonts/${file}`,
                source,
              });
            }
          });
        } catch (e) {
          console.warn('Warning: Could not copy Open Sans fonts:', e.message);
        }
      },
    },
    // Copy jam-icons fonts from node_modules
    {
      name: 'copy-jam-icons-fonts',
      generateBundle() {
        try {
          const fontFiles = readdirSync('node_modules/jam-icons/fonts');
          fontFiles.forEach(file => {
            const source = readFileSync(`node_modules/jam-icons/fonts/${file}`);
            this.emitFile({
              type: 'asset',
              fileName: `fonts/${file}`,
              source,
            });
          });
        } catch (e) {
          console.warn('Warning: Could not copy jam-icons fonts:', e.message);
        }
      },
    },
    // Copy image assets from source
    {
      name: 'copy-image-assets',
      generateBundle() {
        try {
          const imageFiles = readdirSync('src/main/frontend/img');
          imageFiles.forEach(file => {
            if (/\.(jpg|jpeg|png|gif|svg|ico|webmanifest|xml)$/.test(file)) {
              const source = readFileSync(`src/main/frontend/img/${file}`);
              this.emitFile({
                type: 'asset',
                fileName: `img/${file}`,
                source,
              });
            }
          });
        } catch (e) {
          console.warn('Warning: Could not copy image assets:', e.message);
        }
      },
    },
    
    // Add license headers
    {
      name: 'add-license-headers',
      generateBundle(_, bundle) {
        Object.keys(bundle).forEach(fileName => {
          const file = bundle[fileName];
          if (fileName.endsWith('.css') && file.type === 'asset') {
            file.source = apacheLicense + '\n' + file.source;
          } else if (fileName.endsWith('.js') && file.type === 'asset') {
            file.code = apacheLicense + '\n' + file.code;
          }
        });
      },
    },
  ],
});
