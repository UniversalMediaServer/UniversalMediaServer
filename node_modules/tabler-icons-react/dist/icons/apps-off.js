import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function AppsOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-apps-off",
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
    d: "M8 4h1a1 1 0 0 1 1 1v1m-.29 3.704a0.997 .997 0 0 1 -.71 .296h-4a1 1 0 0 1 -1 -1v-4c0 -.276 .111 -.525 .292 -.706"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M18 14h1a1 1 0 0 1 1 1v1m-.29 3.704a0.997 .997 0 0 1 -.71 .296h-4a1 1 0 0 1 -1 -1v-4c0 -.276 .111 -.525 .292 -.706"
  }), /*#__PURE__*/React.createElement("rect", {
    x: "4",
    y: "14",
    width: "6",
    height: "6",
    rx: "1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14 7h6"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M17 4v6"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { AppsOff as default };
