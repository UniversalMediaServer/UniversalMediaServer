import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Swords(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-swords",
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
    d: "M21 3v5l-11 9l-4 4l-3 -3l4 -4l9 -11z"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M5 13l6 6"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14.32 17.32l3.68 3.68l3 -3l-3.365 -3.365"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M10 5.5l-2 -2.5h-5v5l3 2.5"
  }));
}

export { Swords as default };
