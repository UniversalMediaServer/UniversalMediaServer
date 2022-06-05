import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function ChartDots2(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-chart-dots-2",
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
    d: "M3 3v18h18"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "9",
    cy: "15",
    r: "2"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "13",
    cy: "5",
    r: "2"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "18",
    cy: "12",
    r: "2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M21 3l-6 1.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14.113 6.65l2.771 3.695"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M16 12.5l-5 2"
  }));
}

export { ChartDots2 as default };
