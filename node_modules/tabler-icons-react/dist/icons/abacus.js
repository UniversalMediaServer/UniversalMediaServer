import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Abacus(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-abacus",
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
    d: "M5 3v18"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M19 21v-18"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M5 7h14"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M5 15h14"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 13v4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M11 13v4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M16 13v4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14 5v4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M11 5v4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 5v4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 21h18"
  }));
}

export { Abacus as default };
