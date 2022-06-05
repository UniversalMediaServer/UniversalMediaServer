import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Old(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-old",
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
    d: "M11 21l-1 -4l-2 -3v-6"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M5 14l-1 -3l4 -3l3 2l3 .5"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "8",
    cy: "4",
    r: "1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M7 17l-2 4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M16 21v-8.5a1.5 1.5 0 0 1 3 0v.5"
  }));
}

export { Old as default };
