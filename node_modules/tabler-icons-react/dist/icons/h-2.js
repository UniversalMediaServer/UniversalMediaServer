import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function H2(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-h-2",
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
    d: "M17 12a2 2 0 1 1 4 0c0 .591 -.417 1.318 -.816 1.858l-3.184 4.143l4 0"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M4 6v12"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 6v12"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M11 18h2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 18h2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M4 12h8"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 6h2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M11 6h2"
  }));
}

export { H2 as default };
