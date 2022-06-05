import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Drone(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-drone",
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
    d: "M10 10h4v4h-4z"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "10",
    y1: "10",
    x2: "6.5",
    y2: "6.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M9.96 6a3.5 3.5 0 1 0 -3.96 3.96"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14 10l3.5 -3.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M18 9.96a3.5 3.5 0 1 0 -3.96 -3.96"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "14",
    y1: "14",
    x2: "17.5",
    y2: "17.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14.04 18a3.5 3.5 0 1 0 3.96 -3.96"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "10",
    y1: "14",
    x2: "6.5",
    y2: "17.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M6 14.04a3.5 3.5 0 1 0 3.96 3.96"
  }));
}

export { Drone as default };
