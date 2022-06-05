import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function BrandDocker(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-brand-docker",
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
  }), /*#__PURE__*/React.createElement("path", {
    d: "M22 12.54c-1.804 -.345 -2.701 -1.08 -3.523 -2.94c-.487 .696 -1.102 1.568 -.92 2.4c.028 .238 -.32 1.002 -.557 1h-14c0 5.208 3.164 7 6.196 7c4.124 .022 7.828 -1.376 9.854 -5c1.146 -.101 2.296 -1.505 2.95 -2.46z"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M5 10h3v3h-3z"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 10h3v3h-3z"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M11 10h3v3h-3z"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 7h3v3h-3z"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M11 7h3v3h-3z"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M11 4h3v3h-3z"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M4.571 18c1.5 0 2.047 -.074 2.958 -.78"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "10",
    y1: "16",
    x2: "10",
    y2: "16.01"
  }));
}

export { BrandDocker as default };
