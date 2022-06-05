import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function FloatRight(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-float-right",
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
  }), /*#__PURE__*/React.createElement("rect", {
    width: "6",
    height: "6",
    x: "14",
    y: "5",
    rx: "1"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "4",
    y1: "7",
    x2: "10",
    y2: "7"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "4",
    y1: "11",
    x2: "10",
    y2: "11"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "4",
    y1: "15",
    x2: "20",
    y2: "15"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "4",
    y1: "19",
    x2: "20",
    y2: "19"
  }));
}

export { FloatRight as default };
