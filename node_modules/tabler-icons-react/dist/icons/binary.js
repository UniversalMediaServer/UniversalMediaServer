import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Binary(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-binary",
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
    d: "M11 10v-5h-1m8 14v-5h-1"
  }), /*#__PURE__*/React.createElement("rect", {
    x: "15",
    y: "5",
    width: "3",
    height: "5",
    rx: ".5"
  }), /*#__PURE__*/React.createElement("rect", {
    x: "10",
    y: "14",
    width: "3",
    height: "5",
    rx: ".5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M6 10h.01m-.01 9h.01"
  }));
}

export { Binary as default };
