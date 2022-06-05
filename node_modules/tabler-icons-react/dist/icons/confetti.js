import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Confetti(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-confetti",
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
    d: "M4 5h2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M5 4v2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M11.5 4l-.5 2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M18 5h2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M19 4v2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M15 9l-1 1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M18 13l2 -.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M18 19h2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M19 18v2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14 16.518l-6.518 -6.518l-4.39 9.58a1.003 1.003 0 0 0 1.329 1.329l9.579 -4.39z"
  }));
}

export { Confetti as default };
