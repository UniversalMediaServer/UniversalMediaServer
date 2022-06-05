import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function BuildingStore(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-building-store",
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
    y1: "21",
    x2: "21",
    y2: "21"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 7v1a3 3 0 0 0 6 0v-1m0 1a3 3 0 0 0 6 0v-1m0 1a3 3 0 0 0 6 0v-1h-18l2 -4h14l2 4"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "5",
    y1: "21",
    x2: "5",
    y2: "10.85"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "19",
    y1: "21",
    x2: "19",
    y2: "10.85"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M9 21v-4a2 2 0 0 1 2 -2h2a2 2 0 0 1 2 2v4"
  }));
}

export { BuildingStore as default };
