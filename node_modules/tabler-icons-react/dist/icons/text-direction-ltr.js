import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function TextDirectionLtr(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-text-direction-ltr",
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
    d: "M5 19h14"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M17 21l2 -2l-2 -2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M16 4h-6.5a3.5 3.5 0 0 0 0 7h.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14 15v-11"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M10 15v-11"
  }));
}

export { TextDirectionLtr as default };
