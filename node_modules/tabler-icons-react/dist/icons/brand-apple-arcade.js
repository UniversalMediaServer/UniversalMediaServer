import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function BrandAppleArcade(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-brand-apple-arcade",
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
    cx: "12",
    cy: "5",
    r: "2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M20 12.5v4.75a0.734 .734 0 0 1 -.055 .325a0.704 .704 0 0 1 -.348 .366l-5.462 2.58a4.998 4.998 0 0 1 -4.27 0l-5.462 -2.58a0.705 .705 0 0 1 -.401 -.691l-.002 -4.75"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M4.431 12.216l5.634 -2.332a5.065 5.065 0 0 1 3.87 0l5.634 2.332a0.692 .692 0 0 1 .028 1.269l-5.462 2.543a5.064 5.064 0 0 1 -4.27 0l-5.462 -2.543a0.691 .691 0 0 1 .028 -1.27z"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "12",
    y1: "7",
    x2: "12",
    y2: "13"
  }));
}

export { BrandAppleArcade as default };
