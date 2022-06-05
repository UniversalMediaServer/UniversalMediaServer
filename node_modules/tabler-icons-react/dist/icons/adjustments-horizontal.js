import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function AdjustmentsHorizontal(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-adjustments-horizontal",
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
    cx: "14",
    cy: "6",
    r: "2"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "4",
    y1: "6",
    x2: "12",
    y2: "6"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "16",
    y1: "6",
    x2: "20",
    y2: "6"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "8",
    cy: "12",
    r: "2"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "4",
    y1: "12",
    x2: "6",
    y2: "12"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "10",
    y1: "12",
    x2: "20",
    y2: "12"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "17",
    cy: "18",
    r: "2"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "4",
    y1: "18",
    x2: "15",
    y2: "18"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "19",
    y1: "18",
    x2: "20",
    y2: "18"
  }));
}

export { AdjustmentsHorizontal as default };
