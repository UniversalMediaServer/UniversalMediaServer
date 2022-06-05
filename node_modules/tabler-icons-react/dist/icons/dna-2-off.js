import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Dna2Off(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-dna-2-off",
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
    d: "M17 3v1c-.007 2.46 -.91 4.554 -2.705 6.281m-2.295 1.719c-3.328 1.99 -4.997 4.662 -5.008 8.014v1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M17 21.014v-1c-.004 -1.44 -.315 -2.755 -.932 -3.944m-4.068 -4.07c-1.903 -1.138 -3.263 -2.485 -4.082 -4.068"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 4h9"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M7 20h10"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 8h4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 16h8"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { Dna2Off as default };
