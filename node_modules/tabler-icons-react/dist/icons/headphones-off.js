import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function HeadphonesOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-headphones-off",
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
  }), /*#__PURE__*/React.createElement("line", {
    x1: "3",
    y1: "3",
    x2: "21",
    y2: "21"
  }), /*#__PURE__*/React.createElement("rect", {
    x: "4",
    y: "13",
    width: "5",
    height: "7",
    rx: "2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M17 13h1a2 2 0 0 1 2 2v1m-.589 3.417c-.361 .36 -.86 .583 -1.411 .583h-1a2 2 0 0 1 -2 -2v-3"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M4 15v-3c0 -2.21 .896 -4.21 2.344 -5.658m2.369 -1.638a8 8 0 0 1 11.287 7.296v3"
  }));
}

export { HeadphonesOff as default };
