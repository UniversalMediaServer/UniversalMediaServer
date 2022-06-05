import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function RobotOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-robot-off",
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
    d: "M11 7h6a2 2 0 0 1 2 2v1l1 1v3l-1 1m-.171 3.811a2 2 0 0 1 -1.829 1.189h-10a2 2 0 0 1 -2 -2v-3l-1 -1v-3l1 -1v-1a2 2 0 0 1 2 -2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M10 16h4"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "8.5",
    cy: "11.5",
    r: "1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M15.854 11.853a0.498 .498 0 0 0 -.354 -.853a0.498 .498 0 0 0 -.356 .149"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8.336 4.343l-.336 -1.343"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M15 7l1 -4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { RobotOff as default };
