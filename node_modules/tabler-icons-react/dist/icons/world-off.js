import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function WorldOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-world-off",
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
    d: "M3.6 9h5.4m4 0h7.4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M5.65 5.623a9 9 0 1 0 12.71 12.745m1.684 -2.328a9 9 0 0 0 -12.094 -12.08"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3.6 15h11.4m4 0h1.4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8.335 8.388a18.998 18.998 0 0 0 -.335 3.612c0 4.97 1.79 9 4 9c1.622 0 3.018 -2.172 3.646 -5.294m.354 -3.706c0 -4.97 -1.79 -9 -4 -9c-1.035 0 -1.979 .885 -2.689 2.337"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { WorldOff as default };
