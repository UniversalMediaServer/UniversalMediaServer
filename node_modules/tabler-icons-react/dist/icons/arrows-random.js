import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function ArrowsRandom(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-arrows-random",
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
    d: "M20 21.004h-4v-4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M16 21.004l5 -5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M6.5 9.504l-3.5 -2l2 -3.504"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 7.504l6.83 -1.87"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M4 16.004l4 -1l1 4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 15.004l-3.5 6"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M21 5.004l-.5 4l-4 -.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M20.5 9.004l-4.5 -5.5"
  }));
}

export { ArrowsRandom as default };
