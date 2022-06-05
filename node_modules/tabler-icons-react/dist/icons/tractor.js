import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Tractor(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-tractor",
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
    cx: "7",
    cy: "15",
    r: "4"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "7",
    y1: "15",
    x2: "7",
    y2: "15.01"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "19",
    cy: "17",
    r: "2"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "10.5",
    y1: "17",
    x2: "17",
    y2: "17"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M20 15.2v-4.2a1 1 0 0 0 -1 -1h-6l-2 -5h-6v6.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M18 5h-1a1 1 0 0 0 -1 1v4"
  }));
}

export { Tractor as default };
