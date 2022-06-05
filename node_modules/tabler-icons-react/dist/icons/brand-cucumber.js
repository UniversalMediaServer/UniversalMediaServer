import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function BrandCucumber(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-brand-cucumber",
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
    d: "M20 10.99c-.01 5.52 -4.48 10 -10 10.01v-2.26l-.01 -.01c-4.28 -1.11 -6.86 -5.47 -5.76 -9.75a8.001 8.001 0 0 1 9.74 -5.76c3.53 .91 6.03 4.13 6.03 7.78v-.01z"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M10.5 8l-.5 -1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M13.5 14l.5 1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M9 12.5l-1 .5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M11 14l-.5 1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M13 8l.5 -1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M16 12.5l-1 -.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M9 10l-1 -.5"
  }));
}

export { BrandCucumber as default };
