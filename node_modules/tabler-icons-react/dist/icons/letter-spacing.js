import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function LetterSpacing(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-letter-spacing",
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
    d: "M5 12v-5.5a2.5 2.5 0 0 1 5 0v5.5m0 -4h-5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M13 4l3 8l3 -8"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M5 18h14"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M17 20l2 -2l-2 -2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M7 16l-2 2l2 2"
  }));
}

export { LetterSpacing as default };
