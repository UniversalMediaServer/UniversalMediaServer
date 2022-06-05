import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Dimensions(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-dimensions",
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
    d: "M3 5h11"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 7l2 -2l-2 -2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M5 3l-2 2l2 2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M19 10v11"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M17 19l2 2l2 -2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M21 12l-2 -2l-2 2"
  }), /*#__PURE__*/React.createElement("rect", {
    x: "3",
    y: "10",
    width: "11",
    height: "11",
    rx: "2"
  }));
}

export { Dimensions as default };
