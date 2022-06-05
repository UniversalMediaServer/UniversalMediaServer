import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function ChartCandle(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-chart-candle",
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
  }), /*#__PURE__*/React.createElement("rect", {
    x: "4",
    y: "6",
    width: "4",
    height: "5",
    rx: "1"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "6",
    y1: "4",
    x2: "6",
    y2: "6"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "6",
    y1: "11",
    x2: "6",
    y2: "20"
  }), /*#__PURE__*/React.createElement("rect", {
    x: "10",
    y: "14",
    width: "4",
    height: "5",
    rx: "1"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "12",
    y1: "4",
    x2: "12",
    y2: "14"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "12",
    y1: "19",
    x2: "12",
    y2: "20"
  }), /*#__PURE__*/React.createElement("rect", {
    x: "16",
    y: "5",
    width: "4",
    height: "6",
    rx: "1"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "18",
    y1: "4",
    x2: "18",
    y2: "5"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "18",
    y1: "11",
    x2: "18",
    y2: "20"
  }));
}

export { ChartCandle as default };
