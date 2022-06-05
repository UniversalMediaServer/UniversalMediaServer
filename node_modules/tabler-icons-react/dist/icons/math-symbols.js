import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function MathSymbols(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-math-symbols",
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
    y1: "12",
    x2: "21",
    y2: "12"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "12",
    y1: "3",
    x2: "12",
    y2: "21"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "16.5",
    y1: "4.5",
    x2: "19.5",
    y2: "7.5"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "19.5",
    y1: "4.5",
    x2: "16.5",
    y2: "7.5"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "6",
    y1: "4",
    x2: "6",
    y2: "8"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "4",
    y1: "6",
    x2: "8",
    y2: "6"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "18",
    y1: "16",
    x2: "18.01",
    y2: "16"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "18",
    y1: "20",
    x2: "18.01",
    y2: "20"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "4",
    y1: "18",
    x2: "8",
    y2: "18"
  }));
}

export { MathSymbols as default };
