import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function FileScissors(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-file-scissors",
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
    d: "M14 3v4a1 1 0 0 0 1 1h4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M17 21h-10a2 2 0 0 1 -2 -2v-14a2 2 0 0 1 2 -2h7l5 5v11a2 2 0 0 1 -2 2z"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "15",
    cy: "17",
    r: "1"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "9",
    cy: "17",
    r: "1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M9 17l6 -6"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M15 17l-6 -6"
  }));
}

export { FileScissors as default };
