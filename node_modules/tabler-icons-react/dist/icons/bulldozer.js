import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Bulldozer(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-bulldozer",
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
    cx: "4",
    cy: "17",
    r: "2"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "13",
    cy: "17",
    r: "2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M18 13v6h3"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "13",
    y1: "19",
    x2: "4",
    y2: "19"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "4",
    y1: "15",
    x2: "13",
    y2: "15"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 12v-5h2a3 3 0 0 1 3 3v5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M5 15v-2a1 1 0 0 1 1 -1h7"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "18",
    y1: "17",
    x2: "15",
    y2: "17"
  }));
}

export { Bulldozer as default };
