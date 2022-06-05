import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Router(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-router",
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
    x: "3",
    y: "13",
    width: "18",
    height: "8",
    rx: "2"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "17",
    y1: "17",
    x2: "17",
    y2: "17.01"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "13",
    y1: "17",
    x2: "13",
    y2: "17.01"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "15",
    y1: "13",
    x2: "15",
    y2: "11"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M11.75 8.75a4 4 0 0 1 6.5 0"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8.5 6.5a8 8 0 0 1 13 0"
  }));
}

export { Router as default };
