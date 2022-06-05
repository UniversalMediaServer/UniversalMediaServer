import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function VectorTriangleOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-vector-triangle-off",
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
    d: "M10 6v-1a1 1 0 0 1 1 -1h2a1 1 0 0 1 1 1v2a1 1 0 0 1 -1 1h-1"
  }), /*#__PURE__*/React.createElement("rect", {
    x: "3",
    y: "17",
    width: "4",
    height: "4",
    rx: "1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M20.705 20.709a0.997 .997 0 0 1 -.705 .291h-2a1 1 0 0 1 -1 -1v-2c0 -.28 .115 -.532 .3 -.714"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M6.5 17.1l3.749 -6.823"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M13.158 9.197l-.658 -1.197"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M7 19h10"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { VectorTriangleOff as default };
