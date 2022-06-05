import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function GenderTransgender(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-gender-transgender",
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
    cx: "12",
    cy: "12",
    r: "4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M15 9l6 -6"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M21 7v-4h-4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M9 9l-6 -6"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 7v-4h4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M5.5 8.5l3 -3"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 16v5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M9.5 19h5"
  }));
}

export { GenderTransgender as default };
