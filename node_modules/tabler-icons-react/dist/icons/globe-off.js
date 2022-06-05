import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function GlobeOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-globe-off",
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
    d: "M8.36 8.339a4 4 0 0 0 5.281 5.31m1.995 -1.98a4 4 0 0 0 -5.262 -5.325"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M6.75 16a8.015 8.015 0 0 0 9.799 .553m2.016 -1.998a8.015 8.015 0 0 0 -2.565 -11.555"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 18v4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 22h8"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { GlobeOff as default };
