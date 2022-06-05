import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Social(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-social",
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
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "5",
    cy: "19",
    r: "2"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "19",
    cy: "19",
    r: "2"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "12",
    cy: "14",
    r: "3"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "12",
    y1: "7",
    x2: "12",
    y2: "11"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "6.7",
    y1: "17.8",
    x2: "9.5",
    y2: "15.8"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "17.3",
    y1: "17.8",
    x2: "14.5",
    y2: "15.8"
  }));
}

export { Social as default };
