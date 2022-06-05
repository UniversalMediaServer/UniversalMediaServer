import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function BasketOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-basket-off",
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
    d: "M7 10l1.359 -1.63"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M10.176 6.188l1.824 -2.188l5 6"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M18.77 18.757c-.358 .768 -1.027 1.262 -1.77 1.243h-10c-.966 .024 -1.807 -.817 -2 -2l-2 -8h7"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14 10h7l-1.397 5.587"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "12",
    cy: "15",
    r: "2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { BasketOff as default };
