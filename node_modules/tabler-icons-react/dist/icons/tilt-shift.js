import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function TiltShift(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-tilt-shift",
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
    d: "M8.56 3.69a9 9 0 0 0 -2.92 1.95"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3.69 8.56a9 9 0 0 0 -.69 3.44"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3.69 15.44a9 9 0 0 0 1.95 2.92"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8.56 20.31a9 9 0 0 0 3.44 .69"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M15.44 20.31a9 9 0 0 0 2.92 -1.95"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M20.31 15.44a9 9 0 0 0 .69 -3.44"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M20.31 8.56a9 9 0 0 0 -1.95 -2.92"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M15.44 3.69a9 9 0 0 0 -3.44 -.69"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "12",
    cy: "12",
    r: "2"
  }));
}

export { TiltShift as default };
