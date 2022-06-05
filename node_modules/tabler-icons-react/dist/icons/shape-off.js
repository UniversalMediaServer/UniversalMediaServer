import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function ShapeOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-shape-off",
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
    d: "M3.575 3.597a2 2 0 0 0 2.849 2.808"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "19",
    cy: "5",
    r: "2"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "5",
    cy: "19",
    r: "2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M17.574 17.598a2 2 0 0 0 2.826 2.83"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M5 7v10"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M9 5h8"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M7 19h10"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M19 7v8"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { ShapeOff as default };
