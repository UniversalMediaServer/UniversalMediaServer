import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Network(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-network",
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
    cy: "9",
    r: "6"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 3c1.333 .333 2 2.333 2 6s-.667 5.667 -2 6"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 3c-1.333 .333 -2 2.333 -2 6s.667 5.667 2 6"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M6 9h12"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 19h7"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14 19h7"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "12",
    cy: "19",
    r: "2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 15v2"
  }));
}

export { Network as default };
