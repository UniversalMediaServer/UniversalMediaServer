import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Qrcode(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-qrcode",
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
    y: "4",
    width: "6",
    height: "6",
    rx: "1"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "7",
    y1: "17",
    x2: "7",
    y2: "17.01"
  }), /*#__PURE__*/React.createElement("rect", {
    x: "14",
    y: "4",
    width: "6",
    height: "6",
    rx: "1"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "7",
    y1: "7",
    x2: "7",
    y2: "7.01"
  }), /*#__PURE__*/React.createElement("rect", {
    x: "4",
    y: "14",
    width: "6",
    height: "6",
    rx: "1"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "17",
    y1: "7",
    x2: "17",
    y2: "7.01"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "14",
    y1: "14",
    x2: "17",
    y2: "14"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "20",
    y1: "14",
    x2: "20",
    y2: "14.01"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "14",
    y1: "14",
    x2: "14",
    y2: "17"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "14",
    y1: "20",
    x2: "17",
    y2: "20"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "17",
    y1: "17",
    x2: "20",
    y2: "17"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "20",
    y1: "17",
    x2: "20",
    y2: "20"
  }));
}

export { Qrcode as default };
