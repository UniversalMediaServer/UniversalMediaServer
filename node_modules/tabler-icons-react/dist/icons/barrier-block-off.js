import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function BarrierBlockOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-barrier-block-off",
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
    d: "M11 7h8a1 1 0 0 1 1 1v7c0 .27 -.107 .516 -.282 .696"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M16 16h-11a1 1 0 0 1 -1 -1v-7a1 1 0 0 1 1 -1h2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M7 16v4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M7.5 16l4.244 -4.244"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M13.745 9.755l2.755 -2.755"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M13.5 16l1.249 -1.249"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M16.741 12.759l3.259 -3.259"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M4 13.5l4.752 -4.752"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M17 17v3"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M5 20h4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M15 20h4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M17 7v-2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { BarrierBlockOff as default };
