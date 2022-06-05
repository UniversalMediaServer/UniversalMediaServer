import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Atom2(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-atom-2",
    width: size,
    height: size,
    viewBox: "0 0 24 24",
    stroke: color,
    strokeWidth: "2",
    fill: "none",
    strokeLinecap: "round",
    strokeLinejoin: "round"
  }, restProps), /*#__PURE__*/React.createElement("path", {
    stroke: "none",
    d: "M0 0h24v24H0z",
    fill: "none"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "12",
    cy: "12",
    r: "3"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "12",
    y1: "21",
    x2: "12",
    y2: "21.01"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "3",
    y1: "9",
    x2: "3",
    y2: "9.01"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "21",
    y1: "9",
    x2: "21",
    y2: "9.01"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 20.1a9 9 0 0 1 -5 -7.1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M16 20.1a9 9 0 0 0 5 -7.1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M6.2 5a9 9 0 0 1 11.4 0"
  }));
}

export { Atom2 as default };
