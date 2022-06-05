import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function DialpadOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-dialpad-off",
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
    d: "M7 7h-4v-4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M17 3h4v4h-4z"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M10 6v-3h4v4h-3"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 10h4v4h-4z"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M17 13v-3h4v4h-3"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14 14h-4v-4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M10 17h4v4h-4z"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { DialpadOff as default };
