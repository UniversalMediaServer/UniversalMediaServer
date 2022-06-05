import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Forklift(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-forklift",
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
    cx: "5",
    cy: "17",
    r: "2"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "14",
    cy: "17",
    r: "2"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "7",
    y1: "17",
    x2: "12",
    y2: "17"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 17v-6h13v6"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M5 11v-4h4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M9 11v-6h4l3 6"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M22 15h-3v-10"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "16",
    y1: "13",
    x2: "19",
    y2: "13"
  }));
}

export { Forklift as default };
