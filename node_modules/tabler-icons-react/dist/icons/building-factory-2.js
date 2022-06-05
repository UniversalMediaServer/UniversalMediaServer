import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function BuildingFactory2(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-building-factory-2",
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
    d: "M3 21h18"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M5 21v-12l5 4v-4l5 4h4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M19 21v-8l-1.436 -9.574a0.5 .5 0 0 0 -.495 -.426h-1.145a0.5 .5 0 0 0 -.494 .418l-1.43 8.582"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M9 17h1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14 17h1"
  }));
}

export { BuildingFactory2 as default };
