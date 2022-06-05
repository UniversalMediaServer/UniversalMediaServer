import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function PlugX(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-plug-x",
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
    d: "M13.55 17.733a5.806 5.806 0 0 1 -7.356 -4.052a5.81 5.81 0 0 1 1.537 -5.627l2.054 -2.054l7.165 7.165"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M4 20l3.5 -3.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M15 4l-3.5 3.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M20 9l-3.5 3.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M16 16l4 4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M20 16l-4 4"
  }));
}

export { PlugX as default };
