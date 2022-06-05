# tabler-icons-react

[![npm version](https://img.shields.io/npm/v/tabler-icons-react.svg)](https://www.npmjs.com/package/tabler-icons-react)
[![npm downloads](https://img.shields.io/npm/dm/tabler-icons-react.svg)](https://www.npmjs.com/package/tabler-icons-react)

A library of React components for [Tabler Icons](https://github.com/tabler/tabler-icons) — a set of over free 700 open-sourced MIT-licensed icons. 

## Usage

The package is available via npm and can be installed using `npm` or `yarn`:

```sh
# npm
npm install tabler-icons-react

# yarn
yarn add tabler-icons-react
```

After installing the package you can import Tabler Icons as React components as follows:

```jsx
import { Activity } from 'tabler-icons-react';
```

## Example

```jsx
import React from 'react';
import { Activity } from 'tabler-icons-react';

export default function Example() {
  return (
    <div>
      <Activity size={48} color="red" />
    </div>
  );
}
```

## Docs

Every icon component accepts following props:

| Prop  | Default        |
|-------|----------------:|
| `size`  |             `24` |
| `color` | `'currentColor'` |

## License

This project is MIT licensed.