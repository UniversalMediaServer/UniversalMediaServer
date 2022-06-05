import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Grill(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-grill",
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
    d: "M19 8h-14a6 6 0 0 0 6 6h2a6 6 0 0 0 5.996 -5.775l.004 -.225z"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M17 20a2 2 0 1 1 0 -4a2 2 0 0 1 0 4z"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M15 14l1 2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M9 14l-3 6"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M15 18h-8"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M15 5v-1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 5v-1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M9 5v-1"
  }));
}

export { Grill as default };
