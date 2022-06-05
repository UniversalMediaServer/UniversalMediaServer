import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function MapPins(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-map-pins",
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
    d: "M10.828 9.828a4 4 0 1 0 -5.656 0l2.828 2.829l2.828 -2.829z"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "8",
    y1: "7",
    x2: "8",
    y2: "7.01"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M18.828 17.828a4 4 0 1 0 -5.656 0l2.828 2.829l2.828 -2.829z"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "16",
    y1: "15",
    x2: "16",
    y2: "15.01"
  }));
}

export { MapPins as default };
