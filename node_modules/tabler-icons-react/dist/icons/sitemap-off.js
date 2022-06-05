import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function SitemapOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-sitemap-off",
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
  }), /*#__PURE__*/React.createElement("rect", {
    x: "3",
    y: "15",
    width: "6",
    height: "6",
    rx: "2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M19 15a2 2 0 0 1 2 2m-.591 3.42c-.362 .358 -.86 .58 -1.409 .58h-2a2 2 0 0 1 -2 -2v-2c0 -.549 .221 -1.046 .579 -1.407"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M9 5a2 2 0 0 1 2 -2h2a2 2 0 0 1 2 2v2a2 2 0 0 1 -2 2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M6 15v-1a2 2 0 0 1 2 -2h4m4 0a2 2 0 0 1 2 2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { SitemapOff as default };
