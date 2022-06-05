import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Certificate(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-certificate",
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
    cx: "15",
    cy: "15",
    r: "3"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M13 17.5v4.5l2 -1.5l2 1.5v-4.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M10 19h-5a2 2 0 0 1 -2 -2v-10c0 -1.1 .9 -2 2 -2h14a2 2 0 0 1 2 2v10a2 2 0 0 1 -1 1.73"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "6",
    y1: "9",
    x2: "18",
    y2: "9"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "6",
    y1: "12",
    x2: "9",
    y2: "12"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "6",
    y1: "15",
    x2: "8",
    y2: "15"
  }));
}

export { Certificate as default };
