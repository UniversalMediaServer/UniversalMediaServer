import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Multiplier15x(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-multiplier-1-5x",
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
    d: "M4 16v-8l-2 2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M10 16h2a2 2 0 1 0 0 -4h-2v-4h4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M7 16v.01"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M17 16l4 -4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M21 16l-4 -4"
  }));
}

export { Multiplier15x as default };
