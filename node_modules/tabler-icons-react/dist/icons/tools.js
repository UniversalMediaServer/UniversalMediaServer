import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Tools(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-tools",
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
    d: "M3 21h4l13 -13a1.5 1.5 0 0 0 -4 -4l-13 13v4"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "14.5",
    y1: "5.5",
    x2: "18.5",
    y2: "9.5"
  }), /*#__PURE__*/React.createElement("polyline", {
    points: "12 8 7 3 3 7 8 12"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "7",
    y1: "8",
    x2: "5.5",
    y2: "9.5"
  }), /*#__PURE__*/React.createElement("polyline", {
    points: "16 12 21 17 17 21 12 16"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "16",
    y1: "17",
    x2: "14.5",
    y2: "18.5"
  }));
}

export { Tools as default };
