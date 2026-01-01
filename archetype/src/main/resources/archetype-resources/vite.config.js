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
import { resolve } from 'path';
import { readdirSync, readFileSync } from 'fs';

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
  root: '.',
  base: '/static/clientlibs/${appName}/',
  build: {
    // Output to target directory that will be included in OSGi bundle
    outDir: resolve(__dirname, 'target/dist/jcr_root/static/clientlibs/${appName}'),
    emptyOutDir: true,
    
    rollupOptions: {
      input: {
        main: resolve(__dirname, 'src/main.js'),
      },
      output: {
        // Output format: ES modules for modern browsers
        format: 'es',
        
        // JS bundles
        entryFileNames: 'js/[name].bundle.min.js',
        chunkFileNames: 'js/[name]-[hash].min.js',
        
        // CSS, fonts, images
        assetFileNames: (assetInfo) => {
          const name = assetInfo.name;
          if (name.endsWith('.css')) {
            return `css/[name].min.css`;
          }
          if (/\.(woff|woff2|ttf|eot|svg)$/.test(name)) {
            return `fonts/[name].[ext]`;
          }
          if (/\.(jpg|jpeg|png|gif|svg|ico)$/.test(name)) {
            return `img/[name].[ext]`;
          }
          return `assets/[name].[ext]`;
        },
      },
    },
    
    // CSS optimization
    cssCodeSplit: true,
    
    // Source maps for development
    sourcemap: process.env.NODE_ENV !== 'production',
    
    // Minification
    minify: 'terser',
    terserOptions: {
      compress: { drop_console: process.env.NODE_ENV === 'production' },
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