import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function PlantOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-plant-off",
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
    d: "M17 17v2a2 2 0 0 1 -2 2h-6a2 2 0 0 1 -2 -2v-4h8"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M11.9 7.908a6.006 6.006 0 0 0 -4.79 -4.806m-4.11 -.102v2a6 6 0 0 0 6 6h2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12.531 8.528a6.001 6.001 0 0 1 5.469 -3.528h3v1a6.002 6.002 0 0 1 -5.037 5.923"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 15v-3"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { PlantOff as default };
