import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Barcode(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-barcode",
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
    d: "M4 7v-1a2 2 0 0 1 2 -2h2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M4 17v1a2 2 0 0 0 2 2h2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M16 4h2a2 2 0 0 1 2 2v1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M16 20h2a2 2 0 0 0 2 -2v-1"
  }), /*#__PURE__*/React.createElement("rect", {
    x: "5",
    y: "11",
    width: "1",
    height: "2"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "10",
    y1: "11",
    x2: "10",
    y2: "13"
  }), /*#__PURE__*/React.createElement("rect", {
    x: "14",
    y: "11",
    width: "1",
    height: "2"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "19",
    y1: "11",
    x2: "19",
    y2: "13"
  }));
}

export { Barcode as default };
