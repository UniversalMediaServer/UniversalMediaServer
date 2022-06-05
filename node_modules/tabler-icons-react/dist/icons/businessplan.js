import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Businessplan(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-businessplan",
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
  }), /*#__PURE__*/React.createElement("ellipse", {
    cx: "16",
    cy: "6",
    rx: "5",
    ry: "3"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M11 6v4c0 1.657 2.239 3 5 3s5 -1.343 5 -3v-4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M11 10v4c0 1.657 2.239 3 5 3s5 -1.343 5 -3v-4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M11 14v4c0 1.657 2.239 3 5 3s5 -1.343 5 -3v-4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M7 9h-2.5a1.5 1.5 0 0 0 0 3h1a1.5 1.5 0 0 1 0 3h-2.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M5 15v1m0 -8v1"
  }));
}

export { Businessplan as default };
