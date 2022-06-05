import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function ArrowsMaximize(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-arrows-maximize",
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
  }), /*#__PURE__*/React.createElement("polyline", {
    points: "16 4 20 4 20 8"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "14",
    y1: "10",
    x2: "20",
    y2: "4"
  }), /*#__PURE__*/React.createElement("polyline", {
    points: "8 20 4 20 4 16"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "4",
    y1: "20",
    x2: "10",
    y2: "14"
  }), /*#__PURE__*/React.createElement("polyline", {
    points: "16 20 20 20 20 16"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "14",
    y1: "14",
    x2: "20",
    y2: "20"
  }), /*#__PURE__*/React.createElement("polyline", {
    points: "8 4 4 4 4 8"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "4",
    y1: "4",
    x2: "10",
    y2: "10"
  }));
}

export { ArrowsMaximize as default };
