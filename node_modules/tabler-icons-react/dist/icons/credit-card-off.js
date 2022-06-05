import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function CreditCardOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-credit-card-off",
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
  }), /*#__PURE__*/React.createElement("path", {
    d: "M9 5h9a3 3 0 0 1 3 3v8a3 3 0 0 1 -.128 .87"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M18.87 18.872a3 3 0 0 1 -.87 .128h-12a3 3 0 0 1 -3 -3v-8c0 -1.352 .894 -2.495 2.124 -2.87"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "3",
    y1: "11",
    x2: "11",
    y2: "11"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "15",
    y1: "11",
    x2: "21",
    y2: "11"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "7",
    y1: "15",
    x2: "7.01",
    y2: "15"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "11",
    y1: "15",
    x2: "13",
    y2: "15"
  }));
}

export { CreditCardOff as default };
