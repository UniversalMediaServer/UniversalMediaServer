import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Affiliate(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-affiliate",
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
    d: "M5.931 6.936l1.275 4.249m5.607 5.609l4.251 1.275"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M11.683 12.317l5.759 -5.759"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "5.5",
    cy: "5.5",
    r: "1.5"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "18.5",
    cy: "5.5",
    r: "1.5"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "18.5",
    cy: "18.5",
    r: "1.5"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "8.5",
    cy: "15.5",
    r: "4.5"
  }));
}

export { Affiliate as default };
