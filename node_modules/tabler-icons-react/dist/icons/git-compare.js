import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function GitCompare(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-git-compare",
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
    cx: "6",
    cy: "6",
    r: "2"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "18",
    cy: "18",
    r: "2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M11 6h5a2 2 0 0 1 2 2v8"
  }), /*#__PURE__*/React.createElement("polyline", {
    points: "14 9 11 6 14 3"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M13 18h-5a2 2 0 0 1 -2 -2v-8"
  }), /*#__PURE__*/React.createElement("polyline", {
    points: "10 15 13 18 10 21"
  }));
}

export { GitCompare as default };
