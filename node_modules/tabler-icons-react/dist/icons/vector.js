import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Vector(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-vector",
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
    x: "3",
    y: "3",
    width: "4",
    height: "4",
    rx: "1"
  }), /*#__PURE__*/React.createElement("rect", {
    x: "17",
    y: "3",
    width: "4",
    height: "4",
    rx: "1"
  }), /*#__PURE__*/React.createElement("rect", {
    x: "17",
    y: "17",
    width: "4",
    height: "4",
    rx: "1"
  }), /*#__PURE__*/React.createElement("rect", {
    x: "3",
    y: "17",
    width: "4",
    height: "4",
    rx: "1"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "5",
    y1: "7",
    x2: "5",
    y2: "17"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "19",
    y1: "7",
    x2: "19",
    y2: "17"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "7",
    y1: "5",
    x2: "17",
    y2: "5"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "7",
    y1: "19",
    x2: "17",
    y2: "19"
  }));
}

export { Vector as default };
