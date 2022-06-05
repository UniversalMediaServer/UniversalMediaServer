import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function ChargingPile(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-charging-pile",
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
    y1: "7",
    x2: "17",
    y2: "8"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14 11h1a2 2 0 0 1 2 2v3a1.5 1.5 0 0 0 3 0v-7l-3 -3"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M4 20v-14a2 2 0 0 1 2 -2h6a2 2 0 0 1 2 2v14"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M9 11.5l-1.5 2.5h3l-1.5 2.5"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "3",
    y1: "20",
    x2: "15",
    y2: "20"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "4",
    y1: "8",
    x2: "14",
    y2: "8"
  }));
}

export { ChargingPile as default };
