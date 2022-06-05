import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function BuildingCommunity(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-building-community",
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
    d: "M8 9l5 5v7h-5v-4m0 4h-5v-7l5 -5m1 1v-6a1 1 0 0 1 1 -1h10a1 1 0 0 1 1 1v17h-8"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "13",
    y1: "7",
    x2: "13",
    y2: "7.01"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "17",
    y1: "7",
    x2: "17",
    y2: "7.01"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "17",
    y1: "11",
    x2: "17",
    y2: "11.01"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "17",
    y1: "15",
    x2: "17",
    y2: "15.01"
  }));
}

export { BuildingCommunity as default };
