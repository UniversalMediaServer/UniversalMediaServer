import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function DroneOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-drone-off",
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
    d: "M14 14h-4v-4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M10 10l-3.5 -3.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M9.957 5.95a3.503 3.503 0 0 0 -2.917 -2.91m-3.02 .989a3.5 3.5 0 0 0 1.98 5.936"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14 10l3.5 -3.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M18 9.965a3.5 3.5 0 1 0 -3.966 -3.965"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "14",
    y1: "14",
    x2: "17.5",
    y2: "17.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14.035 18a3.5 3.5 0 0 0 5.936 1.98m.987 -3.026a3.503 3.503 0 0 0 -2.918 -2.913"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "10",
    y1: "14",
    x2: "6.5",
    y2: "17.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M6 14.035a3.5 3.5 0 1 0 3.966 3.965"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "3",
    y1: "3",
    x2: "21",
    y2: "21"
  }));
}

export { DroneOff as default };
