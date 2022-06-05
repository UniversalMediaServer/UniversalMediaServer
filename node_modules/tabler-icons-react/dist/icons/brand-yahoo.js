import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function BrandYahoo(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-brand-yahoo",
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
    y1: "6",
    x2: "8",
    y2: "6"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "7",
    y1: "18",
    x2: "14",
    y2: "18"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M4.5 6l5.5 7v5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M10 13l6 -5"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "12.5",
    y1: "8",
    x2: "17.5",
    y2: "8"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "20",
    y1: "11",
    x2: "20",
    y2: "15"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "20",
    y1: "18",
    x2: "20",
    y2: "18.01"
  }));
}

export { BrandYahoo as default };
