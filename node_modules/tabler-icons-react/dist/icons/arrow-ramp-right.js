import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function ArrowRampRight(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-arrow-ramp-right",
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
  }), /*#__PURE__*/React.createElement("line", {
    x1: "7",
    y1: "3",
    x2: "7",
    y2: "11.707"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M11 7l-4 -4l-4 4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M17 14l4 -4l-4 -4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M7 21a11 11 0 0 1 11 -11h3"
  }));
}

export { ArrowRampRight as default };
