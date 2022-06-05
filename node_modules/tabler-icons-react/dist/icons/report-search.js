import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function ReportSearch(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-report-search",
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
    d: "M8 5h-2a2 2 0 0 0 -2 2v12a2 2 0 0 0 2 2h5.697"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M18 12v-5a2 2 0 0 0 -2 -2h-2"
  }), /*#__PURE__*/React.createElement("rect", {
    x: "8",
    y: "3",
    width: "6",
    height: "4",
    rx: "2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 11h4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 15h3"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "16.5",
    cy: "17.5",
    r: "2.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M18.5 19.5l2.5 2.5"
  }));
}

export { ReportSearch as default };
