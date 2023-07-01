/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
import Prism from 'prism-react-renderer/prism';
(typeof global !== 'undefined' ? global : window).Prism = Prism;
Prism.languages.ums = {
  'string': {
    pattern: /"(?:[^"\\\r\n]|\\.)*"|'(?![st] | \w)(?:[^'\\\r\n]|\\.)*'/,
    greedy: true
  },

  'exception': {
    pattern: /(^|[^\w.])[a-z][\w.]*(?:Error|Exception):.*(?:(?:\r\n?|\n)[ \t]*(?:at[ \t].+|\.{3}.*|Caused by:.*))+(?:(?:\r\n?|\n)[ \t]*\.\.\. .*)?/,
    lookbehind: true,
    greedy: true,
    alias: ['javastacktrace', 'language-javastacktrace'],
    inside: Prism.languages['javastacktrace'] || {
      'keyword': /\bat\b/,
      'function': /[a-z_][\w$]*(?=\()/,
      'punctuation': /[.:()]/
    }
  },

  'level': [
    {
      pattern: /\b(?:ERROR)\b/,
      alias: ['error', 'important']
    },
    {
      pattern: /\b(?:WARN)\b/,
      alias: ['warning', 'important']
    },
    {
      pattern: /\b(?:INFO)\b/,
      alias: ['info', 'keyword']
    },
    {
      pattern: /\b(?:DEBUG)\b/,
      alias: ['debug', 'keyword']
    },
    {
      pattern: /\b(?:TRACE)\b/,
      alias: ['trace', 'comment']
    }
  ],

  'property': {
    pattern: /((?:^|[\]|])[ \t]*)[a-z_](?:[\w-]|\b\/\b)*(?:[. ]\(?\w(?:[\w-]|\b\/\b)*\)?)*:(?=\s)/im,
    lookbehind: true
  },

  'separator': {
    pattern: /(^|[^-+])-{3,}|={3,}|\*{3,}|- - /m,
    lookbehind: true,
    alias: 'comment'
  },

  'url': /\b(?:file|ftp|https?):\/\/[^\s|,;'"]*[^\s|,;'">.]/,
  'email': {
    pattern: /(^|\s)[-\w+.]+@[a-z][a-z0-9-]*(?:\.[a-z][a-z0-9-]*)+(?=\s)/,
    lookbehind: true,
    alias: 'url'
  },

  'ip-address': {
    pattern: /\b(?:\d{1,3}(?:\.\d{1,3}){3})\b/,
    alias: 'constant'
  },
  'mac-address': {
    pattern: /\b[a-f0-9]{2}(?::[a-f0-9]{2}){5}\b/i,
    alias: 'constant'
  },
  'domain': {
    pattern: /(^|\s)[a-z][a-z0-9-]*(?:\.[a-z][a-z0-9-]*)*\.[a-z][a-z0-9-]+(?=\s)/,
    lookbehind: true,
    alias: 'constant'
  },

  'uuid': {
    pattern: /\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\b/i,
    alias: 'constant'
  },
  'hash': {
    pattern: /\b(?:[a-f0-9]{32}){1,2}\b/i,
    alias: 'constant'
  },

  'file-path': {
    // eslint-disable-next-line
    pattern: /\b[a-z]:[\\/][^\s|,;:(){}\[\]"']+|(^|[\s:\[\](>|])\.{0,2}\/\w[^\s|,;:(){}\[\]"']*/i,
    lookbehind: true,
    greedy: true,
    alias: 'string'
  },

  'date': {
    pattern: RegExp(
            /\b\d{4}[-/]\d{2}[-/]\d{2}(?:T(?=\d{1,2}:)|(?=\s\d{1,2}:))/.source +
            '|' +
            /\b\d{1,4}[-/ ](?:\d{1,2}|Apr|Aug|Dec|Feb|Jan|Jul|Jun|Mar|May|Nov|Oct|Sep)[-/ ]\d{2,4}T?\b/.source +
            '|' +
            /\b(?:(?:Fri|Mon|Sat|Sun|Thu|Tue|Wed)(?:\s{1,2}(?:Apr|Aug|Dec|Feb|Jan|Jul|Jun|Mar|May|Nov|Oct|Sep))?|Apr|Aug|Dec|Feb|Jan|Jul|Jun|Mar|May|Nov|Oct|Sep)\s{1,2}\d{1,2}\b/.source,
            'i'
            ),
    alias: 'number'
  },
  'time': {
    pattern: /\b\d{1,2}:\d{1,2}:\d{1,2}(?:[.,:]\d+)?(?:\s?[+-]\d{2}:?\d{2}|Z)?\b/,
    alias: 'number'
  },

  'boolean': /\b(?:false|null|true|none)\b/i,
  'number': {
    pattern: /(^|[^.\w])(?:0x[a-f0-9]+|0o[0-7]+|0b[01]+|v?\d[\da-f]*(?:\.\d+)*(?:e[+-]?\d+)?[a-z]{0,3}\b)\b(?!\.\w)/i,
    lookbehind: true
  },

  'operator': /[;:?<=>~/@!$%&+\-|^(){}*#]/,
  // eslint-disable-next-line
  'punctuation': /[\[\].,]/
};