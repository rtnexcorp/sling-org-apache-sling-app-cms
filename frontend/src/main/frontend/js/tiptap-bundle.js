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
 * TipTap Editor Bundle for Apache Sling CMS
 * This file bundles all required TipTap extensions into a single file
 */

import { Editor } from '@tiptap/core';
import Document from '@tiptap/extension-document';
import Paragraph from '@tiptap/extension-paragraph';
import Text from '@tiptap/extension-text';
import Bold from '@tiptap/extension-bold';
import Italic from '@tiptap/extension-italic';
import Underline from '@tiptap/extension-underline';
import Heading from '@tiptap/extension-heading';
import BulletList from '@tiptap/extension-bullet-list';
import OrderedList from '@tiptap/extension-ordered-list';
import ListItem from '@tiptap/extension-list-item';
import Blockquote from '@tiptap/extension-blockquote';
import CodeBlock from '@tiptap/extension-code-block';
import HardBreak from '@tiptap/extension-hard-break';
import History from '@tiptap/extension-history';
import Link from '@tiptap/extension-link';
import Image from '@tiptap/extension-image';
import TextAlign from '@tiptap/extension-text-align';

/**
 * Default TipTap extensions for Sling CMS RTE
 */
const defaultExtensions = [
  Document,
  Paragraph,
  Text,
  Bold,
  Italic,
  Underline,
  Heading.configure({
    levels: [1, 2, 3, 4, 5, 6]
  }),
  BulletList,
  OrderedList,
  ListItem,
  Blockquote,
  CodeBlock,
  HardBreak,
  History,
  Link.configure({
    openOnClick: false,
    HTMLAttributes: {
      rel: 'noopener noreferrer'
    }
  }),
  Image.configure({
    inline: false,
    allowBase64: false
  }),
  TextAlign.configure({
    types: ['heading', 'paragraph']
  })
];

/**
 * Create a TipTap editor instance with default Sling CMS configuration
 */
function createEditor(options = {}) {
  const { element, content = '', onUpdate, onSelectionUpdate, onTransaction, extensions = [] } = options;
  
  return new Editor({
    element: element,
    content: content,
    extensions: [...defaultExtensions, ...extensions],
    editorProps: {
      attributes: {
        class: 'tiptap-editor ProseMirror'
      }
    },
    onUpdate: onUpdate,
    onSelectionUpdate: onSelectionUpdate,
    onTransaction: onTransaction
  });
}

// Export for global access
window.TiptapEditor = {
  create: createEditor,
  Editor: Editor,
  extensions: {
    Document,
    Paragraph,
    Text,
    Bold,
    Italic,
    Underline,
    Heading,
    BulletList,
    OrderedList,
    ListItem,
    Blockquote,
    CodeBlock,
    HardBreak,
    History,
    Link,
    Image,
    TextAlign
  }
};

export { createEditor, Editor };
