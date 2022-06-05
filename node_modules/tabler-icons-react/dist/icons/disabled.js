import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Disabled(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-disabled",
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
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "11",
    cy: "5",
    r: "2"
  }), /*#__PURE__*/React.createElement("polyline", {
    points: "11 7 11 15 15 15 19 20"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "11",
    y1: "11",
    x2: "16",
    y2: "11"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M7 11.5a5 5 0 1 0 6 7.5"
  }));
}

export { Disabled as default };
