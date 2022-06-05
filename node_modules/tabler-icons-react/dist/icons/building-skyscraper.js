import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function BuildingSkyscraper(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-building-skyscraper",
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
    x1: "3",
    y1: "21",
    x2: "21",
    y2: "21"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M5 21v-14l8 -4v18"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M19 21v-10l-6 -4"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "9",
    y1: "9",
    x2: "9",
    y2: "9.01"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "9",
    y1: "12",
    x2: "9",
    y2: "12.01"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "9",
    y1: "15",
    x2: "9",
    y2: "15.01"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "9",
    y1: "18",
    x2: "9",
    y2: "18.01"
  }));
}

export { BuildingSkyscraper as default };
