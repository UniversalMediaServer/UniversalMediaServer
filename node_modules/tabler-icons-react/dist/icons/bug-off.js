import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function BugOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-bug-off",
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
    d: "M9.884 5.873a3 3 0 0 1 5.116 2.127v1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M13 9h3a6 6 0 0 1 1 3v1m-.298 3.705a5.002 5.002 0 0 1 -9.702 -1.705v-3a6 6 0 0 1 1 -3h1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 13h4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M17 13h4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 20v-6"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M4 19l3.35 -2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M4 7l3.75 2.4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M20 7l-3.75 2.4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { BugOff as default };
