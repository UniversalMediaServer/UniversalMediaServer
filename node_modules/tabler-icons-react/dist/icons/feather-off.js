import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function FeatherOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-feather-off",
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
    d: "M4 20l8 -8"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14 5v5h5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M9 11v4h4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M6 13v5h5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M6 13l3.502 -3.502m2.023 -2.023l2.475 -2.475"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M19 10c.638 -.636 1 -1.515 1 -2.486a3.515 3.515 0 0 0 -3.517 -3.514c-.97 0 -1.847 .367 -2.483 1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M11 18l3.499 -3.499m2.008 -2.008l2.493 -2.493"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { FeatherOff as default };
