import tsPlugin from '@typescript-eslint/eslint-plugin'
import tsParser from '@typescript-eslint/parser';
import tseslint from 'typescript-eslint';
import reactPlugin from 'eslint-plugin-react';
import hooksPlugin from 'eslint-plugin-react-hooks';
import refreshPlugin from 'eslint-plugin-react-refresh';

export default tseslint.config(
        {
          ignores: [
            '.yarn/**',
            'node/**',
            'node_modules/**'
          ]
        },
        {
          files: ['**/*.{js,mjs,cjs,ts,tsx}'],
          languageOptions: {
            parser: tsParser,
            parserOptions: {
              sourceType: 'module',
              ecmaVersion: 2021
            }
          },
          plugins: {
            '@typescript-eslint': tsPlugin,
            'react': reactPlugin,
            'react-hooks': hooksPlugin,
            'react-refresh': refreshPlugin
          },
          rules: {
            ...tsPlugin.configs['eslint-recommended'].rules,
            ...tsPlugin.configs.recommended.rules,
            '@typescript-eslint/no-empty-function': 'off',
            '@typescript-eslint/no-unused-vars': 'off',
            '@typescript-eslint/no-explicit-any': 'off',
            ...reactPlugin.configs['jsx-runtime'].rules,
            ...hooksPlugin.configs.recommended.rules,
            'react-hooks/exhaustive-deps': 'off',
            'react-refresh/only-export-components': [
              'warn',
              {allowConstantExport: true}
            ]
          },
          settings: {
            react: {
              version: 'detect'
            }
          }
        }
)