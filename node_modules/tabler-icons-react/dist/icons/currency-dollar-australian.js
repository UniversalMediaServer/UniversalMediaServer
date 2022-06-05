import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function CurrencyDollarAustralian(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-currency-dollar-australian",
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
    d: "M3 18l3.279 -11.476a0.75 .75 0 0 1 1.442 0l3.279 11.476"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M21 6h-4a3 3 0 0 0 0 6h1a3 3 0 0 1 0 6h-4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M17 20v-2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M18 6v-2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M4.5 14h5"
  }));
}

export { CurrencyDollarAustralian as default };
