import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function MathFunctionOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-math-function-off",
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
    d: "M14 10h1c.882 0 .986 .777 1.694 2.692"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M13 17c.864 0 1.727 -.663 2.495 -1.512m1.717 -2.302c.993 -1.45 2.39 -3.186 3.788 -3.186"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 19c0 1.5 .5 2 2 2s2 -4 3 -9c.237 -1.186 .446 -2.317 .647 -3.35m.727 -3.248c.423 -1.492 .91 -2.402 1.626 -2.402c1.5 0 2 .5 2 2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M5 12h6"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { MathFunctionOff as default };
