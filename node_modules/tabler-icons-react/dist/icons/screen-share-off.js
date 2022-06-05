import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function ScreenShareOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-screen-share-off",
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
    d: "M21 12v3a1 1 0 0 1 -1 1h-16a1 1 0 0 1 -1 -1v-10a1 1 0 0 1 1 -1h9"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "7",
    y1: "20",
    x2: "17",
    y2: "20"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "9",
    y1: "16",
    x2: "9",
    y2: "20"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "15",
    y1: "16",
    x2: "15",
    y2: "20"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M17 8l4 -4m-4 0l4 4"
  }));
}

export { ScreenShareOff as default };
