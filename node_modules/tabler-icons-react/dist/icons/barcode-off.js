import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function BarcodeOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-barcode-off",
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
    d: "M4 7v-1c0 -.552 .224 -1.052 .586 -1.414"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M4 17v1a2 2 0 0 0 2 2h2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M16 4h2a2 2 0 0 1 2 2v1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M16 20h2c.551 0 1.05 -.223 1.412 -.584"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M5 11h1v2h-1z"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M10 11v2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M15 11v.01"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M19 11v2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { BarcodeOff as default };
