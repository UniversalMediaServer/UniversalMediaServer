import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function CalculatorOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-calculator-off",
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
    d: "M19.823 19.824a2 2 0 0 1 -1.823 1.176h-12a2 2 0 0 1 -2 -2v-14c0 -.295 .064 -.575 .178 -.827m2.822 -1.173h11a2 2 0 0 1 2 2v11"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M10 10h-1a1 1 0 0 1 -1 -1v-1m3 -1h4a1 1 0 0 1 1 1v1a1 1 0 0 1 -1 1h-1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 14v.01"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 14v.01"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 17v.01"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 17v.01"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M16 17v.01"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { CalculatorOff as default };
