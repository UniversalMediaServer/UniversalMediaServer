import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function LifebuoyOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-lifebuoy-off",
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
    d: "M9.171 9.172a4 4 0 0 0 5.65 5.663m1.179 -2.835a4 4 0 0 0 -4 -4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M5.64 5.632a9 9 0 1 0 12.73 12.725m1.667 -2.301a9 9 0 0 0 -12.077 -12.1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M15 15l3.35 3.35"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M9 15l-3.35 3.35"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M5.65 5.65l3.35 3.35"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M18.35 5.65l-3.35 3.35"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { LifebuoyOff as default };
