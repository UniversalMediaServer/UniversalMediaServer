import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function ExchangeOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-exchange-off",
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
    cy: "18",
    r: "2"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "19",
    cy: "6",
    r: "2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M19 8v5c0 .594 -.104 1.164 -.294 1.692m-1.692 2.298a4.978 4.978 0 0 1 -3.014 1.01h-3l3 -3"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14 21l-3 -3"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M5 16v-5c0 -1.632 .782 -3.082 1.992 -3.995m3.008 -1.005h3l-3 -3"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M11.501 7.499l1.499 -1.499"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { ExchangeOff as default };
