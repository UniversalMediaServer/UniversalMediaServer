import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Dna2(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-dna-2",
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
    d: "M17 3v1c-.01 3.352 -1.68 6.023 -5.008 8.014c-3.328 1.99 3.336 -2.005 .008 -.014c-3.328 1.99 -4.997 4.662 -5.008 8.014v1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M17 21.014v-1c-.01 -3.352 -1.68 -6.023 -5.008 -8.014c-3.328 -1.99 3.336 2.005 .008 .014c-3.328 -1.991 -4.997 -4.662 -5.008 -8.014v-1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M7 4h10"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M7 20h10"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 8h8"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 16h8"
  }));
}

export { Dna2 as default };
