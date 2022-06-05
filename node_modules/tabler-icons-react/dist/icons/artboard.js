import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Artboard(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-artboard",
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
    x: "8",
    y: "8",
    width: "8",
    height: "8",
    rx: "1"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "3",
    y1: "8",
    x2: "4",
    y2: "8"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "3",
    y1: "16",
    x2: "4",
    y2: "16"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "8",
    y1: "3",
    x2: "8",
    y2: "4"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "16",
    y1: "3",
    x2: "16",
    y2: "4"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "20",
    y1: "8",
    x2: "21",
    y2: "8"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "20",
    y1: "16",
    x2: "21",
    y2: "16"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "8",
    y1: "20",
    x2: "8",
    y2: "21"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "16",
    y1: "20",
    x2: "16",
    y2: "21"
  }));
}

export { Artboard as default };
