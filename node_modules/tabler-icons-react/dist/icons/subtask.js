import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Subtask(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-subtask",
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
    x1: "6",
    y1: "9",
    x2: "12",
    y2: "9"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "4",
    y1: "5",
    x2: "8",
    y2: "5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M6 5v11a1 1 0 0 0 1 1h5"
  }), /*#__PURE__*/React.createElement("rect", {
    x: "12",
    y: "7",
    width: "8",
    height: "4",
    rx: "1"
  }), /*#__PURE__*/React.createElement("rect", {
    x: "12",
    y: "15",
    width: "8",
    height: "4",
    rx: "1"
  }));
}

export { Subtask as default };
