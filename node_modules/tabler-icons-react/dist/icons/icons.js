import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Icons(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-icons",
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
    cx: "6.5",
    cy: "6.5",
    r: "3.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M2.5 21h8l-4 -7z"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14 3l7 7"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14 10l7 -7"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14 14h7v7h-7z"
  }));
}

export { Icons as default };
