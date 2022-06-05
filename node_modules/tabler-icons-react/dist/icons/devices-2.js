import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Devices2(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-devices-2",
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
    d: "M10 15h-6a1 1 0 0 1 -1 -1v-8a1 1 0 0 1 1 -1h6"
  }), /*#__PURE__*/React.createElement("rect", {
    x: "13",
    y: "4",
    width: "8",
    height: "16",
    rx: "1"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "7",
    y1: "19",
    x2: "10",
    y2: "19"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "17",
    y1: "8",
    x2: "17",
    y2: "8.01"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "17",
    cy: "16",
    r: "1"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "9",
    y1: "15",
    x2: "9",
    y2: "19"
  }));
}

export { Devices2 as default };
