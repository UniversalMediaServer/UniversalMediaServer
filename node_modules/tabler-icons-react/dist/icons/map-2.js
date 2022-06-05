import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Map2(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-map-2",
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
    x1: "18",
    y1: "6",
    x2: "18",
    y2: "6.01"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M18 13l-3.5 -5a4 4 0 1 1 7 0l-3.5 5"
  }), /*#__PURE__*/React.createElement("polyline", {
    points: "10.5 4.75 9 4 3 7 3 20 9 17 15 20 21 17 21 15"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "9",
    y1: "4",
    x2: "9",
    y2: "17"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "15",
    y1: "15",
    x2: "15",
    y2: "20"
  }));
}

export { Map2 as default };
