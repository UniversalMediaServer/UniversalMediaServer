import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function CpuOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-cpu-off",
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
    d: "M9 5h9a1 1 0 0 1 1 1v9m-.292 3.706a0.997 .997 0 0 1 -.708 .294h-12a1 1 0 0 1 -1 -1v-12c0 -.272 .108 -.518 .284 -.698"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M13 9h2v2m0 4h-6v-6"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 10h2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 14h2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M10 3v2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14 3v2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M21 10h-2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M21 14h-2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14 21v-2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M10 21v-2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { CpuOff as default };
